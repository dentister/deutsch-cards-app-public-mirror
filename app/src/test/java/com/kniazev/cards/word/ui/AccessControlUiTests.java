package com.kniazev.cards.word.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class AccessControlUiTests extends AbstractViewUiTest {

    private static final Pattern LOGIN_URL = Pattern.compile(".*/login(\\?.*)?");

    @ParameterizedTest
    @ValueSource(strings = {"/", "/dictionary", "/nouns", "/verbs", "/adjectives", "/adverbs", "/phrases", "/users"})
    void anonymousVisitorIsSentToLogin(String path) {
        page.navigate(url(path));

        assertThat(page).hasURL(LOGIN_URL);
        assertThat(new LoginPage(page).username()).isVisible();
    }

    @Test
    void wrongPasswordIsRejected() {
        testData.user(LEARNER);

        page.navigate(url("/login"));
        new LoginPage(page).submit(LEARNER, "not-the-password");

        assertThat(new LoginPage(page).error()).isVisible();
        assertThat(page).hasURL(Pattern.compile(".*/login\\?error"));
    }

    @Test
    void learnerLandsOnDictionaryAndSeesNoAdminTools() {
        signInAsLearner();

        assertThat(page).hasURL(url("/dictionary"));
        assertThat(shell().title()).hasText("Dictionary");
        assertThat(shell().navItem("Nouns")).isVisible();
        assertThat(shell().navItem("Users")).hasCount(0);

        assertThat(grid().menuButton("refresh")).isVisible();
        assertThat(grid().menuButton("plus")).hasCount(0);
        assertThat(grid().menuButton("pencil")).hasCount(0);
        assertThat(grid().menuButton("trash")).hasCount(0);
    }

    @Test
    void learnerCannotOpenUsersPage() {
        signInAsLearner();

        page.navigate(url("/users"));

        assertThat(page.locator("vaadin-grid")).hasCount(0);
    }

    @Test
    void adminSeesAndOpensUsersPage() {
        signInAsAdmin();

        assertThat(page).hasURL(url("/dictionary"));
        assertThat(shell().navItem("Users")).isVisible();
        assertThat(grid().menuButton("plus")).isVisible();

        shell().navigateTo("Users");

        assertThat(page).hasURL(url("/users"));
        assertThat(shell().title()).hasText("Users");
        assertThat(grid().cell("Login")).isVisible();
    }

    @Test
    void logoutClosesTheSession() {
        signInAsLearner();

        shell().logout();

        assertThat(page).hasURL(LOGIN_URL);

        page.navigate(url("/dictionary"));

        assertThat(page).hasURL(LOGIN_URL);
    }
}
