package com.kniazev.cards.word.ui;

import com.microsoft.playwright.Page;

/** Synchronisation with the Vaadin client; Playwright's own auto-waiting knows nothing about its server round trips. */
final class VaadinClient {

    private VaadinClient() {
    }

    /**
     * Waits until the client has no request in flight. Without it, text typed right after a navigation can be
     * overwritten by the late server response and never reach the server (seen as a filter that silently does nothing).
     */
    static void awaitIdle(Page page) {
        page.waitForFunction("""
                () => {
                    const clients = window.Vaadin && window.Vaadin.Flow && window.Vaadin.Flow.clients;

                    return !!clients && !Object.values(clients).some(client => client.isActive && client.isActive());
                }""");
    }
}
