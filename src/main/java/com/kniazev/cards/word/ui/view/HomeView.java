package com.kniazev.cards.word.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;

import java.util.List;

import jakarta.annotation.security.PermitAll;

/**
 * Both login flows send the user to "/", so it has to be open to every signed-in account: it forwards to the first
 * page from {@link #LANDING_PAGES} that the account may open.
 */
@PermitAll
@Route("")
public class HomeView extends Div implements BeforeEnterObserver {
    private static final List<Class<? extends Component>> LANDING_PAGES = List.of(DictionaryView.class, NounListView.class);

    private final AccessAnnotationChecker accessChecker;

    public HomeView(AccessAnnotationChecker accessChecker) {
        this.accessChecker = accessChecker;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        LANDING_PAGES.stream()
                .filter(page -> accessChecker.hasAccess(page))
                .findFirst()
                .ifPresentOrElse(page -> event.forwardTo(page), () -> event.rerouteToError(AccessDeniedException.class));
    }
}
