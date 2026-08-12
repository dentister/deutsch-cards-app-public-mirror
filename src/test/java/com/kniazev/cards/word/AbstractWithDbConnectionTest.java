package com.kniazev.cards.word;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.PostgreSQLContainer;

import com.kniazev.cards.word.config.TestHttpClientConfig;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {WordCardsApplication.class, TestHttpClientConfig.class}, webEnvironment = WebEnvironment.DEFINED_PORT)
@ContextConfiguration(initializers = AbstractWithDbConnectionTest.class)
@ActiveProfiles("test")
@PropertySource(value = "classpath:application.properties", encoding = "UTF-8")
public class AbstractWithDbConnectionTest implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @SuppressWarnings("resource")
    private final static PostgreSQLContainer<?> CONTAINER = new PostgreSQLContainer<>("postgres:16.1")
            .withUsername("test")
            .withPassword("test")
            .withDatabaseName("test-db");
    
    static {
        CONTAINER.start();
        Runtime.getRuntime().addShutdownHook(new Thread(CONTAINER::stop));
    }
    
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        TestPropertyValues.of(
                "spring.datasource.url=" + CONTAINER.getJdbcUrl(),
                "spring.datasource.username=" + CONTAINER.getUsername(),
                "spring.datasource.password=" + CONTAINER.getPassword(),
                "spring.liquibase.default-schema=test",
                "server.port=8090")
        .applyTo(applicationContext.getEnvironment());
    }
}
