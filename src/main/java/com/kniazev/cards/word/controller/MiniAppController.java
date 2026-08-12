package com.kniazev.cards.word.controller;

import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.db.services.UserService;
import com.kniazev.cards.word.db.services.WordService;
import com.kniazev.cards.word.services.GameConfiguration;
import com.kniazev.cards.word.services.GameService;
import com.kniazev.cards.word.telegram.TelegramInitDataValidator;
import com.kniazev.cards.word.telegram.TelegramInitDataValidator.TelegramUser;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

/**
 * REST backing the "Specific words" Telegram Mini App.
 *
 * <p>Every endpoint that touches per-user state authenticates via the signed
 * {@code X-Telegram-Init-Data} header (see {@link TelegramInitDataValidator}); the
 * submitted request body is treated as untrusted.
 */
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

    /** Full word catalog for client-side search/filter. Cached-friendly, ~5–6k rows. */
    @GetMapping("/words")
    public List<WordDto> words() {
        return wordService.findAll().stream().map(WordDto::from).toList();
    }

    /** The current user's picked word ids (to pre-check the boxes). */
    @GetMapping("/selection")
    public List<Long> getSelection(@RequestHeader(INIT_DATA_HEADER) String initData) {
        String username = authenticate(initData);
        return gameService.getGameConfiguration(username).getSpecificWordIds();
    }

    /** Persist the picked set and restart the game so "only selected" takes effect. */
    @PostMapping("/selection")
    public Map<String, Object> saveSelection(@RequestHeader(INIT_DATA_HEADER) String initData,
                                             @RequestBody SelectionRequest body) {
        String username = authenticate(initData);

        List<Long> requested = (body == null || body.wordIds() == null) ? List.of() : body.wordIds();
        if (requested.size() > MAX_SELECTION) {
            requested = requested.subList(0, MAX_SELECTION);
        }
        // Trust only ids that actually exist; drop garbage/duplicates.
        List<Long> valid = wordService.findByIds(requested).stream().map(Word::getId).distinct().toList();

        GameConfiguration cfg = gameService.getGameConfiguration(username);
        cfg.setSpecificWordIds(valid.isEmpty() ? null : valid);
        gameService.restartGame(username, cfg);

        return Map.of("saved", valid.size());
    }

    private String authenticate(String initData) {
        TelegramUser user = initDataValidator.validateAndExtractUser(initData);
        String username = user.username();
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Telegram username required");
        }
        if (!userService.userExists(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unknown user");
        }
        return username;
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
