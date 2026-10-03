package com.kniazev.cards.word.ui;

import com.kniazev.cards.word.model.dictionary.Noun;
import com.kniazev.cards.word.model.dictionary.Noun.GenderType;
import com.kniazev.cards.word.model.dictionary.Verb;
import com.kniazev.cards.word.model.dictionary.Word;
import com.kniazev.cards.word.model.dictionary.Word.WordLevel;
import com.kniazev.cards.word.model.dictionary.Word.WordType;
import com.kniazev.cards.word.service.WordService;
import com.kniazev.cards.word.support.TestData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * An admin adds, changes and deletes words on the Dictionary page. Every outcome is checked twice: in the grid, and in
 * the database through {@link WordService}, because the grid alone would not show a save that went to the wrong row.
 * <p>
 * Words made here start with {@link TestData#FIXTURE_PREFIX}, which is how {@code TestData.reset()} finds them again.
 */
class AdminWordCrudUiTests extends AbstractViewUiTest {

    private static final String DE = TestData.FIXTURE_PREFIX + "haus";

    @Autowired
    private WordService wordService;

    @BeforeEach
    void openDictionaryAsAdmin() {
        signInAsAdmin();
    }

    @Test
    void createsNoun() {
        grid().menuButton("plus").click();
        grid().choose("New Noun");

        assertThat(dialog().overlay()).isVisible();

        dialog().fill("DE", DE);
        dialog().fill("RU", "uitest-дом");
        dialog().fill("Plural", "uitest-häuser");
        dialog().selectGender("N");
        dialog().selectLevel("B1");
        dialog().addTag(TestData.FIXTURE_TAG);
        dialog().save();

        assertThat(dialog().overlay()).hasCount(0);

        grid().filter(DE);
        assertThat(grid().cell(DE)).isVisible();
        assertThat(grid().cell("uitest-дом")).isVisible();

        Noun saved = (Noun) wordService.findOneByDeAndWordType(DE, WordType.NOUN).orElseThrow();

        assertThat(saved.getRu()).isEqualTo("uitest-дом");
        assertThat(saved.getPlural()).isEqualTo("uitest-häuser");
        assertThat(saved.getGender()).isEqualTo(GenderType.N);
        assertThat(saved.getLevel()).isEqualTo(WordLevel.B1);
        assertThat(saved.getTags()).containsExactly(TestData.FIXTURE_TAG);
    }

    @Test
    void createsVerb() {
        grid().menuButton("plus").click();
        grid().choose("New Verb");

        dialog().fill("DE", TestData.FIXTURE_PREFIX + "gehen");
        dialog().fill("RU", "uitest-идти");
        dialog().selectLevel("A2");
        dialog().fill("Ich", "gehe");
        dialog().fill("Du", "gehst");
        dialog().fill("Er/Sie/Es", "geht");
        dialog().fill("Wir", "gehen");
        dialog().fill("Ihr", "geht");
        dialog().fill("Sie/sie", "gehen");
        dialog().fill("Partizip II", "gegangen");
        dialog().save();

        assertThat(dialog().overlay()).hasCount(0);

        Verb saved = (Verb) wordService.findOneByDeAndWordType(TestData.FIXTURE_PREFIX + "gehen", WordType.VERB)
                .orElseThrow();

        assertThat(saved.getLevel()).isEqualTo(WordLevel.A2);
        assertThat(saved.getIch()).isEqualTo("gehe");
        assertThat(saved.getPartizip2()).isEqualTo("gegangen");

        grid().filter(TestData.FIXTURE_PREFIX + "gehen");
        assertThat(grid().cell(TestData.FIXTURE_PREFIX + "gehen")).isVisible();
    }

    @ParameterizedTest
    @CsvSource({
            "New Adjective, ADJECTIVE",
            "New Adverb,    ADVERB",
            "New Phrase,    PHRASE"})
    void createsWordsWithoutExtraFieldsAndKeepsTheirLevel(String menuItem, WordType type) {
        String de = TestData.FIXTURE_PREFIX + type.name().toLowerCase();

        grid().menuButton("plus").click();
        grid().choose(menuItem);

        dialog().fill("DE", de);
        dialog().fill("RU", "uitest-перевод");
        dialog().selectLevel("B2");
        dialog().save();

        assertThat(dialog().overlay()).hasCount(0);

        grid().filter(de);
        assertThat(grid().cell(de)).isVisible();

        assertThat(wordService.findOneByDeAndWordType(de, type)).get()
                .extracting(Word::getLevel).isEqualTo(WordLevel.B2);
    }

    @Test
    void cancelDiscardsTheNewWord() {
        grid().menuButton("plus").click();
        grid().choose("New Phrase");

        dialog().fill("DE", DE);
        dialog().fill("RU", "uitest-дом");
        dialog().cancel();

        assertThat(dialog().overlay()).hasCount(0);

        grid().filter(DE);
        assertThat(grid().cell(DE)).hasCount(0);
        assertThat(wordService.findOneByDeAndWordType(DE, WordType.PHRASE)).isEmpty();
    }

    @Test
    void editsWordFromMenu() {
        testData.noun(DE, "uitest-дом", "uitest-häuser", GenderType.N);

        grid().filter(DE);
        grid().selectRow(DE);
        grid().menuButton("pencil").click();

        assertThat(dialog().field("DE")).hasValue(DE);

        dialog().fill("RU", "uitest-здание");
        dialog().save();

        assertThat(dialog().overlay()).hasCount(0);
        assertThat(grid().cell("uitest-здание")).isVisible();
        assertThat(grid().cell("uitest-дом")).hasCount(0);

        assertThat(wordService.findOneByDeAndWordType(DE, WordType.NOUN)).get()
                .extracting(Word::getRu).isEqualTo("uitest-здание");
    }

    @Test
    void editsWordByDoubleClick() {
        testData.noun(DE, "uitest-дом", "uitest-häuser", GenderType.N);

        grid().filter(DE);
        grid().cell(DE).dblclick();

        assertThat(dialog().field("RU")).hasValue("uitest-дом");

        dialog().fill("RU", "uitest-жилище");
        dialog().save();

        assertThat(grid().cell("uitest-жилище")).isVisible();
        assertThat(wordService.findOneByDeAndWordType(DE, WordType.NOUN)).get()
                .extracting(Word::getRu).isEqualTo("uitest-жилище");
    }

    @Test
    void editAndDeleteStartDisabledUntilARowIsSelected() {
        testData.noun(DE, "uitest-дом", "uitest-häuser", GenderType.N);

        grid().filter(DE);

        assertThat(grid().menuButton("pencil")).hasAttribute("disabled", "");
        assertThat(grid().menuButton("trash")).hasAttribute("disabled", "");

        grid().selectRow(DE);

        assertThat(grid().menuButton("pencil")).not().hasAttribute("disabled", "");
        assertThat(grid().menuButton("trash")).not().hasAttribute("disabled", "");
    }

    @Test
    void deletesSelectedWord() {
        testData.noun(DE, "uitest-дом", "uitest-häuser", GenderType.N);
        testData.noun(TestData.FIXTURE_PREFIX + "baum", "uitest-дерево", "uitest-bäume", GenderType.M);

        grid().filter(TestData.FIXTURE_PREFIX);
        grid().selectRow(DE);
        grid().menuButton("trash").click();

        assertThat(grid().cell(DE)).hasCount(0);
        assertThat(grid().cell(TestData.FIXTURE_PREFIX + "baum")).isVisible();

        assertThat(wordService.findOneByDeAndWordType(DE, WordType.NOUN)).isEmpty();
        assertThat(wordService.findOneByDeAndWordType(TestData.FIXTURE_PREFIX + "baum", WordType.NOUN)).isPresent();
    }

    @Test
    void createsWordsFromJson() {
        String de = TestData.FIXTURE_PREFIX + "json";

        grid().menuButton("plus").click();
        grid().choose("As Json");

        dialog().fill("JSON", "{\"wordType\":\"PHRASE\",\"de\":\"" + de + "\",\"ru\":\"uitest-джейсон\",\"level\":\"A1\"}");
        dialog().save();

        assertThat(dialog().overlay()).hasCount(0);

        grid().filter(de);
        assertThat(grid().cell(de)).isVisible();
        assertThat(wordService.findOneByDeAndWordType(de, WordType.PHRASE)).isPresent();
    }

    @Test
    void invalidJsonKeepsTheDialogOpenAndShowsAnError() {
        grid().menuButton("plus").click();
        grid().choose("As Json");

        dialog().fill("JSON", "{ this is not json");
        dialog().save();

        assertThat(page.locator("vaadin-notification-card")).containsText("Not valid JSON");
        assertThat(dialog().overlay()).isVisible();
    }
}
