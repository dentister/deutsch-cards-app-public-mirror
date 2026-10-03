package com.kniazev.cards.word.telegram.bot;

import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class TelegramMessenger {

    private final TelegramClient telegramClient;

    public void send(String chatId, String text) throws TelegramApiException {
        telegramClient.execute(SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build());
    }

    public void send(String chatId, String text, InlineKeyboardMarkup keyboard) throws TelegramApiException {
        telegramClient.execute(SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboard)
                .build());
    }

    public void sendMarkdown(String chatId, String text) throws TelegramApiException {
        telegramClient.execute(SendMessage.builder()
                .chatId(chatId)
                .parseMode("MarkdownV2")
                .text(escaped(text))
                .build());
    }

    public void sendMarkdown(String chatId, Integer replyToMessageId, String text) throws TelegramApiException {
        telegramClient.execute(SendMessage.builder()
                .chatId(chatId)
                .replyToMessageId(replyToMessageId)
                .parseMode("MarkdownV2")
                .text(escaped(text))
                .build());
    }

    public void removeKeyboard(String chatId, Integer messageId) throws TelegramApiException {
        telegramClient.execute(EditMessageReplyMarkup.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build());
    }

    public void answerCallback(String callbackQueryId) throws TelegramApiException {
        telegramClient.execute(AnswerCallbackQuery.builder().callbackQueryId(callbackQueryId).build());
    }

    public void registerCommands(List<BotCommand> commands) throws TelegramApiException {
        telegramClient.execute(new SetMyCommands(commands, new BotCommandScopeDefault(), null));
    }

    private static String escaped(String text) {
        return text
//                .replace("\\", "\\\\")
                .replace("_", "\\_")
//                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
//                .replace("~", "\\~")
//                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace(".", "\\.")
                .replace("!", "\\!");
    }
}
