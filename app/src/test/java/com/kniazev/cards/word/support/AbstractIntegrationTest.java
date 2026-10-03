package com.kniazev.cards.word.support;

import com.kniazev.cards.word.WordCardsApplication;
import com.kniazev.cards.word.game.WordEnrichment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests that need the whole application: real Spring context, real Postgres (full
 * Liquibase changelog, seeded dictionary included) and real Redis. Subclasses share one cached context and one pair of
 * containers per JVM.
 * <p>
 * Deliberately not {@code @Transactional}: a shared persistence context would hide detached-entity behaviour that
 * production code hits. State is reset before every test instead, see {@link TestData#reset()}.
 * <p>
 * The only thing replaced by a mock is {@link WordEnrichment}, the edge to Gemini. Add further {@code @MockitoBean}s
 * here only for external systems, never for our own services.
 */
@Tag("docker")
@SpringBootTest(
        classes = WordCardsApplication.class,
        webEnvironment = WebEnvironment.MOCK,
        properties = {
                "telegram.bot.enabled=false",
                "admin.bot.enabled=false",
                "word.job.enabled=false",
                "spring.ai.google.genai.api-key=test-key-not-used"})
@ActiveProfiles("prod")
@Import(TestData.class)
public abstract class AbstractIntegrationTest {

    private static final int REDIS_PORT = 6379;

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.1")
            .withUsername("test")
            .withPassword("test")
            .withDatabaseName("test-db");

    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(REDIS_PORT);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.liquibase.default-schema", () -> "test");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(REDIS_PORT));
    }

    @MockitoBean
    protected WordEnrichment wordEnrichment;

    @Autowired
    protected TestData testData;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void resetSharedState() {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();

            return null;
        });

        testData.reset();
    }
}
