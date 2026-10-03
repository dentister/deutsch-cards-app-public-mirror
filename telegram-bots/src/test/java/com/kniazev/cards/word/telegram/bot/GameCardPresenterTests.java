package com.kniazev.cards.word.telegram.bot;

import static org.assertj.core.api.Assertions.assertThat;

import com.kniazev.cards.word.game.GameCardFactory;
import com.kniazev.cards.word.game.representation.TaskEnum;
import com.kniazev.cards.word.game.representation.WordCard;
import com.kniazev.cards.word.model.dictionary.Adjective;
import com.kniazev.cards.word.model.dictionary.Adverb;
import com.kniazev.cards.word.model.dictionary.Noun;
import com.kniazev.cards.word.model.dictionary.Noun.GenderType;
import com.kniazev.cards.word.model.dictionary.Phrase;
import com.kniazev.cards.word.model.dictionary.Verb;
import com.kniazev.cards.word.model.dictionary.Word;
import com.kniazev.cards.word.model.dictionary.Word.WordLevel;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The texts are pinned to what the learning bot showed before they moved out of {@code core}, except two deliberate
 * fixes: the colored circle after a wrong answer ({@code {emojiSuffix}} was never filled) and the example leaves out a
 * sentence that has no translation yet instead of printing {@code null}.
 */
class GameCardPresenterTests {

    private static final Locale RU = Locale.forLanguageTag("ru");

    private final GameCardPresenter presenter = new GameCardPresenter();

    private final Noun tisch = Noun.builder().de("Tisch").ru("стол").en("table").level(WordLevel.A1).gender(GenderType.M)
            .plural("Tische").sample("Der Tisch ist groß.").sampleRu("Стол большой.").sampleEn("The table is big.").build();
    private final Noun lampe = Noun.builder().de("Lampe").ru("лампа").level(WordLevel.A2).gender(GenderType.F).build();
    private final Noun kind = Noun.builder().de("Kind").ru("ребёнок").level(WordLevel.A1).gender(GenderType.N)
            .plural("Kinder").sample("Das Kind spielt.").build();
    private final Noun eltern = Noun.builder().de("Eltern").ru("родители").level(WordLevel.B1).gender(GenderType.PL).build();
    private final Verb machen = Verb.builder().de("machen").ru("делать").level(WordLevel.A1).ich("mache").du("machst")
            .er("macht").wir("machen").ihr("macht").sie("machen").partizip2("gemacht").build();
    private final Adjective schnell = Adjective.builder().de("schnell").ru("быстрый").level(WordLevel.A1).build();
    private final Adverb oft = Adverb.builder().de("oft").ru("часто").level(WordLevel.A2).build();
    private final Phrase phrase = Phrase.builder().de("auf jeden Fall").ru("в любом случае").level(WordLevel.B2).build();

    @Test
    void everyTaskIsPromptedInTheDefaultLanguage() {
        Map<TaskEnum, String> expected = Map.ofEntries(
            Map.entry(TaskEnum.SINGULAR_NOUN, "Next noun: *стол* (singular, A1)"),
            Map.entry(TaskEnum.PLURAL_NOUN, "Next noun: *стол* (plural, A1)"),
            Map.entry(TaskEnum.VERB, "Next verb: *делать* (A1)"),
            Map.entry(TaskEnum.ICH_VERB, "Next verb: ich *делать* (A1)"),
            Map.entry(TaskEnum.DU_VERB, "Next verb: du *делать* (A1)"),
            Map.entry(TaskEnum.ER_VERB, "Next verb: er *делать* (A1)"),
            Map.entry(TaskEnum.WIR_VERB, "Next verb: wir *делать* (A1)"),
            Map.entry(TaskEnum.IHR_VERB, "Next verb: ihr *делать* (A1)"),
            Map.entry(TaskEnum.SIE_VERB, "Next verb: Sie *делать* (A1)"),
            Map.entry(TaskEnum.PARTIZIP2, "Next verb: ich habe/ist *делать* (A1)"),
            Map.entry(TaskEnum.ADJECTIVE, "Next adjective: *быстрый* (A1)"),
            Map.entry(TaskEnum.ADVERB, "Next adverb: *часто* (A1)"),
            Map.entry(TaskEnum.PHRASE, "Next phrase: *в любом случае* (A1)"));

        for (Map.Entry<TaskEnum, String> entry : expected.entrySet()) {
            assertThat(presenter.task(promptCard(entry.getKey()), Locale.ROOT)).as(entry.getKey().name()).isEqualTo(entry.getValue());
        }
    }

