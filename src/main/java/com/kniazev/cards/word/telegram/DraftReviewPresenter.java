package com.kniazev.cards.word.telegram;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kniazev.cards.word.ai.WordDraft;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;

import lombok.RequiredArgsConstructor;

import static com.kniazev.cards.word.constant.Emoji.*;

@Component
@RequiredArgsConstructor
public class DraftReviewPresenter {

    public static final String CALLBACK_ACCEPT = "WORDDRAFT:ACCEPT";
    public static final String CALLBACK_REJECT = "WORDDRAFT:REJECT";
    public static final String CALLBACK_RECHECK = "WORDDRAFT:RECHECK";

    private final ObjectMapper objectMapper;

    public Rendered render(WordDraft draft, List<String> errors, List<String> warnings) {
        StringBuilder sb = new StringBuilder();

        errors.forEach(e -> sb.append(CROSS_ICON + " ").append(e).append('\n'));
        warnings.forEach(w -> sb.append(WARNING_ICON + "️ ").append(w).append('\n'));

        if (!errors.isEmpty() || !warnings.isEmpty()) {
            sb.append('\n');
        }

        sb.append(toPrettyJson(draft));

        return new Rendered(sb.toString(), buildKeyboard());
    }

    private InlineKeyboardMarkup buildKeyboard() {
        return InlineKeyboards.of(new InlineKeyboardRow(
                InlineKeyboards.button(OK_ICON + " Accept", CALLBACK_ACCEPT),
                InlineKeyboards.button(CROSS_ICON + " Reject", CALLBACK_REJECT),
                InlineKeyboards.button(SWAP_ICON + " Recheck", CALLBACK_RECHECK)
        ));
    }

    private String toPrettyJson(WordDraft draft) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(draft);
        } catch (JsonProcessingException e) {
            return draft.toString();
        }
    }

    public record Rendered(String text, InlineKeyboardMarkup keyboard) {
    }
}
