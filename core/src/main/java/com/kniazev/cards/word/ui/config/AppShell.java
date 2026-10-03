package com.kniazev.cards.word.ui.config;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;

/**
 * The Vaadin application shell. It is kept with the views and the {@code myapp} theme, not with the Spring Boot main
 * class, so that everything Vaadin needs for its frontend build lives in the UI and the rest of the application does not
 * have to know about Vaadin.
 */
@Theme("myapp")
public class AppShell implements AppShellConfigurator {
}
