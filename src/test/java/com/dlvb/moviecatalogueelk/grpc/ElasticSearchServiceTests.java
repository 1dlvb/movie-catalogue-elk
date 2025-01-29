package com.dlvb.moviecatalogueelk.grpc;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.dlvb.graphqlmoviecatalogue.Search;
import com.dlvb.moviecatalogueelk.container.ElasticTestContainer;
import com.dlvb.moviecatalogueelk.model.MovieDocument;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class ElasticSearchServiceTests {

    @Autowired
    private ElasticSearchService elasticSearchService;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Container
    static ElasticTestContainer elasticTestContainer = new ElasticTestContainer();

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.rest.uris",
                () -> "http://localhost:" + elasticTestContainer.getMappedPort(9200));
    }

    @BeforeEach
    public void setUp() throws Exception {
        elasticTestContainer.start();

        String[] mappingFiles = { "src/test/resources/config/index-config.json" };
        String[] urls = { "http://localhost:" + elasticTestContainer.getMappedPort(9200) + "/movie.public.movie" };
        ElasticTestContainer.startContainer(elasticTestContainer, mappingFiles, urls);

        MovieDocument movie1 = new MovieDocument(1, "Inception", "description thriller", 9, false);
        MovieDocument movie2 = new MovieDocument(2, "Matrix", "description classic", 10, false);

        elasticsearchClient.index(index -> index.index("movie.public.movie").id("1").document(movie1));
        elasticsearchClient.index(index -> index.index("movie.public.movie").id("2").document(movie2));

        Thread.sleep(10000);
    }


    @AfterEach
    public void tearDown() {
        ElasticTestContainer.stopContainer(elasticTestContainer);
    }

    @Test
    @DisplayName("search: returns movie by title")
    void testSearchMovieByTitle() {
        Search.SearchRequest searchRequest = Search.SearchRequest.newBuilder()
                .setQuery("Inception")
                .setPageNumber(0)
                .setPageSize(10)
                .build();

        StreamObserver<Search.SearchResponse> responseObserver = mock(StreamObserver.class);
        elasticSearchService.searchMovies(searchRequest, responseObserver);

        await().untilAsserted(() -> {
            verify(responseObserver, times(1)).onNext(any(Search.SearchResponse.class));
            verify(responseObserver, times(1)).onCompleted();
        });

        ArgumentCaptor<Search.SearchResponse> captor = ArgumentCaptor.forClass(Search.SearchResponse.class);
        verify(responseObserver).onNext(captor.capture());

        Search.SearchResponse response = captor.getValue();
        assertEquals(1, response.getMoviesList().size());
        assertEquals("Inception", response.getMoviesList().getFirst().getTitle());
    }

    @Test
    @DisplayName("search: returns full size list when empty query")
    void testSearchWhenEmptyQueryReturnsFullSizeList() throws Exception {
        Search.SearchRequest searchRequest = Search.SearchRequest.newBuilder()
                .setQuery("")
                .setPageNumber(0)
                .setPageSize(10)
                .build();

        StreamObserver<Search.SearchResponse> responseObserver = mock(StreamObserver.class);
        Thread.sleep(20000);
        elasticSearchService.searchMovies(searchRequest, responseObserver);

        await().untilAsserted(() -> {
            verify(responseObserver, times(1)).onNext(any(Search.SearchResponse.class));
            verify(responseObserver, times(1)).onCompleted();
        });

        ArgumentCaptor<Search.SearchResponse> captor = ArgumentCaptor.forClass(Search.SearchResponse.class);
        verify(responseObserver).onNext(captor.capture());

        Search.SearchResponse response = captor.getValue();
        assertEquals(2, response.getMoviesList().size());
    }

}
