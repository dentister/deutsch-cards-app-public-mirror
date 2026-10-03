package com.kniazev.cards.word.ui;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * The table pages ({@code AbstractTableView}): filter bar, icon-only menu bar and the grid. None of these components
 * has an id, so they are found by placeholder, icon and text.
 */
record GridPage(Page page) {

    Locator filterField() {
        // The placeholder is on both the vaadin-text-field host and its inner input; fill() needs the input.
        return page.locator("vaadin-text-field input[placeholder='Filter by value']");
    }

    void filter(String text) {
        filterField().fill(text);
    }

    /** The Dictionary-only word type filter; {@code "All"} clears it. */
    void selectType(String caption) {
        page.locator("vaadin-select").click();
        page.locator("vaadin-select-item").filter(TextMatch.exactText(caption)).click();
    }

    /**
     * Picks one value in the tags filter. Like the grid, the overlay recycles its items and leaves the ones it no
     * longer needs in the DOM, hidden, so only visible items count.
     */
    void selectTag(String tag) {
        Locator input = page.locator("vaadin-multi-select-combo-box input");

        input.click();
        input.fill(tag);
        page.locator("vaadin-multi-select-combo-box-item:visible").filter(TextMatch.exactText(tag)).click();
        input.press("Escape");
    }

    /**
     * A grid cell whose whole text is {@code text}, header cells included. The grid recycles its rows and leaves the
     * ones it no longer needs in the DOM, hidden, so only visible cells count.
     */
    Locator cell(String text) {
        return visibleCells().filter(TextMatch.exactText(text));
    }

    /** A grid cell whose text contains {@code text}, for columns that decorate the value (nouns, tag lists). */
    Locator cellContaining(String text) {
        return visibleCells().filter(new Locator.FilterOptions().setHasText(text));
    }

    private Locator visibleCells() {
        return page.locator("vaadin-grid-cell-content:visible");
    }

    void selectRow(String cellText) {
        cell(cellText).click();
    }

    /** One of the icon-only menu bar buttons: {@code refresh}, {@code plus}, {@code pencil} or {@code trash}. */
    Locator menuButton(String icon) {
        return page.locator("vaadin-menu-bar-button:has(vaadin-icon[icon='vaadin:" + icon + "'])");
    }

    void choose(String menuItemText) {
        page.locator("vaadin-menu-bar-item").filter(TextMatch.exactText(menuItemText)).click();
    }
}