    @Test
    void everyTaskIsPromptedInRussian() {
        Map<TaskEnum, String> expected = Map.ofEntries(
            Map.entry(TaskEnum.SINGULAR_NOUN, "Далее существительное: *стол* (в ед.числе, A1)"),
            Map.entry(TaskEnum.PLURAL_NOUN, "Далее существительное: *стол* (во мн.числе, A1)"),
            Map.entry(TaskEnum.VERB, "Далее глагол: *делать* (A1)"),
            Map.entry(TaskEnum.ICH_VERB, "Далее глагол: ich *делать* (A1)"),
            Map.entry(TaskEnum.DU_VERB, "Далее глагол: du *делать* (A1)"),
            Map.entry(TaskEnum.ER_VERB, "Далее глагол: er *делать* (A1)"),
            Map.entry(TaskEnum.WIR_VERB, "Далее глагол: wir *делать* (A1)"),
            Map.entry(TaskEnum.IHR_VERB, "Далее глагол: ihr *делать* (A1)"),
            Map.entry(TaskEnum.SIE_VERB, "Далее глагол: Sie *делать* (A1)"),
            Map.entry(TaskEnum.PARTIZIP2, "Далее глагол: ich habe/ist *делать* (A1)"),
            Map.entry(TaskEnum.ADJECTIVE, "Далее прилагательное: *быстрый* (A1)"),
            Map.entry(TaskEnum.ADVERB, "Далее наречие: *часто* (A1)"),
            Map.entry(TaskEnum.PHRASE, "Далее фраза: *в любом случае* (A1)"));

        for (Map.Entry<TaskEnum, String> entry : expected.entrySet()) {
            assertThat(presenter.task(promptCard(entry.getKey()), RU)).as(entry.getKey().name()).isEqualTo(entry.getValue());
        }
    }

    @Test
    void aMissingLocaleFallsBackToTheDefaultLanguage() {
        WordCard card = promptCard(TaskEnum.ADJECTIVE);

        assertThat(presenter.task(card, null)).isEqualTo(presenter.task(card, Locale.ROOT));
    }

    @Test
    void theTaskPromptCarriesTheLevelOfTheWord() {
        Adjective loud = Adjective.builder().de("laut").ru("громкий").level(WordLevel.C1).build();

        assertThat(presenter.task(GameCardFactory.rebuildCard(loud, TaskEnum.ADJECTIVE), Locale.ROOT)).contains("C1");
    }

    @Test
    void theHintListsTheFormsOfEachKindOfWord() {
        assertThat(presenter.hint(tisch)).isEqualTo("```A1\nder Tisch, die Tische```");
        assertThat(presenter.hint(lampe)).as("a noun without a plural").isEqualTo("```A2\ndie Lampe```");
        assertThat(presenter.hint(kind)).isEqualTo("```A1\ndas Kind, die Kinder```");
        assertThat(presenter.hint(eltern)).as("a noun that exists only in the plural").isEqualTo("```B1\ndie Eltern```");
        assertThat(presenter.hint(machen)).isEqualTo("```A1\nich | mache\ndu  | machst\ner  | macht\nwir | machen\nihr | macht\nSie | machen\nPartizip II: gemacht```");
        assertThat(presenter.hint(schnell)).isEqualTo("```A1\nschnell```");
        assertThat(presenter.hint(oft)).isEqualTo("```A2\noft```");
        assertThat(presenter.hint(phrase)).isEqualTo("```B2\nauf jeden Fall```");
    }

