package com.kniazev.cards.word.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import com.vaadin.flow.spring.security.VaadinWebSecurity;

@Configuration
@Profile("test")
public class TestSecurityConfig extends VaadinWebSecurity {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf(conf -> conf.disable());
        http.authorizeHttpRequests(registry -> registry.anyRequest().permitAll());
    }
}