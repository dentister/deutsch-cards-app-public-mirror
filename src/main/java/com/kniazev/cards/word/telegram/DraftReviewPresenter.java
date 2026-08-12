package com.kniazev.cards.word.telegram;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kniazev.cards.word.ai.WordDraft;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;

import lombok.RequiredArgsConstructor;

/**
 * Renders an AI-drafted {@link WordDraft} as plain text (deliberately not MarkdownV2 - the
 * JSON body would need heavy escaping that isn't worth the risk of a malformed-Markdown API
 * error) plus the shared Accept/Reject/Recheck keyboard. Stateless, reused by every draft
 * shown to the admin - there's only ever one draft in flight, so callback data carries no
 * token.
 */
@Component
@RequiredArgsConstructor
public class DraftReviewPresenter {

    public static final String CALLBACK_ACCEPT = "WORDDRAFT:ACCEPT";
    public static final String CALLBACK_REJECT = "WORDDRAFT:REJECT";
    public static final String CALLBACK_RECHECK = "WORDDRAFT:RECHECK";

    private final ObjectMapper objectMapper;

    public Rendered render(WordDraft draft, List<String> errors, List<String> warnings) {
        StringBuilder sb = new StringBuilder();
        errors.forEach(e -> sb.append("❌ ").append(e).append('\n'));
        warnings.forEach(w -> sb.append("⚠️ ").append(w).append('\n'));
        if (!errors.isEmpty() || !warnings.isEmpty()) {
            sb.append('\n');
        }
        sb.append(toPrettyJson(draft));

        return new Rendered(sb.toString(), buildKeyboard());
    }

    private InlineKeyboardMarkup buildKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("✅ Accept").callbackData(CALLBACK_ACCEPT).build(),
                        InlineKeyboardButton.builder().text("❌ Reject").callbackData(CALLBACK_REJECT).build(),
                        InlineKeyboardButton.builder().text("🔁 Recheck").callbackData(CALLBACK_RECHECK).build()
                )))
                .build();
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
