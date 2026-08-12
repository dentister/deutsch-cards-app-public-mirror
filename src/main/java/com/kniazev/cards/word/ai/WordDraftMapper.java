package com.kniazev.cards.word.ai;

import com.kniazev.cards.word.db.model.word.Adjective;
import com.kniazev.cards.word.db.model.word.Adverb;
import com.kniazev.cards.word.db.model.word.Noun;
import com.kniazev.cards.word.db.model.word.Phrase;
import com.kniazev.cards.word.db.model.word.Verb;
import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.db.model.word.Word.WordLevel;

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

    // word.de/ru and verb.ich/du/er/wir/ihr/sie/partizip_2/noun.plural are all varchar(100)
    // (db/sql/game/001-game.sql, 002-game.sql).
    private static final int SHORT_FIELD_MAX_LENGTH = 100;
    // verb.notes is varchar(50) (db/sql/game/008-game.sql) - a short tag, not free text. A
    // drafted value that overflows this crashes the save with a DataIntegrityViolationException
    // if it slips past here, so this is a real constraint to catch, not a hypothetical one.
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

    /**
     * Builds the right {@link Word} subtype from a draft. Callers must {@link #validate}
     * first - this does not re-check required fields.
     *
     * <p>Explicitly sets {@code wordType} on the built entity (via the builder chain, so it's
     * populated in memory even though the {@code word_type} column itself is
     * {@code insertable=false}). Every existing admin dialog skips this, which silently
     * breaks {@code WordService.createOrRewrite}'s dedup lookup for newly-created words
     * ({@code findOneByDeAndWordType(de, null)} never matches) - we deliberately don't
     * repeat that here, since dedup actually matters for this flow.
     */
    public static Word toEntity(WordDraft draft) {
        WordLevel level = draft.level() != null ? draft.level() : WordLevel.C2;

        return switch (draft.wordType()) {
            case NOUN -> Noun.builder()
                    .wordType(draft.wordType()).de(draft.de()).ru(draft.ru()).level(level)
                    .sample(draft.sample()).sampleRu(draft.sampleRu())
                    .gender(draft.gender()).plural(draft.plural())
                    .build();
            case VERB -> Verb.builder()
                    .wordType(draft.wordType()).de(draft.de()).ru(draft.ru()).level(level)
                    .sample(draft.sample()).sampleRu(draft.sampleRu())
                    .ich(draft.ich()).du(draft.du()).er(draft.er())
                    .wir(draft.wir()).ihr(draft.ihr()).sie(draft.sie())
                    .partizip2(draft.partizip2()).prefix(draft.prefix())
                    .rootVerb(draft.rootVerb()).notes(draft.notes())
                    .build();
            case ADJECTIVE -> Adjective.builder()
                    .wordType(draft.wordType()).de(draft.de()).ru(draft.ru()).level(level)
                    .sample(draft.sample()).sampleRu(draft.sampleRu())
                    .build();
            case ADVERB -> Adverb.builder()
                    .wordType(draft.wordType()).de(draft.de()).ru(draft.ru()).level(level)
                    .sample(draft.sample()).sampleRu(draft.sampleRu())
                    .build();
            case PHRASE -> Phrase.builder()
                    .wordType(draft.wordType()).de(draft.de()).ru(draft.ru()).level(level)
                    .sample(draft.sample()).sampleRu(draft.sampleRu())
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
