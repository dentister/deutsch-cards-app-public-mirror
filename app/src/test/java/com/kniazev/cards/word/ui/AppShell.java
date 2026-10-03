package com.kniazev.cards.word.ui;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/** The frame around every page behind the login: side navigation, the view title and the logout button. */
record AppShell(Page page) {

    /**
     * A side-navigation entry, found by its {@code path} attribute, which is the lower-cased label ("Verbs" is
     * {@code verbs}). Matching on text instead is ambiguous: "Verbs" is a substring of "Adverbs".
     */
    Locator navItem(String label) {
        return page.locator("vaadin-side-nav-item[path='" + label.toLowerCase() + "']");
    }

    /**
     * Clicks the entry and waits until the new page has replaced the old one. The click only starts the navigation: the
     * client asks the server for the new view and swaps it in afterwards, so a test that went on at once would still be
     * working on the old page, and what it typed there would be wiped by the swap.
     */
    void navigateTo(String label) {
        navItem(label).click();

        page.waitForURL("**/" + label.toLowerCase());

        VaadinClient.awaitIdle(page);
    }

    /** The title in the navbar, taken from the view's {@code @PageTitle}. */
    Locator title() {
        return page.locator("vaadin-app-layout > h2");
    }

    void logout() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Logout")).click();
    }
}
