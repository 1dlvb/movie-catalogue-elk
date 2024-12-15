package com.dlvb.moviecatalogueelk.grpc;

import com.dlvb.graphqlmoviecatalogue.MovieSearchServiceGrpc;
import com.dlvb.graphqlmoviecatalogue.Search;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.ArrayList;
import java.util.List;

@GrpcService
public class ElasticSearchService extends MovieSearchServiceGrpc.MovieSearchServiceImplBase {

    @Override
    public void searchMovies(Search.SearchRequest request, StreamObserver<Search.SearchResponse> responseObserver) {
        String query = request.getQuery();

        List<Search.Movie> movies = mockSearch(query);

        Search.SearchResponse.Builder responseBuilder = Search.SearchResponse.newBuilder();
        responseBuilder.addAllMovies(movies);

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    private List<Search.Movie> mockSearch(String query) {
        List<Search.Movie> movies = new ArrayList<>();

        if (query.contains("action")) {
            movies.add(Search.Movie.newBuilder()
                    .setId("1")
                    .setTitle("Action Movie 1")
                    .setGenreId("1")
                    .setDescription("An exciting action movie.")
                    .build());
            movies.add(Search.Movie.newBuilder()
                    .setId("2")
                    .setTitle("Action Movie 2")
                    .setGenreId("1")
                    .setDescription("Another thrilling action movie.")
                    .build());
        } else if (query.contains("comedy")) {
            movies.add(Search.Movie.newBuilder()
                    .setId("3")
                    .setGenreId("2")
                    .setTitle("Comedy Movie 1")
                    .setDescription("A hilarious comedy movie.")
                    .build());
        } else {
            movies.add(Search.Movie.newBuilder()
                    .setId("4")
                    .setGenreId("3")
                    .setTitle("Generic Movie")
                    .setDescription("A generic movie with no specific genre.")
                    .build());
        }

        return movies;
    }

}
