package com.kniazev.cards.word.ai;

import com.kniazev.cards.word.db.model.word.Noun.GenderType;
import com.kniazev.cards.word.db.model.word.Word.WordLevel;
import com.kniazev.cards.word.db.model.word.Word.WordType;

/**
 * AI-proposed word card, flat across every {@code Word} subtype so Spring AI's structured
 * output can target a single concrete class. Fields that don't apply to the drafted
 * {@code wordType} (e.g. {@code gender} for a VERB) are left {@code null}. Using the real
 * entity enums (rather than plain strings) makes Spring AI emit a JSON-schema {@code enum}
 * constraint for them, so the model is constrained at generation time instead of us having
 * to validate/parse a free-text value afterwards.
 */
public record WordDraft(
        WordType wordType,
        String de,
        String ru,
        WordLevel level,

        // Noun-only
        GenderType gender,
        String plural,

        // Verb-only
        String ich,
        String du,
        String er,
        String wir,
        String ihr,
        String sie,
        String partizip2,
        String prefix,
        String rootVerb,
        String notes,

        String sample,
        String sampleRu,

        // Non-null only when the raw input included a user-proposed RU translation that the AI corrected.
        String translationCorrectionNote
) {
}
