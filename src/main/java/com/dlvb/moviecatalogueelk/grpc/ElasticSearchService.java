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
