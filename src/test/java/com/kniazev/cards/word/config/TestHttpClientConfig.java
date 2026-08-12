package com.kniazev.cards.word.config;
import com.kniazev.cards.word.test.client.api.WordCrudApi;
import com.kniazev.test.cards.word.test.client.invoker.ApiClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@TestConfiguration
public class TestHttpClientConfig {
    
    @Value("${server.port}")
    private String port;
    
    @Bean
    RestClient restClient() {
        return RestClient.create();
    }
    
    @Bean
    ApiClient apiClient() {
        return new ApiClient(restClient()).setBasePath("http://localhost:" + port);
    }

    @Bean
    WordCrudApi wordCrudApi(ApiClient apiClient) {
        return new WordCrudApi(apiClient);
    }
    
}