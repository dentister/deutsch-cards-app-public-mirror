package com.kniazev.cards.word.ui;

import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class NavigationUiTests extends AbstractViewUiTest {

    /** Side-navigation label, route, and a column header that only that page has. */
    private record Destination(String label, String path, String header) {
    }

    private static final Destination[] WORD_PAGES = {
            new Destination("Dictionary", "/dictionary", "Word Type"),
            new Destination("Nouns", "/nouns", "Der"),
            new Destination("Verbs", "/verbs", "Infinitiv"),
            new Destination("Adjectives", "/adjectives", "Adjective"),
            new Destination("Adverbs", "/adverbs", "Adverb"),
            new Destination("Phrases", "/phrases", "Phrase")
    };

    @Test
    void learnerClicksThroughAllWordPages() {
        signInAsLearner();

        visitAll(WORD_PAGES);
    }

    @Test
    void adminClicksThroughAllPagesIncludingUsers() {
        signInAsAdmin();

        visitAll(WORD_PAGES);

        visit(new Destination("Users", "/users", "Login"));
    }

    @Test
    void backAndForwardFollowTheClicks() {
        signInAsLearner();

        shell().navigateTo("Nouns");
        assertThat(page).hasURL(url("/nouns"));

        shell().navigateTo("Verbs");
        assertThat(page).hasURL(url("/verbs"));

        page.goBack();
        assertThat(page).hasURL(url("/nouns"));
        assertThat(shell().title()).hasText("Nouns");

        page.goForward();
        assertThat(page).hasURL(url("/verbs"));
        assertThat(shell().title()).hasText("Verbs");
    }

    private void visitAll(Destination... destinations) {
        // Forward, then back again, so that each page is entered from two different neighbours.
        for (Destination destination : destinations) {
            visit(destination);
        }

        for (int i = destinations.length - 1; i >= 0; i--) {
            visit(destinations[i]);
        }
    }

    private void visit(Destination destination) {
        shell().navigateTo(destination.label());

        assertThat(page).hasURL(url(destination.path()));
        assertThat(shell().title()).hasText(destination.label());
        assertThat(grid().cell(destination.header())).isVisible();
    }
}
