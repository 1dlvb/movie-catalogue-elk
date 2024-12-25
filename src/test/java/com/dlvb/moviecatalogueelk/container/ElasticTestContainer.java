package com.dlvb.moviecatalogueelk.container;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ElasticTestContainer extends ElasticsearchContainer {

    private static final String DOCKER_ELASTIC = "elasticsearch:8.15.0";
    private static final String CLUSTER_NAME = "sample-cluster";
    private static final String ELASTIC_SEARCH = "elasticsearch";

    public ElasticTestContainer() {
        super(DOCKER_ELASTIC);
        this.addFixedExposedPort(9200, 9200);
        this.addFixedExposedPort(9300, 9300);
        this.withEnv("xpack.security.enabled", "false");
        this.addEnv(CLUSTER_NAME, ELASTIC_SEARCH);
    }

    public static void startContainer(ElasticsearchContainer container, String[] mappingFiles, String[] urls) throws IOException {
        container.start();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        for (int i = 0; i < mappingFiles.length; i++) {
            String jsonMapping = new String(Files.readAllBytes(Paths.get(mappingFiles[i])));
            HttpEntity<String> entity = new HttpEntity<>(jsonMapping, headers);
            new RestTemplate().exchange(urls[i], HttpMethod.PUT, entity, String.class);
        }
    }

    public static void stopContainer(ElasticsearchContainer container) {
        container.stop();
    }

}