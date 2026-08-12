package com.kniazev.cards.word.ai;

import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.db.services.WordService;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WordExampleService {
    private static final String PROMPT_TEMPLATE = """
            You are a German language teacher. Write ONE short, simple example sentence in German
            that uses the word "%s" (%s), suitable for a learner at CEFR level %s.
            Keep it natural and no longer than 10 words. Also provide its Russian translation.
            """;

    private final ChatClient chatClient;
    private final WordService wordService;

    public WordExampleService(ChatClient.Builder chatClientBuilder, WordService wordService) {
        this.chatClient = chatClientBuilder.build();
        this.wordService = wordService;
    }

    public Optional<WordUsageExample> generateExample(Word word) {
        try {
            WordUsageExample example = chatClient.prompt()
                    .user(PROMPT_TEMPLATE.formatted(word.getDe(), word.getWordType(), word.getLevel()))
                    .call()
                    .entity(WordUsageExample.class);

            return Optional.ofNullable(example).map(WordExampleService::sanitize);
        } catch (Exception e) {
            log.warn("Failed to generate usage example for word [{}]: {}", word.getDe(), e.getMessage());
            return Optional.empty();
        }
    }

    @Async("wordExampleExecutor")
    public void ensureExampleCached(Word word) {
        cacheExampleIfMissing(word);
    }

    // Package-private so it's directly unit-testable without going through the @Async proxy.
    void cacheExampleIfMissing(Word word) {
        if (StringUtils.isNotBlank(word.getSample())) {
            log.debug("Sample already cached for word [{}], skipping generation", word.getDe());
            return;
        }

        generateExample(word).ifPresent(example -> {
            word.setSample(example.sentence());
            word.setSampleRu(example.translationRu());
            wordService.save(word);
            log.debug("Cached new usage example for word [{}]", word.getDe());
        });
    }

    private static WordUsageExample sanitize(WordUsageExample raw) {
        return new WordUsageExample(strip(raw.sentence()), strip(raw.translationRu()));
    }

    private static String strip(String s) {
        return s == null ? null : s.replaceAll("[*`~]", "");
    }
}
