package com.kniazev.cards.word.ui;

import com.kniazev.cards.word.model.dictionary.Noun.GenderType;
import com.kniazev.cards.word.model.dictionary.Word.WordLevel;
import com.kniazev.cards.word.support.TestData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Every page is seeded with two words of its own kind, "alpha" tagged {@code uitest-a} and "beta" tagged
 * {@code uitest-b}. The Dictionary grid also holds the whole seeded dictionary, so its filters are always narrowed down
 * to the fixtures first.
 */
class FilterUiTests extends AbstractViewUiTest {

    /** The pages that list one kind of word; the label is the side-navigation entry. */
    enum WordPage {
        NOUNS("Nouns"),
        VERBS("Verbs"),
        ADJECTIVES("Adjectives"),
        ADVERBS("Adverbs"),
        PHRASES("Phrases");

        final String label;

        WordPage(String label) {
            this.label = label;
        }

        String alpha() {
            return "uitest-alpha-" + name().toLowerCase();
        }

        String beta() {
            return "uitest-beta-" + name().toLowerCase();
        }
    }

    @BeforeEach
    void seedWords() {
        testData.noun(WordPage.NOUNS.alpha(), "uitest-альфа-существительное", "uitest-Alphas", GenderType.M, "uitest-a");
        testData.noun(WordPage.NOUNS.beta(), "uitest-бета-существительное", "uitest-Betas", GenderType.F, "uitest-b");

        testData.verb(WordPage.VERBS.alpha(), "uitest-альфа-глагол", "ich1", "du1", "er1", "wir1", "ihr1", "sie1");
        testData.verb(WordPage.VERBS.beta(), "uitest-бета-глагол", "ich2", "du2", "er2", "wir2", "ihr2", "sie2");

        testData.adjective(WordPage.ADJECTIVES.alpha(), "uitest-альфа-прилагательное", WordLevel.A1, "uitest-a");
        testData.adjective(WordPage.ADJECTIVES.beta(), "uitest-бета-прилагательное", WordLevel.A1, "uitest-b");

        testData.adverb(WordPage.ADVERBS.alpha(), "uitest-альфа-наречие", "uitest-a");
        testData.adverb(WordPage.ADVERBS.beta(), "uitest-бета-наречие", "uitest-b");

        testData.phrase(WordPage.PHRASES.alpha(), "uitest-альфа-фраза", "uitest-a");
        testData.phrase(WordPage.PHRASES.beta(), "uitest-бета-фраза", "uitest-b");

        testData.refreshWordCaches();
    }

    // ---- Dictionary -------------------------------------------------------------------------------------------------

    @Test
    void dictionaryTextFilterMatchesGermanIgnoringCase() {
        signInAsLearner();

        grid().filter("UITEST-ALPHA-NOUNS");

        assertThat(grid().cell(WordPage.NOUNS.alpha())).isVisible();
        assertThat(grid().cell(WordPage.NOUNS.beta())).hasCount(0);
        assertThat(grid().cell(WordPage.VERBS.alpha())).hasCount(0);
    }

    @Test
    void dictionaryTextFilterMatchesRussian() {
        signInAsLearner();

        grid().filter("uitest-бета-глагол");

        assertThat(grid().cell(WordPage.VERBS.beta())).isVisible();
        assertThat(grid().cell(WordPage.VERBS.alpha())).hasCount(0);
    }

    @Test
    void dictionaryTextFilterCanBeBroadenedAgain() {
        signInAsLearner();

        grid().filter("uitest-alpha-nouns");
        assertThat(grid().cell(WordPage.NOUNS.beta())).hasCount(0);

        grid().filter("uitest-");
        assertThat(grid().cell(WordPage.NOUNS.alpha())).isVisible();
        assertThat(grid().cell(WordPage.NOUNS.beta())).isVisible();
    }

