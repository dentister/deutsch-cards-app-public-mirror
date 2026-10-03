package com.kniazev.cards.word.ai.draft;

import static org.assertj.core.api.Assertions.assertThat;

import com.kniazev.cards.word.ai.draft.WordDraftMapper.ValidationResult;
import com.kniazev.cards.word.model.dictionary.*;
import com.kniazev.cards.word.model.dictionary.Noun.GenderType;
import com.kniazev.cards.word.model.dictionary.Word.WordLevel;
import com.kniazev.cards.word.model.dictionary.Word.WordType;

import org.junit.jupiter.api.Test;

class WordDraftMapperTests {

    @Test
    void nounWithoutGenderIsHardError() {
        WordDraft draft = draft(WordType.NOUN, "Tisch", "стол", WordLevel.A1, null, null,
                null, null, null, null, null, null, null, null, null, null);

        ValidationResult result = WordDraftMapper.validate(draft);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors()).anyMatch(e -> e.toLowerCase().contains("gender"));
    }

    @Test
    void verbNotesOverflowingDbColumnIsHardError() {
        // verb.notes is varchar(50) (db/sql/game/008-game.sql) - a real crash if this slips
        // through: DataIntegrityViolationException at save time, not just a style nitpick.
        String tooLong = "Dieses Verb wird meist im Sinne von 'plötzlich erfassen' verwendet.";
        WordDraft draft = draft(WordType.VERB, "überkommen", "одолевать", WordLevel.C1, null, null,
                "überkomme", "überkommst", "überkommt", "überkommen", "überkommt", "überkommen",
                "überkommen", null, "Sample.", "Пример.");
        draft = withNotes(draft, tooLong);

        ValidationResult result = WordDraftMapper.validate(draft);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors()).anyMatch(e -> e.toLowerCase().contains("notes") && e.contains("too long"));
    }

    @Test
    void verbWithIncompleteConjugationsIsHardError() {
        WordDraft draft = draft(WordType.VERB, "gehen", "идти", WordLevel.A1, null, null,
                "gehe", "gehst", "geht", null, "geht", "gehen", "gegangen", null, null, null);

        ValidationResult result = WordDraftMapper.validate(draft);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors()).anyMatch(e -> e.toLowerCase().contains("conjugat"));
    }

    @Test
    void missingLevelIsWarningOnlyNotError() {
        WordDraft draft = draft(WordType.PHRASE, "auf jeden Fall", "в любом случае", null, null, null,
                null, null, null, null, null, null, null, null, null, null);

        ValidationResult result = WordDraftMapper.validate(draft);

        assertThat(result.hasErrors()).isFalse();
        assertThat(result.warnings()).anyMatch(w -> w.toLowerCase().contains("level"));
    }

    @Test
    void missingDeOrRuIsHardError() {
        WordDraft draft = draft(WordType.ADJECTIVE, null, null, WordLevel.B1, null, null,
                null, null, null, null, null, null, null, null, null, null);

        ValidationResult result = WordDraftMapper.validate(draft);

        assertThat(result.errors().size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void completeNounDraftHasNoErrorsOrWarnings() {
        WordDraft draft = draft(WordType.NOUN, "Tisch", "стол", WordLevel.A1, GenderType.M, "Tische",
                null, null, null, null, null, null, null, null, null, null);

        ValidationResult result = WordDraftMapper.validate(draft);

        assertThat(result.hasErrors()).isFalse();
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void toEntitySetsWordTypeExplicitlyForNoun() {
        // Regression test: existing admin dialogs (NounDialog etc.) never call setWordType()
        // on new entities, which silently breaks WordService.createOrRewrite's
        // findOneByDeAndWordType dedup lookup. Our mapper must not repeat that.
        WordDraft draft = draft(WordType.NOUN, "Tisch", "стол", WordLevel.A1, GenderType.M, "Tische",
                null, null, null, null, null, null, null, null, "Sample.", "Пример.");

        Word word = WordDraftMapper.toEntity(draft);

        assertThat(word).isInstanceOf(Noun.class);
        assertThat(word.getWordType()).isEqualTo(WordType.NOUN);
        assertThat(word.getDe()).isEqualTo("Tisch");
        assertThat(word.getSample()).isEqualTo("Sample.");
        assertThat(word.getSampleRu()).isEqualTo("Пример.");
        assertThat(((Noun) word).getGender()).isEqualTo(GenderType.M);
        assertThat(((Noun) word).getPlural()).isEqualTo("Tische");
    }

    @Test
    void toEntitySetsAllConjugationsForVerb() {
        WordDraft draft = draft(WordType.VERB, "gehen", "идти", WordLevel.A1, null, null,
                "gehe", "gehst", "geht", "gehen", "geht", "gehen", "gegangen", null, null, null);

        Word word = WordDraftMapper.toEntity(draft);

        assertThat(word).isInstanceOf(Verb.class);
        assertThat(word.getWordType()).isEqualTo(WordType.VERB);
        Verb verb = (Verb) word;
        assertThat(verb.getIch()).isEqualTo("gehe");
        assertThat(verb.getSie()).isEqualTo("gehen");
        assertThat(verb.getPartizip2()).isEqualTo("gegangen");
    }

    @Test
    void toEntityCopiesEnglishFieldsOntoBuiltWord() {
        WordDraft draft = new WordDraft(WordType.NOUN, "Tisch", "стол", "table", WordLevel.A1,
                GenderType.M, "Tische", null, null, null, null, null, null, null, null, null, null,
                "Der Tisch ist alt.", "Стол старый.", "The table is old.", null);

        Word word = WordDraftMapper.toEntity(draft);

        assertThat(word.getEn()).isEqualTo("table");
        assertThat(word.getSampleEn()).isEqualTo("The table is old.");
    }

    @Test
    void toEntityDefaultsMissingLevelToC2() {
        WordDraft draft = draft(WordType.PHRASE, "auf jeden Fall", "в любом случае", null, null, null,
                null, null, null, null, null, null, null, null, null, null);

        Word word = WordDraftMapper.toEntity(draft);

        assertThat(word).isInstanceOf(Phrase.class);
        assertThat(word.getWordType()).isEqualTo(WordType.PHRASE);
        assertThat(word.getLevel()).isEqualTo(WordLevel.C2);
    }

    private static WordDraft draft(WordType wordType, String de, String ru, WordLevel level,
                                    GenderType gender, String plural,
                                    String ich, String du, String er, String wir, String ihr, String sie,
                                    String partizip2, String prefix, String sample, String sampleRu) {
        return new WordDraft(wordType, de, ru, null, level, gender, plural,
                ich, du, er, wir, ihr, sie, partizip2, prefix, null, null, sample, sampleRu, null, null);
    }

    private static WordDraft withNotes(WordDraft draft, String notes) {
        return new WordDraft(draft.wordType(), draft.de(), draft.ru(), draft.en(), draft.level(),
                draft.gender(), draft.plural(), draft.ich(), draft.du(), draft.er(), draft.wir(),
                draft.ihr(), draft.sie(), draft.partizip2(), draft.prefix(), draft.rootVerb(), notes,
                draft.sample(), draft.sampleRu(), draft.sampleEn(), draft.translationCorrectionNote());
    }
}
