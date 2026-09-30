package com.kniazev.cards.word.ui.view;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import org.springframework.beans.factory.annotation.Value;

import com.kniazev.cards.word.constant.UIRoute;

import java.util.List;
import java.util.Map;

@Route(UIRoute.LOGIN)
@PageTitle("Login | Deutsche Wörter")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm login = new LoginForm();

    public LoginView(@Value("${telegram.bot.username:}") String telegramBotUsername) {
        addClassName("login-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        login.setAction("login");

        add(new H1("Deutsche Wörter"), login);

        if (!telegramBotUsername.isBlank()) {
            add(buildTelegramLoginButton(telegramBotUsername));
        }
    }

    private Div buildTelegramLoginButton(String botUsername) {
        Div container = new Div();

        Element widget = new Element("script");
        widget.setAttribute("src", "https://telegram.org/js/telegram-widget.js?22");
        widget.setAttribute("data-telegram-login", botUsername);
        widget.setAttribute("data-size", "large");
        widget.setAttribute("data-auth-url", "/telegram-login/callback");

        container.getElement().appendChild(widget);

        return container;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        Map<String, List<String>> params = beforeEnterEvent.getLocation().getQueryParameters().getParameters();

        if (params.containsKey("error")) {
            if (params.getOrDefault("error", List.of()).contains("telegram_unregistered")) {
                LoginI18n i18n = LoginI18n.createDefault();
                i18n.getErrorMessage().setTitle("Telegram account not linked");
                i18n.getErrorMessage().setMessage("Message the bot and send /start first, then try again.");
                login.setI18n(i18n);
            }

            login.setError(true);
        }
    }
}