    @Test
    void aRightAnswerIsConfirmedAndFollowedByTheFormsAndTheExample() {
        WordCard card = answered(tisch, TaskEnum.SINGULAR_NOUN, true);

        assertThat(presenter.feedback(card, Locale.ROOT)).isEqualTo("*Right*✅\n\n```A1\nder Tisch, die Tische```\n\n🇩🇪 Der Tisch ist groß.\n🇷🇺 Стол большой.\n🇬🇧 The table is big.");
        assertThat(presenter.feedback(card, RU)).isEqualTo("*Правильно*✅\n\n```A1\nder Tisch, die Tische```\n\n🇩🇪 Der Tisch ist groß.\n🇷🇺 Стол большой.\n🇬🇧 The table is big.");
    }

    @Test
    void aWrongAnswerShowsTheRightOneWithACircleThatTellsTheGenderOfANoun() {
        assertThat(presenter.feedback(answered(tisch, TaskEnum.SINGULAR_NOUN, false), Locale.ROOT)).as("masculine").isEqualTo("*Wrong*⚠️ Right answer: *der Tisch* 🔵\n\n```A1\nder Tisch, die Tische```\n\n🇩🇪 Der Tisch ist groß.\n🇷🇺 Стол большой.\n🇬🇧 The table is big.");
        assertThat(presenter.feedback(answered(lampe, TaskEnum.SINGULAR_NOUN, false), Locale.ROOT)).as("feminine").isEqualTo("*Wrong*⚠️ Right answer: *die Lampe* 🔴\n\n```A2\ndie Lampe```");
        assertThat(presenter.feedback(answered(kind, TaskEnum.SINGULAR_NOUN, false), Locale.ROOT)).as("neuter").isEqualTo("*Wrong*⚠️ Right answer: *das Kind* 🟢\n\n```A1\ndas Kind, die Kinder```\n\n🇩🇪 Das Kind spielt.");
        assertThat(presenter.feedback(answered(eltern, TaskEnum.SINGULAR_NOUN, false), Locale.ROOT)).as("plural only").isEqualTo("*Wrong*⚠️ Right answer: *die Eltern* 🔴\n\n```B1\ndie Eltern```");
    }

    @Test
    void aWrongAnswerToThePluralTaskShowsThePlural() {
        assertThat(presenter.feedback(answered(tisch, TaskEnum.PLURAL_NOUN, false), Locale.ROOT)).isEqualTo("*Wrong*⚠️ Right answer: *die Tische* 🔵\n\n```A1\nder Tisch, die Tische```\n\n🇩🇪 Der Tisch ist groß.\n🇷🇺 Стол большой.\n🇬🇧 The table is big.");
    }

    @Test
    void aWrongAnswerForOtherWordsHasNoCircle() {
        assertThat(presenter.feedback(answered(machen, TaskEnum.ICH_VERB, false), Locale.ROOT)).isEqualTo("*Wrong*⚠️ Right answer: *mache*\n\n```A1\nich | mache\ndu  | machst\ner  | macht\nwir | machen\nihr | macht\nSie | machen\nPartizip II: gemacht```");
        assertThat(presenter.feedback(answered(schnell, TaskEnum.ADJECTIVE, false), Locale.ROOT)).isEqualTo("*Wrong*⚠️ Right answer: *schnell*\n\n```A1\nschnell```");
        assertThat(presenter.feedback(answered(phrase, TaskEnum.PHRASE, false), Locale.ROOT)).isEqualTo("*Wrong*⚠️ Right answer: *auf jeden Fall*\n\n```B2\nauf jeden Fall```");
    }

