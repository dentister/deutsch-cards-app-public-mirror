package com.kniazev.cards.word.telegram.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kniazev.cards.word.model.user.User;
import com.kniazev.cards.word.service.UserService;
import com.kniazev.cards.word.telegram.web.TelegramLoginWidgetValidator.TelegramLoginUser;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class TelegramLoginControllerTests {

    private static final String SECURITY_CONTEXT_KEY = "SPRING_SECURITY_CONTEXT";
    private static final String SAVED_REQUEST_KEY = "SPRING_SECURITY_SAVED_REQUEST";

    @Mock
    private TelegramLoginWidgetValidator validator;
    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TelegramLoginController(validator, userService)).build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aBoundAccountIsSignedInAndTheContextIsStoredInTheSession() throws Exception {
        User user = account("alice", 42L);

        when(validator.validateAndExtractUser(anyMap())).thenReturn(new TelegramLoginUser(42L, "alice", "Alice", null, null));
        when(userService.findOrBindByTelegramId(42L, "alice")).thenReturn(Optional.of(user));

        MvcResult result = mockMvc.perform(get("/telegram-login/callback").param("id", "42").param("hash", "signed"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"))
                .andReturn();

        // Without an explicit save the next request would be anonymous again.
        SecurityContext context = (SecurityContext) result.getRequest().getSession().getAttribute(SECURITY_CONTEXT_KEY);

        assertThat(context).isNotNull();
        assertThat(context.getAuthentication().isAuthenticated()).isTrue();
        assertThat(context.getAuthentication().getPrincipal()).isSameAs(user);
        assertThat(context.getAuthentication().getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_PLAYER");
    }

    @Test
    void anAccountWithoutATelegramUsernameCanSignInById() throws Exception {
        User user = account("tg-42", 42L);

        when(validator.validateAndExtractUser(anyMap())).thenReturn(new TelegramLoginUser(42L, null, "Alice", null, null));
        when(userService.findOrBindByTelegramId(42L, null)).thenReturn(Optional.of(user));

        MvcResult result = mockMvc.perform(get("/telegram-login/callback").param("id", "42").param("hash", "signed"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"))
                .andReturn();

        assertThat(result.getRequest().getSession().getAttribute(SECURITY_CONTEXT_KEY)).isNotNull();
    }

    @Test
    void theRequestedPageIsRestoredAfterSigningIn() throws Exception {
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest original = new MockHttpServletRequest("GET", "/dictionary");
        original.setSession(session);
        new HttpSessionRequestCache().saveRequest(original, new MockHttpServletResponse());

        when(validator.validateAndExtractUser(anyMap())).thenReturn(new TelegramLoginUser(42L, "alice", "Alice", null, null));
        when(userService.findOrBindByTelegramId(42L, "alice")).thenReturn(Optional.of(account("alice", 42L)));

        MvcResult result = mockMvc.perform(get("/telegram-login/callback").session(session).param("id", "42"))
                .andExpect(status().isFound())
                .andReturn();

        assertThat(result.getResponse().getRedirectedUrl()).startsWith("http://localhost/dictionary");
        assertThat(session.getAttribute(SAVED_REQUEST_KEY)).isNull();
    }

    @Test
    void anUnknownTelegramAccountIsSentBackToLoginWithoutASession() throws Exception {
        when(validator.validateAndExtractUser(anyMap())).thenReturn(new TelegramLoginUser(42L, "alice", "Alice", null, null));
        when(userService.findOrBindByTelegramId(42L, "alice")).thenReturn(Optional.empty());

        MvcResult result = mockMvc.perform(get("/telegram-login/callback").param("id", "42"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?error=telegram_unregistered"))
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    void aNotFoundErrorFromTheServiceIsTreatedAsAnUnregisteredAccount() throws Exception {
        when(validator.validateAndExtractUser(anyMap())).thenReturn(new TelegramLoginUser(42L, "alice", "Alice", null, null));
        when(userService.findOrBindByTelegramId(42L, "alice")).thenThrow(new EntityNotFoundException("gone"));

        mockMvc.perform(get("/telegram-login/callback").param("id", "42"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?error=telegram_unregistered"));
    }

    @Test
    void aBadSignatureIsSentBackToLoginWithoutASession() throws Exception {
        when(validator.validateAndExtractUser(anyMap())).thenThrow(new SecurityException("Callback hash mismatch"));

        MvcResult result = mockMvc.perform(get("/telegram-login/callback").param("id", "42").param("hash", "forged"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?error=telegram_invalid"))
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
    }

    private static User account(String handle, long telegramId) {
        return User.builder()
                .username(handle)
                .telegramId(telegramId)
                .roles(List.of("PLAYER"))
                .enabled(true)
                .build();
    }
}
