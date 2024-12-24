package com.dlvb.moviecatalogueelk.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for setting up an {@link ElasticsearchClient} bean.
 * <p>Configuration properties:
 * <ul>
 *     <li>{@code spring.elasticsearch.host} - The host address of the Elasticsearch server.</li>
 *     <li>{@code spring.elasticsearch.port} - The port on which Elasticsearch is running.</li>
 * </ul>
 * @author Matushkin Anton
 */
@Configuration
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.host}")
    private String host;

    @Value("${spring.elasticsearch.port}")
    private Integer port;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        RestClient restClient = RestClient.builder(new HttpHost(host, port)).build();
        RestClientTransport transport = new RestClientTransport(
                restClient,
                new co.elastic.clients.json.jackson.JacksonJsonpMapper()
        );
        return new ElasticsearchClient(transport);
    }
}