    @Test
    void theVerdictIsGivenInTheLanguageOfTheUser() {
        assertThat(presenter.feedback(answered(tisch, TaskEnum.SINGULAR_NOUN, false), RU)).isEqualTo("*Неверно*⚠️ Правильный ответ: *der Tisch* 🔵\n\n```A1\nder Tisch, die Tische```\n\n🇩🇪 Der Tisch ist groß.\n🇷🇺 Стол большой.\n🇬🇧 The table is big.");
        assertThat(presenter.feedback(answered(lampe, TaskEnum.SINGULAR_NOUN, false), RU)).isEqualTo("*Неверно*⚠️ Правильный ответ: *die Lampe* 🔴\n\n```A2\ndie Lampe```");
    }

    @Test
    void noPlaceholderIsLeftUnresolved() {
        for (Locale locale : List.of(Locale.ROOT, RU)) {
            for (WordCard card : List.of(answered(tisch, TaskEnum.SINGULAR_NOUN, false), answered(machen, TaskEnum.ICH_VERB, false),
                    answered(schnell, TaskEnum.ADJECTIVE, false), answered(phrase, TaskEnum.PHRASE, false))) {
                assertThat(presenter.feedback(card, locale)).doesNotContain("{").doesNotContain("}").doesNotContain("null");
            }
        }
    }

    @Test
    void theExampleLeavesOutASentenceThatHasNoTranslationYet() {
        String feedback = presenter.feedback(answered(kind, TaskEnum.SINGULAR_NOUN, true), Locale.ROOT);

        assertThat(feedback).endsWith("\n\n🇩🇪 Das Kind spielt.").doesNotContain("null");
    }

    @Test
    void thereIsNoExampleWhenTheWordHasNoSample() {
        assertThat(presenter.feedback(answered(lampe, TaskEnum.SINGULAR_NOUN, true), Locale.ROOT)).endsWith("```");
        assertThat(presenter.feedback(answered(noSample("  "), TaskEnum.SINGULAR_NOUN, true), Locale.ROOT)).endsWith("```");
    }

    @Test
    void theAnnouncementListsTheNewWordsWithTheirForms() {
        List<WordCard> cards = List.of(GameCardFactory.rebuildCard(tisch, TaskEnum.SINGULAR_NOUN),
                GameCardFactory.rebuildCard(lampe, TaskEnum.SINGULAR_NOUN));

        assertThat(presenter.newWordsPreview(cards, Locale.ROOT)).isEqualTo("New game. Next group of words to learn:\n\n🇩🇪*Tisch* | 🇷🇺*стол* | 🇬🇧*table* \n```A1\nder Tisch, die Tische```\n\n🇩🇪*Lampe* | 🇷🇺*лампа* \n```A2\ndie Lampe```\n\n");
    }

    private Noun noSample(String sample) {
        return Noun.builder().de("Katze").ru("кошка").level(WordLevel.A1).gender(GenderType.F).sample(sample).build();
    }

    private static WordCard answered(Word word, TaskEnum task, boolean right) {
        WordCard card = GameCardFactory.rebuildCard(word, task);

        card.setUserAnswer(right ? card.getRightAnswer().get() : "falsch");

        return card;
    }

    /** A card for each task; the prompt depends only on the Russian word and the level (A1 here). */
    private WordCard promptCard(TaskEnum task) {
        Word word = switch (task) {
            case SINGULAR_NOUN, PLURAL_NOUN -> tisch;
            case VERB, ICH_VERB, DU_VERB, ER_VERB, WIR_VERB, IHR_VERB, SIE_VERB, PARTIZIP2 -> machen;
            case ADJECTIVE -> schnell;
            case ADVERB -> Adverb.builder().de("oft").ru("часто").level(WordLevel.A1).build();
            case PHRASE -> Phrase.builder().de("auf jeden Fall").ru("в любом случае").level(WordLevel.A1).build();
        };

        return GameCardFactory.rebuildCard(word, task);
    }
}
