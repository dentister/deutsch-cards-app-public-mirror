package com.kniazev.cards.word.ui;

import com.kniazev.cards.word.support.AbstractUiTest;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** What the browser tests of the pages behind the login share: signing in and the page objects. */
abstract class AbstractViewUiTest extends AbstractUiTest {

    static final String LEARNER = "learner";
    static final String ADMIN = "admin";

    /** Creates the account (password {@code <username>-password}, see TestData) and signs in through the form. */
    void signInAsLearner() {
        testData.user(LEARNER);

        signIn(LEARNER);
    }

    void signInAsAdmin() {
        testData.admin(ADMIN);

        signIn(ADMIN);
    }

    /** Signs in and waits for the redirect to the first page the account may open. */
    void signIn(String username) {
        page.navigate(url("/login"));
        new LoginPage(page).submit(username, username + "-password");

        assertThat(page.locator("vaadin-app-layout")).isVisible();

        VaadinClient.awaitIdle(page);
    }

    AppShell shell() {
        return new AppShell(page);
    }

    GridPage grid() {
        return new GridPage(page);
    }

    WordDialogPage dialog() {
        return new WordDialogPage(page);
    }
}
