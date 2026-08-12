package com.kniazev.cards.word.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WordDraftService {
    // Shared by both prompts below - a revision call has no memory of the fresh-draft call, so
    // it needs the full rules restated too, or corrections silently drift from these conventions.
    private static final String FIELD_RULES = """
            - Correct/normalize the German spelling (fix typos, capitalization, umlauts).
            - If the input already includes a Russian translation proposed by the admin, verify it is
              actually correct for the German word. If it is wrong, replace "ru" with the correct
              translation and explain the correction in one short sentence in "translationCorrectionNote".
              If no translation was proposed, or the proposed one was already correct, leave
              "translationCorrectionNote" null.
            - If wordType is NOUN: fill "gender" and "plural" (the plural form, or null if the word has
              no plural).
            - If wordType is VERB: fill "ich", "du", "er", "wir", "ihr", "sie" with the present-tense
              conjugations and "partizip2" with the Partizip II form; fill "prefix"/"rootVerb" only if the
              verb is separable, otherwise leave them null.
              "notes" captures the verb's government (Rektion) in compact dictionary shorthand: whether
              it's reflexive ("sich") and which preposition + case it takes with a noun. Examples:
                "interessieren" (reflexive, + für/Akkusativ) -> "interessieren sich für Akk."
                "einladen" (separable, + zu/Dativ + Akkusativ object) -> "lade Akk. zur Dat. ein"
              Leave "notes" null if the verb has no such reflexive/prepositional government worth noting
              (e.g. plain transitive/intransitive verbs like "sehen", "gehen"). One short line, well
              under 50 characters - never a full sentence or explanation.
            - For any field that does not apply to the classified wordType, leave it null.
            - Write one short, natural example sentence using the word in "sample", and its Russian
              translation in "sampleRu".
            - Every field is a short word or phrase, never a full sentence, except "sample"/"sampleRu"/
              "translationCorrectionNote".
            """;

    private static final String FRESH_PROMPT_TEMPLATE = """
            You are a German language teacher and lexicographer helping an admin add a word to a
            German-Russian vocabulary app. From the raw input below, produce a complete, correct word card.

            Raw input from the admin (may be just a German word, a German word with a proposed Russian
            translation, or a short phrase): "%s"

            Rules:
            %s
            """;

    private static final String REVISION_PROMPT_TEMPLATE = """
            You previously drafted this German word card:
            %s

            The admin reviewed it and asked for this correction: "%s"

            Apply the correction and return the complete, corrected word card, following these field rules:
            %s

            Return the full card, not just the changed fields.
            """;

    private final ChatClient chatClient;

    public WordDraftService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public Optional<WordDraft> generateDraft(String rawInput) {
        return callModel(FRESH_PROMPT_TEMPLATE.formatted(rawInput, FIELD_RULES), rawInput);
    }

    public Optional<WordDraft> reviseDraft(WordDraft previousDraft, String correctionNote) {
        return callModel(REVISION_PROMPT_TEMPLATE.formatted(previousDraft, correctionNote, FIELD_RULES),
                previousDraft.de());
    }

    private Optional<WordDraft> callModel(String prompt, String logContext) {
        try {
            WordDraft draft = chatClient.prompt().user(prompt).call().entity(WordDraft.class);
            return Optional.ofNullable(draft);
        } catch (Exception e) {
            log.warn("Failed to generate word draft for [{}]: {}", logContext, e.getMessage());
            return Optional.empty();
        }
    }
}
