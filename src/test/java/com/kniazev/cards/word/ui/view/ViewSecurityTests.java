package com.kniazev.cards.word.ui.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.kniazev.cards.word.constant.Roles;
import com.kniazev.cards.word.db.model.User;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

class ViewSecurityTests {

    @Test
    void usersViewIsAdminOnly() {
        RolesAllowed rolesAllowed = UserListView.class.getAnnotation(RolesAllowed.class);

        assertThat(rolesAllowed).isNotNull();
        assertThat(rolesAllowed.value()).containsExactly(Roles.ROLE_ADMIN);
        assertThat(UserListView.class.isAnnotationPresent(PermitAll.class)).isFalse();
    }

    @Test
    void theHomeViewIsOpenToEveryLoggedInAccountAndOnlyToThem() {
        // Both login flows redirect to "/": whatever roles the account has, it must not get a "Could not navigate" page there.
        assertThat(canOpen(HomeView.class, List.of())).isTrue();
        assertThat(canOpen(HomeView.class, List.of("ALL", "PLAYER", "LEARNER"))).isTrue();

        assertThat(new AccessAnnotationChecker().hasAccess(HomeView.class, null, role -> false)).isFalse();
    }

    @Test
    void anAccountCreatedByTheBotCanOpenEveryWordListButNotTheAdminPages() {
        List<String> roles = List.of("ALL", "PLAYER", "LEARNER");

        List<Class<?>> wordLists = List.of(NounListView.class, VerbListView.class, AdjectiveListView.class,
                AdverbListView.class, PhraseListView.class);

        for (Class<?> view : wordLists) {
            assertThat(canOpen(view, roles)).as(view.getSimpleName()).isTrue();
        }

        assertThat(canOpen(UserListView.class, roles)).isFalse();
        assertThat(canOpen(DictionaryView.class, roles)).isFalse();
    }

    private static boolean canOpen(Class<?> view, List<String> roles) {
        Set<String> authorities = User.builder().roles(roles).build().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return new AccessAnnotationChecker().hasAccess(view, () -> "someone", authorities::contains);
    }
}
