package com.kniazev.cards.word.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kniazev.cards.word.db.model.User;
import com.kniazev.cards.word.db.service.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

class CommonConfigurationTests {

    private final CommonConfiguration configuration = new CommonConfiguration();
    private final PasswordEncoder encoder = configuration.passwordEncoder();

    @Test
    void blankPasswordNeverMatchesEvenTheHashOfAnEmptyString() {
        String hashOfEmpty = encoder.encode("");

        // This is why the guard exists: a stock BCrypt encoder accepts "" against BCrypt("").
        assertThat(new BCryptPasswordEncoder().matches("", hashOfEmpty)).isTrue();

        assertThat(encoder.matches("", hashOfEmpty)).isFalse();
        assertThat(encoder.matches("   ", hashOfEmpty)).isFalse();
        assertThat(encoder.matches(null, hashOfEmpty)).isFalse();
    }

    @Test
    void regularPasswordsStillMatch() {
        String hash = encoder.encode("secret");

        assertThat(encoder.matches("secret", hash)).isTrue();
        assertThat(encoder.matches("wrong", hash)).isFalse();
    }

    @Test
    void formLoginRejectsABlankPasswordForABotCreatedAccount() {
        UserService userService = mock(UserService.class);
        User botUser = User.builder()
                .username("alice")
                .password(encoder.encode(""))
                .roles(List.of("PLAYER"))
                .enabled(true)
                .build();

        when(userService.loadUserByUsername("alice")).thenReturn(botUser);

        DaoAuthenticationProvider provider = configuration.authenticationProvider(userService);

        assertThatThrownBy(() -> provider.authenticate(new UsernamePasswordAuthenticationToken("alice", "")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
