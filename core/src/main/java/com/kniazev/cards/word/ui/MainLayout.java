package com.kniazev.cards.word.ui;

import com.kniazev.cards.word.ui.view.*;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.lumo.LumoUtility;

import org.vaadin.lineawesome.LineAwesomeIcon;

public class MainLayout extends AppLayout {
    private static final String APPLICATION_TITLE = "Deutsche Wörter v.1.1.0";
    private final VaadinSecurityService securityService;

    private H2 viewTitle;

    public MainLayout(VaadinSecurityService securityService) {
        this.securityService = securityService;

        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle = new H2();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        addToNavbar(true, toggle, viewTitle);
    }

    private void addDrawerContent() {
        H1 appName = new H1(APPLICATION_TITLE);
        appName.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
        Header header = new Header(appName);

        Scroller scroller = new Scroller(createNavigation());

        addToDrawer(header, scroller, createFooter());
    }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();
        
        if (securityService.isAdmin()) {
            nav.addItem(new SideNavItem("Users", UserListView.class, LineAwesomeIcon.USER_FRIENDS_SOLID.create()));
        }

        nav.addItem(new SideNavItem("Dictionary", DictionaryView.class, LineAwesomeIcon.BOOK_SOLID.create()));
        nav.addItem(new SideNavItem("Nouns", NounListView.class, LineAwesomeIcon.SCHOOL_SOLID.create()));
        nav.addItem(new SideNavItem("Verbs", VerbListView.class, LineAwesomeIcon.SCHOOL_SOLID.create()));
        nav.addItem(new SideNavItem("Adjectives", AdjectiveListView.class, LineAwesomeIcon.SCHOOL_SOLID.create()));
        nav.addItem(new SideNavItem("Adverbs", AdverbListView.class, LineAwesomeIcon.SCHOOL_SOLID.create()));
        nav.addItem(new SideNavItem("Phrases", PhraseListView.class, LineAwesomeIcon.SCHOOL_SOLID.create()));

        return nav;
    }

    private Footer createFooter() {
        Footer footer = new Footer();

        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        layout.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM, LumoUtility.Padding.Vertical.SMALL);

        String username = securityService.getAuthenticatedUsername();
        if (username != null) {
            Span userSpan = new Span(username);
            userSpan.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.SECONDARY);
            layout.add(userSpan);
        }

        Button logoutButton = new Button("Logout", e -> securityService.logout());
        logoutButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

        layout.add(logoutButton);
        footer.add(layout);

        return footer;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        
        return title == null ? "" : title.value();
    }
}
