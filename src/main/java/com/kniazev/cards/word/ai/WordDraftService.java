package com.kniazev.cards.word.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WordDraftService {
    private static final Resource FRESH_PROMPT_RESOURCE = new ClassPathResource("prompts/word-draft-fresh.st");
    private static final Resource REVISION_PROMPT_RESOURCE = new ClassPathResource("prompts/word-draft-revision.st");

    private static final String FIELD_RULES = readResource(new ClassPathResource("prompts/word-draft-field-rules.st"));

    private final ChatClient chatClient;

    public WordDraftService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public Optional<WordDraft> generateDraft(String rawInput) {
        return callModel(u -> u.text(FRESH_PROMPT_RESOURCE)
                .param("rawInput", rawInput)
                .param("fieldRules", FIELD_RULES), rawInput);
    }

    public Optional<WordDraft> reviseDraft(WordDraft previousDraft, String correctionNote) {
        return callModel(u -> u.text(REVISION_PROMPT_RESOURCE)
                .param("previousDraft", previousDraft.toString())
                .param("correctionNote", correctionNote)
                .param("fieldRules", FIELD_RULES), previousDraft.de());
    }

    private Optional<WordDraft> callModel(Consumer<ChatClient.PromptUserSpec> userSpec, String logContext) {
        try {
            WordDraft draft = chatClient.prompt().user(userSpec).call().entity(WordDraft.class);

            return Optional.ofNullable(draft);
        } catch (Exception e) {
            log.warn("Failed to generate word draft for [{}]: {}", logContext, e.getMessage());
            return Optional.empty();
        }
    }

    private static String readResource(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
