package com.kniazev.cards.word;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import com.kniazev.cards.word.config.TestHttpClientConfig;

@SpringBootTest(classes = {WordCardsApplication.class, TestHttpClientConfig.class})
@ActiveProfiles("test")
class WordCardsApplicationTests {

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.1")
            .withUsername("test")
            .withPassword("test")
            .withDatabaseName("test-db");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.liquibase.default-schema", () -> "test");
    }

    @Test
    void contextLoads() {
        // If the context fails to load, this test will fail.
        // Therefore, an empty test body is sufficient to verify context startup.
    }
}
