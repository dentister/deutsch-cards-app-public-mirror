package com.kniazev.cards.word.ui;

import com.microsoft.playwright.Locator;

import java.util.regex.Pattern;

/**
 * Playwright hands a {@link Pattern} to the browser as a JavaScript regular expression, so {@link Pattern#quote} (which
 * produces {@code \Q..\E}) cannot be used: JavaScript does not know that syntax.
 */
final class TextMatch {

    private TextMatch() {
    }

    /** Matches an element whose whole text is {@code text}; "Verbs" does not match "Adverbs". */
    static Locator.FilterOptions exactText(String text) {
        return new Locator.FilterOptions().setHasText(Pattern.compile("^\\s*" + escape(text) + "\\s*$"));
    }

    private static String escape(String text) {
        return text.replaceAll("[\\\\^$.*+?()\\[\\]{}|/-]", "\\\\$0");
    }
}
