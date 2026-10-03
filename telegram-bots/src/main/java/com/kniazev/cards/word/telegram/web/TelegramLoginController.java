package com.kniazev.cards.word.telegram.web;

import com.kniazev.cards.word.model.user.User;
import com.kniazev.cards.word.service.UserService;
import com.kniazev.cards.word.telegram.web.TelegramLoginWidgetValidator.TelegramLoginUser;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.util.Map;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles the redirect Telegram's "Login Widget" sends the browser to after the user
 * approves login on telegram.org. Only ever logs in an ALREADY-registered user (one who has
 * messaged {@link com.kniazev.cards.word.telegram.bot.GermanCardsBot} at least once) - it never
 * provisions an account itself.
 */
@Slf4j
@Controller
@RequestMapping("/telegram-login")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@RequiredArgsConstructor
public class TelegramLoginController {

    private final TelegramLoginWidgetValidator validator;
    private final UserService userService;

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam Map<String, String> params,
                                          HttpServletRequest request, HttpServletResponse response) {
        TelegramLoginUser tgUser = validator.validateAndExtractUser(params);

        User user = userService.findOrBindByTelegramId(tgUser.id(), tgUser.username())
                .orElseThrow(() -> new EntityNotFoundException("No account for Telegram id " + tgUser.id()));

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                user, null, user.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);

        RequestCache requestCache = new HttpSessionRequestCache();
        SavedRequest saved = requestCache.getRequest(request, response);
        if (saved != null) {
            requestCache.removeRequest(request, response);
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(saved != null ? saved.getRedirectUrl() : "/"))
                .build();
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Void> onInvalidSignature(SecurityException e) {
        log.warn("Telegram login rejected: {}", e.getMessage());

        return redirectToLogin("telegram_invalid");
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Void> onUnknownUser() {
        return redirectToLogin("telegram_unregistered");
    }

    private ResponseEntity<Void> redirectToLogin(String errorCode) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/login?error=" + errorCode)).build();
    }
}
