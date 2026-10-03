package com.kniazev.cards.word.ui;

import com.kniazev.cards.word.common.Roles;
import com.vaadin.flow.spring.security.AuthenticationContext;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
public class VaadinSecurityService {

    private final AuthenticationContext authenticationContext;

    public VaadinSecurityService(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
    }

    public Optional<UserDetails> getAuthenticatedUser() {
        return authenticationContext.getAuthenticatedUser(UserDetails.class);
    }

    public String getAuthenticatedUsername() {
        return authenticationContext.getPrincipalName().orElse(null);
    }

    public void logout() {
        authenticationContext.logout();
    }
    
    public boolean isAdmin() {
        return getAuthenticatedUser()
                .map(UserDetails::getAuthorities)
                .stream()
                .flatMap(Collection::stream)
                .anyMatch(auth -> auth.getAuthority().equals(Roles.ROLE_ADMIN));
    }
}
