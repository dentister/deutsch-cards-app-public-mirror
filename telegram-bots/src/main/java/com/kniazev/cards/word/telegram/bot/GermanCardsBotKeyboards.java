package com.kniazev.cards.word.telegram.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.Locale;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GermanCardsBotKeyboards {

    public static final String CALLBACK_BLITZ_YES = "BLITZ:YES";
    public static final String CALLBACK_BLITZ_NO = "BLITZ:NO";

    public InlineKeyboardMarkup buildYesNoKeyboard(Locale locale) {
        InlineKeyboardButton yes = InlineKeyboards.button(Messages.get("bot.blitz_offer_yes", locale), CALLBACK_BLITZ_YES);
        InlineKeyboardButton no = InlineKeyboards.button(Messages.get("bot.blitz_offer_no", locale), CALLBACK_BLITZ_NO);

        return InlineKeyboards.of(new InlineKeyboardRow(yes, no));
    }

    public InlineKeyboardMarkup buildLevelKeyboard() {
        InlineKeyboardButton a1 = InlineKeyboards.button("A1", "A1");
        InlineKeyboardButton a2 = InlineKeyboards.button("A2", "A2");
        InlineKeyboardButton b1 = InlineKeyboards.button("B1", "B1");
        InlineKeyboardButton b2 = InlineKeyboards.button("B2", "B2");
        InlineKeyboardButton c1 = InlineKeyboards.button("C1", "C1");
        InlineKeyboardButton c2 = InlineKeyboards.button("C2", "C2");

        return InlineKeyboards.of(
                new InlineKeyboardRow(a1, a2),
                new InlineKeyboardRow(b1, b2),
                new InlineKeyboardRow(c1, c2)
        );
    }
}
