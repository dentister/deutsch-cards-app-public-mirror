package com.kniazev.cards.word.ui;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/** The word dialogs ({@code WordDialog} and its subclasses, {@code JsonWordDialog}); only one is open at a time. */
record WordDialogPage(Page page) {

    /**
     * The open dialog. Closed overlays stay in the DOM, and in dev mode so do those of Vaadin Copilot, so the
     * {@code opened} attribute is what tells ours apart.
     */
    Locator overlay() {
        return page.locator("vaadin-dialog-overlay[opened]");
    }

    /** A text field by its exact label: "DE" is also a substring of "Gender". */
    Locator field(String label) {
        return overlay().getByLabel(label, new Locator.GetByLabelOptions().setExact(true));
    }

    void fill(String label, String value) {
        field(label).fill(value);
    }

    void selectLevel(String level) {
        overlay().locator("vaadin-select").click();
        page.locator("vaadin-select-item").filter(TextMatch.exactText(level)).click();
    }

    void selectGender(String gender) {
        overlay().locator("vaadin-radio-button").filter(TextMatch.exactText(gender)).click();
    }

    /** The tag field adds a chip on ENTER. */
    void addTag(String tag) {
        Locator tags = field("Tags");

        tags.fill(tag);
        tags.press("Enter");
    }

    void save() {
        overlay().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save")).click();
    }

    void cancel() {
        overlay().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel")).click();
    }
}
