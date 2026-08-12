package com.kniazev.cards.word;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Regression guard that the application can still boot in a non-web context (no
 * embedded web server) under the production profile set ("prod,no-vaadin").
 *
 * <p>Note: since the "Specific words" Mini App was added, the VPS actually runs
 * as a servlet web app ({@code application-prod.properties} sets
 * web-application-type=servlet) to serve the Mini App — see
 * {@code MiniAppWebContextTests} for that path. This test forces
 * {@code webEnvironment=NONE} to ensure every web-only bean stays properly gated
 * behind {@code @ConditionalOnWebApplication(SERVLET)} ({@code BotOnlySecurityConfig},
 * {@code MiniAppController}, {@code MiniAppWebConfig}); an un-gated web-only bean
 * would fail to build this non-web context. The bot is disabled so no polling happens.
 */
@SpringBootTest(
        classes = WordCardsApplication.class,
        webEnvironment = WebEnvironment.NONE,
        properties = {"telegram.bot.enabled=false", "admin.bot.enabled=false", "spring.ai.google.genai.api-key=test-key-not-used"})
@ActiveProfiles({"prod", "no-vaadin"})
class HeadlessBotContextTests {

    @SuppressWarnings("resource")
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
    void headlessContextLoads() {
        // Empty: a failure to build the non-web context fails this test.
    }
}