    @Test
    void dictionaryTypeFilterKeepsOnlyThatKind() {
        signInAsLearner();

        grid().filter("uitest-");
        assertThat(grid().cell(WordPage.NOUNS.alpha())).isVisible();
        assertThat(grid().cell(WordPage.VERBS.alpha())).isVisible();

        grid().selectType("VERB");

        assertThat(grid().cell(WordPage.VERBS.alpha())).isVisible();
        assertThat(grid().cell(WordPage.VERBS.beta())).isVisible();
        assertThat(grid().cell(WordPage.NOUNS.alpha())).hasCount(0);
        assertThat(grid().cell(WordPage.PHRASES.alpha())).hasCount(0);

        grid().selectType("All");

        assertThat(grid().cell(WordPage.NOUNS.alpha())).isVisible();
    }

    @Test
    void dictionaryTagFilterKeepsOnlyTaggedWords() {
        signInAsLearner();

        grid().selectTag("uitest-a");

        assertThat(grid().cell(WordPage.NOUNS.alpha())).isVisible();
        assertThat(grid().cell(WordPage.PHRASES.alpha())).isVisible();
        assertThat(grid().cell(WordPage.NOUNS.beta())).hasCount(0);
        assertThat(grid().cell(WordPage.PHRASES.beta())).hasCount(0);
    }

    @Test
    void dictionaryFiltersCombine() {
        signInAsLearner();

        grid().selectTag("uitest-a");
        grid().selectType("ADJECTIVE");

        assertThat(grid().cell(WordPage.ADJECTIVES.alpha())).isVisible();
        assertThat(grid().cell(WordPage.NOUNS.alpha())).hasCount(0);

        grid().filter("no-such-word");

        assertThat(grid().cell(WordPage.ADJECTIVES.alpha())).hasCount(0);
    }

    // ---- Per-kind pages ---------------------------------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(WordPage.class)
    void pageTextFilterNarrowsTheList(WordPage wordPage) {
        signInAsLearner();
        shell().navigateTo(wordPage.label);

        // The unfiltered page lists the whole seeded dictionary of this kind and the grid only renders what fits on
        // the screen, so the fixtures are first brought into view by their common prefix.
        grid().filter(TestData.FIXTURE_PREFIX);

        assertThat(grid().cellContaining(wordPage.alpha())).isVisible();
        assertThat(grid().cellContaining(wordPage.beta())).isVisible();

        grid().filter(wordPage.alpha());

        assertThat(grid().cellContaining(wordPage.alpha())).isVisible();
        assertThat(grid().cellContaining(wordPage.beta())).hasCount(0);
    }

    @ParameterizedTest
    @EnumSource(value = WordPage.class, names = "VERBS", mode = EnumSource.Mode.EXCLUDE)
    void pageTagFilterNarrowsTheList(WordPage wordPage) {
        signInAsLearner();
        shell().navigateTo(wordPage.label);

        grid().selectTag("uitest-b");

        assertThat(grid().cellContaining(wordPage.beta())).isVisible();
        assertThat(grid().cellContaining(wordPage.alpha())).hasCount(0);
    }

    @Test
    void verbsPageFiltersByConjugatedForm() {
        signInAsLearner();
        shell().navigateTo("Verbs");

        grid().filter("wir2");

        assertThat(grid().cellContaining(WordPage.VERBS.beta())).isVisible();
        assertThat(grid().cellContaining(WordPage.VERBS.alpha())).hasCount(0);
    }

    // ---- Users ------------------------------------------------------------------------------------------------------

    @Test
    void usersPageFiltersByLogin() {
        testData.user("uitest-anna");
        testData.user("uitest-boris");

        signInAsAdmin();
        shell().navigateTo("Users");

        assertThat(grid().cell("uitest-anna")).isVisible();
        assertThat(grid().cell("uitest-boris")).isVisible();

        grid().filter("anna");

        assertThat(grid().cell("uitest-anna")).isVisible();
        assertThat(grid().cell("uitest-boris")).hasCount(0);
    }
}
