package com.kniazev.cards.word.ui.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.kniazev.cards.word.model.user.User;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Only what the browser tests in the app module cannot reach: they sign in as a learner or an admin, never as an
 * account without the LEARNER role.
 */
class HomeViewTests {

    private static final String[] ROLES_WITHOUT_LEARNER = {"ALL", "PLAYER"};

    @Test
    void anAccountThatMayOpenNoLandingPageGetsAnAccessDeniedError() {
        BeforeEnterEvent event = mock(BeforeEnterEvent.class);

        new HomeView(accessCheckerFor(ROLES_WITHOUT_LEARNER)).beforeEnter(event);

        verify(event).rerouteToError(AccessDeniedException.class);
        verifyNoMoreInteractions(event);
    }

    @Test
    void theDictionaryIsClosedToAnAccountWithoutTheLearnerOrAdminRole() {
        assertThat(accessCheckerFor(ROLES_WITHOUT_LEARNER).hasAccess(DictionaryView.class)).isFalse();
    }

    private static AccessAnnotationChecker accessCheckerFor(String... roles) {
        User user = User.builder().username("someone").roles(List.of(roles)).build();
        Set<String> authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // The same annotation rules as in production, with the role check of a request for this account.
        return new AccessAnnotationChecker() {
            @Override
            public boolean hasAccess(Class<?> cls) {
                return hasAccess(cls, user::getUsername, authorities::contains);
            }
        };
    }
}
