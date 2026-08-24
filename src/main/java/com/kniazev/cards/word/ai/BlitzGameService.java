package com.kniazev.cards.word.ai;

import com.kniazev.cards.word.db.model.word.Word;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BlitzGameService {

    private static final Resource SENTENCES_PROMPT_RESOURCE = new ClassPathResource("prompts/blitz-sentences.st");
    private static final Resource CHECK_PROMPT_RESOURCE = new ClassPathResource("prompts/blitz-check.st");
    private static final int EXPECTED_SENTENCE_COUNT = 3;

    private final ChatClient chatClient;

    public BlitzGameService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public Optional<BlitzSentences> generateSentences(List<Word> roundWords) {
        String wordList = roundWords.stream()
                .map(w -> w.getDe() + " - " + w.getRu())
                .collect(Collectors.joining("\n"));

        try {
            BlitzSentences result = chatClient.prompt()
                    .user(u -> u.text(SENTENCES_PROMPT_RESOURCE).param("words", wordList))
                    .call()
                    .entity(BlitzSentences.class);

            return Optional.ofNullable(result).map(BlitzGameService::sanitizeSentences).filter(BlitzGameService::isValid);
        } catch (Exception e) {
            log.warn("Failed to generate blitz sentences: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<BlitzCheckResult> checkTranslations(List<String> russianSentences, List<String> germanTranslations) {
        StringBuilder pairs = new StringBuilder();

        for (int i = 0; i < russianSentences.size(); i++) {
            String translation = i < germanTranslations.size() ? germanTranslations.get(i) : "";
            String shown = StringUtils.isBlank(translation) ? "(not answered)" : translation;

            pairs.append(i + 1).append(". RU: ").append(russianSentences.get(i)).append("\n")
                    .append("   DE (learner): ").append(shown).append("\n");
        }

        try {
            BlitzCheckResult result = chatClient.prompt()
                    .user(u -> u.text(CHECK_PROMPT_RESOURCE).param("pairs", pairs.toString()))
                    .call()
                    .entity(BlitzCheckResult.class);

            return Optional.ofNullable(result).map(BlitzGameService::sanitizeFeedback);
        } catch (Exception e) {
            log.warn("Failed to check blitz translations: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static boolean isValid(BlitzSentences s) {
        return s.sentences() != null && s.sentences().size() == EXPECTED_SENTENCE_COUNT
                && s.sentences().stream().noneMatch(StringUtils::isBlank);
    }

    private static BlitzSentences sanitizeSentences(BlitzSentences raw) {
        return new BlitzSentences(raw.sentences() == null ? null
                : raw.sentences().stream().map(BlitzGameService::strip).toList());
    }

    private static BlitzCheckResult sanitizeFeedback(BlitzCheckResult raw) {
        return new BlitzCheckResult(raw.allCorrect(), strip(raw.feedback()));
    }

    private static String strip(String s) {
        return s == null ? null : s.replaceAll("[*`~]", "");
    }
}
