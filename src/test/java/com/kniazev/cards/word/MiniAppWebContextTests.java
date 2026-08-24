package com.kniazev.cards.word;

import static org.junit.jupiter.api.Assertions.*;

import com.kniazev.cards.word.controller.MiniAppController;
import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.db.repository.WordScoreRepository;
import com.kniazev.cards.word.db.service.WordService;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Verifies that under the production profile set ("prod,no-vaadin") the app now boots
 * as a servlet web application (to serve the "Specific words" Mini App), that the Mini
 * App REST is wired, and that saving a selection is rejected without valid initData.
 * Vaadin stays excluded — this is the slim-server path the VPS runs.
 */
@SpringBootTest(
        classes = WordCardsApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"telegram.bot.enabled=false", "admin.bot.enabled=false", "spring.ai.google.genai.api-key=test-key-not-used"})
@ActiveProfiles({"prod", "no-vaadin"})
class MiniAppWebContextTests {

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

    @Autowired
    private ApplicationContext context;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private WordService wordService;

    @Autowired
    private WordScoreRepository wordScoreRepository;

    @Test
    void miniAppControllerIsWiredInServletContext() {
        assertNotNull(context.getBean(MiniAppController.class));
    }

    @Test
    void catalogEndpointServesWords() {
        ResponseEntity<Object[]> response = rest.getForEntity("/api/miniapp/words", Object[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0, "expected the seeded word catalog to be served");
    }

    @Test
    void savingSelectionIsRejectedWithoutValidInitData() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Telegram-Init-Data", "user=%7B%22id%22%3A1%7D&hash=bogus");

        ResponseEntity<String> response = rest.postForEntity(
                "/api/miniapp/selection",
                new HttpEntity<>("{\"wordIds\":[1]}", headers),
                String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void selectionIsRestrictedToSpecificWordIds() {
        List<Long> picked = wordService.findAll().stream().map(Word::getId).limit(3).toList();
        Long[] pickedArray = picked.toArray(new Long[0]);

        // p_word_ids restricts the weighted-random pool to exactly these ids ("only selected").
        List<Long> result = wordScoreRepository.findWordScores(null, null, null, null, pickedArray);

        assertFalse(result.isEmpty(), "expected the picked words to be returned");
        assertTrue(picked.containsAll(result), "every returned id must be within the picked set");
        assertTrue(result.size() <= pickedArray.length);
    }
}
