package com.kniazev.cards.word.ai;

import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.db.service.WordService;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WordExampleService {

    private static final Resource PROMPT_RESOURCE = new ClassPathResource("prompts/word-example.st");

    private final ChatClient chatClient;
    private final WordService wordService;

    public WordExampleService(ChatClient.Builder chatClientBuilder, WordService wordService) {
        this.chatClient = chatClientBuilder.build();
        this.wordService = wordService;
    }

    public Optional<WordUsageExample> generateExample(Word word) {
        try {
            WordUsageExample example = chatClient.prompt()
                    .user(u -> u.text(PROMPT_RESOURCE)
                            .param("word", word.getDe())
                            .param("wordType", Objects.toString(word.getWordType(), ""))
                            .param("level", Objects.toString(word.getLevel(), "")))
                    .call()
                    .entity(WordUsageExample.class);

            return Optional.ofNullable(example).map(WordExampleService::sanitize);
        } catch (Exception e) {
            log.warn("Failed to generate usage example for word [{}]: {}", word.getDe(), e.getMessage());
            return Optional.empty();
        }
    }

    @Async("wordExampleExecutor")
    public void ensureExampleAndEnglishExist(Word word) {
        doEnsureExampleAndEnglishExist(word);
    }

    void doEnsureExampleAndEnglishExist(Word word) {
        if (StringUtils.isAnyEmpty(word.getEn(), word.getSample(), word.getSampleRu(), word.getSampleEn())) {
            generateExample(word).ifPresent(example -> {
                word.setSample(example.sentence());
                word.setSampleRu(example.translationRu());
                word.setSampleEn(example.translationEn());
                word.setEn(example.wordEn());

                wordService.save(word);

                log.debug("Saved usage example for word [{}]", word.getDe());
            });
        }
    }

    private static WordUsageExample sanitize(WordUsageExample raw) {
        return new WordUsageExample(strip(raw.sentence()), strip(raw.translationRu()),
                strip(raw.translationEn()), normalizeSlashes(strip(raw.wordEn())));
    }

    private static String strip(String s) {
        return s == null ? null : s.replaceAll("[*`~]", "");
    }

    private static String normalizeSlashes(String s) {
        return s == null ? null : s.replaceAll("\\s*/\\s*", ", ");
    }
}
