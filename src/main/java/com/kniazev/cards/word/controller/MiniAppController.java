package com.kniazev.cards.word.controller;

import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.db.service.UserService;
import com.kniazev.cards.word.db.service.WordService;
import com.kniazev.cards.word.game.GameConfiguration;
import com.kniazev.cards.word.game.GameService;
import com.kniazev.cards.word.i18n.Messages;
import com.kniazev.cards.word.telegram.GermanCardsBot;
import com.kniazev.cards.word.telegram.TelegramInitDataValidator;
import com.kniazev.cards.word.telegram.TelegramInitDataValidator.TelegramUser;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST backing the "Specific words" Telegram Mini App.
 *
 * <p>Every endpoint that touches per-user state authenticates via the signed
 * {@code X-Telegram-Init-Data} header (see {@link TelegramInitDataValidator}); the
 * submitted request body is treated as untrusted.
 */
@Slf4j
@RestController
@RequestMapping("/api/miniapp")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@RequiredArgsConstructor
public class MiniAppController {

    private static final String INIT_DATA_HEADER = "X-Telegram-Init-Data";
    private static final int MAX_SELECTION = 1000;

    private final WordService wordService;
    private final GameService gameService;
    private final UserService userService;
    private final TelegramInitDataValidator initDataValidator;
    private final Optional<GermanCardsBot> germanCardsBot;

    @GetMapping("/words")
    public List<WordDto> words() {
        return wordService.findAll().stream().map(WordDto::from).toList();
    }

    @GetMapping("/selection")
    public List<Long> getSelection(@RequestHeader(INIT_DATA_HEADER) String initData) {
        TelegramUser user = authenticate(initData);

        return gameService.getGameConfiguration(user.username()).getSpecificWordIds();
    }

    @PostMapping("/selection")
    public Map<String, Object> saveSelection(@RequestHeader(INIT_DATA_HEADER) String initData,
                                             @RequestBody SelectionRequest body) {
        TelegramUser user = authenticate(initData);
        String username = user.username();

        List<Long> requested = (body == null || body.wordIds() == null) ? List.of() : body.wordIds();

        if (requested.size() > MAX_SELECTION) {
            requested = requested.subList(0, MAX_SELECTION);
        }

        List<Long> valid = wordService.findByIds(requested).stream().map(Word::getId).distinct().toList();

        Locale locale = Messages.resolveLocale(user.languageCode());

        GameConfiguration cfg = gameService.getGameConfiguration(username);
        cfg.setSpecificWordIds(valid.isEmpty() ? null : valid);
        gameService.restartGame(username, cfg, locale);

        germanCardsBot.ifPresent(bot -> {
            try {
                bot.notifyGameRestarted(username, String.valueOf(user.id()), locale);
            } catch (TelegramApiException e) {
                log.warn("Failed to notify user [{}] after mini app selection save: {}", username, e.getMessage());
            }
        });

        return Map.of("saved", valid.size());
    }

    private TelegramUser authenticate(String initData) {
        TelegramUser user = initDataValidator.validateAndExtractUser(initData);
        String username = user.username();

        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Telegram username required");
        }

        if (!userService.userExists(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unknown user");
        }

        return user;
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> onSecurity(SecurityException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
    }

    public record SelectionRequest(List<Long> wordIds) {}

    public record WordDto(Long id, String de, String ru, String level, List<String> tags, String type) {
        static WordDto from(Word w) {
            return new WordDto(
                    w.getId(),
                    w.getDe(),
                    w.getRu(),
                    w.getLevel() == null ? null : w.getLevel().name(),
                    w.getTags(),
                    w.getWordType() == null ? null : w.getWordType().name());
        }
    }
}
