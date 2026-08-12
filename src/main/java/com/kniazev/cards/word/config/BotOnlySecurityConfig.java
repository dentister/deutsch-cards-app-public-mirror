package com.kniazev.cards.word.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Minimal security configuration used when the Vaadin UI is disabled
 * (i.e. the "no-vaadin" profile is active) but an embedded web server is still
 * running. No dependency on VaadinWebSecurity.
 *
 * <p>Gated on a servlet web application: in fully-headless bot-only mode (the
 * "prod" profile sets spring.main.web-application-type=none) there is no HTTP
 * server to secure and no HttpSecurity bean, so this config is skipped.
 */
@Profile("no-vaadin")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableWebSecurity
@Configuration
public class BotOnlySecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(reqs -> reqs
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/api/**").permitAll()
                .requestMatchers("/miniapp/**").permitAll()
                .requestMatchers("/test").permitAll()
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
