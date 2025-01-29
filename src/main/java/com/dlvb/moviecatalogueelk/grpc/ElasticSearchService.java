package com.dlvb.moviecatalogueelk.grpc;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.dlvb.graphqlmoviecatalogue.MovieSearchServiceGrpc;
import com.dlvb.graphqlmoviecatalogue.Search;
import com.dlvb.moviecatalogueelk.model.MovieDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * GRPC service implementation for searching movies using Elasticsearch.
 * <p>Class extends {@link MovieSearchServiceGrpc.MovieSearchServiceImplBase}
 * and implements the GRPC method for searching movies.</p>
 * @author Matushkin Anton
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class ElasticSearchService extends MovieSearchServiceGrpc.MovieSearchServiceImplBase {

    @NonNull
    private final ObjectMapper objectMapper;

    @NonNull
    private final ElasticsearchClient elasticsearchClient;

    @Value("${spring.elasticsearch.index.name.movie}")
    private String movieIndexName;

    /**
     * Searches for movies based on the provided search request.
     *
     * @param request the search request containing the query, page number, and page size
     * @param responseObserver the response observer to send the search results back to the client
     */
    @Override
    public void searchMovies(Search.SearchRequest request, StreamObserver<Search.SearchResponse> responseObserver) {
        String query = request.getQuery();
        int pageNumber = request.getPageNumber();
        int size = request.getPageSize();

        List<Search.Movie> movies = performFullTextSearch(query, pageNumber, size);

        Search.SearchResponse.Builder responseBuilder = Search.SearchResponse.newBuilder();
        responseBuilder.addAllMovies(movies);

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    /**
     * Performs a full-text search on Elasticsearch to find movies based on the given query, page number, and size.
     *
     * @param query the search query
     * @param pageNumber the page number for pagination
     * @param size the number of results per page
     * @return a list of movies that match the search query
     */
    private List<Search.Movie> performFullTextSearch(String query, int pageNumber, int size) {
        try {
            int from = pageNumber * size;
            SearchResponse<Map> searchResponse = elasticsearchClient.search(
                    SearchRequest.of(s -> s
                            .index(movieIndexName)
                            .from(from)
                            .size(size)
                            .query(q -> {
                                if (query == null || query.isEmpty()) {
                                    return q.matchAll(m -> m);
                                } else {
                                    return q.multiMatch(m -> m
                                            .fields("title", "description")
                                            .query(query)
                                    );
                                }
                            })
                    ),
                    Map.class
            );

            return mapSearchHitsToMovies(searchResponse);
        } catch (IOException e) {
            log.error("Error occurred while performing full-text search: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Maps Elasticsearch search hits to a list of {@link Search.Movie} objects.
     *
     * @param searchResponse the Elasticsearch search response
     * @return a list of movies based on the search hits
     */
    private List<Search.Movie> mapSearchHitsToMovies(SearchResponse<Map> searchResponse) {
        List<Search.Movie> movies = new ArrayList<>();

        for (Hit<Map> hit : searchResponse.hits().hits()) {
            Map<String, Object> source = hit.source();
            if (source != null) {
                MovieDocument movieDocument = objectMapper.convertValue(source, MovieDocument.class);

                Search.Movie.Builder movieBuilder = Search.Movie.newBuilder();
                movieBuilder.setId(String.valueOf(movieDocument.getId()));
                movieBuilder.setTitle(movieDocument.getTitle());
                movieBuilder.setDescription(movieDocument.getDescription());
                movieBuilder.setGenreId(String.valueOf(movieDocument.getGenreId()));

                movies.add(movieBuilder.build());
            }
        }
        return movies;
    }

}
