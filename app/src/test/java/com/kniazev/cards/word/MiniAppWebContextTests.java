package com.kniazev.cards.word;

import static org.assertj.core.api.Assertions.assertThat;

import com.kniazev.cards.word.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Verifies that under the production profile the app boots as a servlet web application (to serve the
 * "Specific words" Mini App), that the Mini App REST is open without a login, and that saving a selection is rejected
 * without valid initData.
 */
@SpringBootTest(
        classes = WordCardsApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {
                "telegram.bot.enabled=false",
                "admin.bot.enabled=false",
                "word.job.enabled=false",
                "spring.ai.google.genai.api-key=test-key-not-used"})
class MiniAppWebContextTests extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void catalogEndpointServesWords() {
        ResponseEntity<Object[]> response = rest.getForEntity("/api/miniapp/words", Object[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as("the seeded word catalog").isNotEmpty();
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

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
