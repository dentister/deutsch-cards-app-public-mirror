package com.kniazev.cards.word;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;

@SpringBootApplication
@Theme("myapp")
public class WordCardsApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(WordCardsApplication.class, args);
    }

}
