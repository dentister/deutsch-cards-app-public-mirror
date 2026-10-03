package com.kniazev.cards.word.ui;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/** The Vaadin {@code LoginForm} on {@code /login}. */
record LoginPage(Page page) {

    /** Labels are matched exactly: "Password" is also a substring of the reveal button's "Show password". */
    Locator username() {
        return page.getByLabel("Username", new Page.GetByLabelOptions().setExact(true));
    }

    Locator error() {
        return page.getByText("Incorrect username or password");
    }

    void submit(String username, String password) {
        username().fill(username);
        page.getByLabel("Password", new Page.GetByLabelOptions().setExact(true)).fill(password);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Log in")).click();
    }
}
