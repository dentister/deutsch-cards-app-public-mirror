package com.kniazev.cards.word.ai.draft;

import com.kniazev.cards.word.model.dictionary.*;
import com.kniazev.cards.word.model.dictionary.Word.WordLevel;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts an AI-proposed {@link WordDraft} into a persistable {@link Word}. Not a Spring
 * bean - plain static helper, easy to unit-test without a context.
 */
public final class WordDraftMapper {

    private WordDraftMapper() {
    }

    private static final int SHORT_FIELD_MAX_LENGTH = 100;
    private static final int NOTES_MAX_LENGTH = 50;

    /**
     * Validates a draft against the same constraints the DB enforces ({@code de}/{@code ru}/
     * {@code word_type} and noun gender / verb conjugations are {@code NOT NULL}; the varchar
     * columns above have hard length caps). Missing {@code level} is not fatal - {@link #toEntity}
     * defaults it to {@link WordLevel#C2}, matching the existing admin dialogs' own default - so
     * it's surfaced only as a warning.
     */
    public static ValidationResult validate(WordDraft draft) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (StringUtils.isBlank(draft.de())) {
            errors.add("German word (de) is missing.");
        } else {
            checkLength(errors, "de", draft.de(), SHORT_FIELD_MAX_LENGTH);
        }
        if (StringUtils.isBlank(draft.ru())) {
            errors.add("Russian translation (ru) is missing.");
        } else {
            checkLength(errors, "ru", draft.ru(), SHORT_FIELD_MAX_LENGTH);
        }

        if (draft.wordType() == null) {
            errors.add("Word type could not be determined.");
        } else if (draft.wordType() == Word.WordType.NOUN) {
            if (draft.gender() == null) {
                errors.add("Noun gender is missing.");
            }
            checkLength(errors, "plural", draft.plural(), SHORT_FIELD_MAX_LENGTH);
        } else if (draft.wordType() == Word.WordType.VERB) {
            if (!hasAllConjugations(draft)) {
                errors.add("Verb conjugations (ich/du/er/wir/ihr/sie) are incomplete.");
            }
            checkLength(errors, "ich", draft.ich(), SHORT_FIELD_MAX_LENGTH);
            checkLength(errors, "du", draft.du(), SHORT_FIELD_MAX_LENGTH);
            checkLength(errors, "er", draft.er(), SHORT_FIELD_MAX_LENGTH);
            checkLength(errors, "wir", draft.wir(), SHORT_FIELD_MAX_LENGTH);
            checkLength(errors, "ihr", draft.ihr(), SHORT_FIELD_MAX_LENGTH);
            checkLength(errors, "sie", draft.sie(), SHORT_FIELD_MAX_LENGTH);
            checkLength(errors, "partizip2", draft.partizip2(), SHORT_FIELD_MAX_LENGTH);
            checkLength(errors, "notes", draft.notes(), NOTES_MAX_LENGTH);
        }

        if (draft.level() == null) {
            warnings.add("Level not determined - will default to C2.");
        }

        return new ValidationResult(errors, warnings);
    }

    private static void checkLength(List<String> errors, String fieldName, String value, int max) {
        if (value != null && value.length() > max) {
            errors.add(fieldName + " is too long (" + value.length() + " chars, max " + max
                    + ") - use Recheck to ask for a shorter one.");
        }
    }

    public static Word toEntity(WordDraft draft) {
        WordLevel level = draft.level() != null ? draft.level() : WordLevel.C2;

        return switch (draft.wordType()) {
            case NOUN -> Noun.builder()
                    .wordType(draft.wordType()).level(level)
                    .de(draft.de()).ru(draft.ru()).en(draft.en())
                    .sample(draft.sample()).sampleRu(draft.sampleRu()).sampleEn(draft.sampleEn())
                    .gender(draft.gender()).plural(draft.plural())
                    .build();
            case VERB -> Verb.builder()
                    .wordType(draft.wordType()).level(level)
                    .de(draft.de()).ru(draft.ru()).en(draft.en())
                    .sample(draft.sample()).sampleRu(draft.sampleRu()).sampleEn(draft.sampleEn())
                    .ich(draft.ich()).du(draft.du()).er(draft.er())
                    .wir(draft.wir()).ihr(draft.ihr()).sie(draft.sie())
                    .partizip2(draft.partizip2()).prefix(draft.prefix())
                    .rootVerb(draft.rootVerb()).notes(draft.notes())
                    .build();
            case ADJECTIVE -> Adjective.builder()
                    .wordType(draft.wordType()).level(level)
                    .de(draft.de()).ru(draft.ru()).en(draft.en())
                    .sample(draft.sample()).sampleRu(draft.sampleRu()).sampleEn(draft.sampleEn())
                    .build();
            case ADVERB -> Adverb.builder()
                    .wordType(draft.wordType()).level(level)
                    .de(draft.de()).ru(draft.ru()).en(draft.en())
                    .sample(draft.sample()).sampleRu(draft.sampleRu()).sampleEn(draft.sampleEn())
                    .build();
            case PHRASE -> Phrase.builder()
                    .wordType(draft.wordType()).level(level)
                    .de(draft.de()).ru(draft.ru()).en(draft.en())
                    .sample(draft.sample()).sampleRu(draft.sampleRu()).sampleEn(draft.sampleEn())
                    .build();
        };
    }

    private static boolean hasAllConjugations(WordDraft draft) {
        return StringUtils.isNotBlank(draft.ich()) && StringUtils.isNotBlank(draft.du())
                && StringUtils.isNotBlank(draft.er()) && StringUtils.isNotBlank(draft.wir())
                && StringUtils.isNotBlank(draft.ihr()) && StringUtils.isNotBlank(draft.sie());
    }

    public record ValidationResult(List<String> errors, List<String> warnings) {
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }
}
