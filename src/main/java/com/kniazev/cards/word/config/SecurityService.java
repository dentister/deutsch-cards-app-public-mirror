package com.kniazev.cards.word.config;

import com.vaadin.flow.spring.security.AuthenticationContext;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@Profile("!no-vaadin")
public class SecurityService {

    private final AuthenticationContext authenticationContext;

    public SecurityService(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
    }

    public UserDetails getAuthenticatedUser() {
        return authenticationContext.getAuthenticatedUser(UserDetails.class).get();
    }

    public String getAuthenticatedUsername() {
        return authenticationContext.getPrincipalName().orElse(null);
    }

    public void logout() {
        authenticationContext.logout();
    }
}
