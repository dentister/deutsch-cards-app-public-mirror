package com.kniazev.cards.word.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import com.kniazev.cards.word.ui.view.LoginView;
import com.vaadin.flow.spring.security.VaadinWebSecurity;

@Profile("!test & !no-vaadin")
@EnableWebSecurity
@Configuration
public class SecurityConfig extends VaadinWebSecurity {
    
    @Autowired
    private ProviderManager authenticationManager;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(reqs -> {
                reqs.requestMatchers("/swagger-ui.html").permitAll()
                    .requestMatchers("/api/**").permitAll()
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .requestMatchers("/v3/api-docs/swagger-config").permitAll()
                    .requestMatchers("/test").permitAll()
                    .requestMatchers("/miniapp", "/miniapp/**").permitAll();
            })
            .authenticationManager(authenticationManager);
        
        setLoginView(http, LoginView.class);
        
        super.configure(http);
        
        http.csrf(csrf -> csrf.disable());
    }
}
