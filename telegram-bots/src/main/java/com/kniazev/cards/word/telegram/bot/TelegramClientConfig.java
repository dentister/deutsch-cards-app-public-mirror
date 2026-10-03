package com.kniazev.cards.word.telegram.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
public class TelegramClientConfig {

    @Bean
    @ConditionalOnProperty(name = "telegram.bot.enabled", havingValue = "true", matchIfMissing = true)
    public TelegramClient germanCardsBotTelegramClient(@Value("${telegram.bot.token}") String botToken) {
        return new OkHttpTelegramClient(botToken);
    }

    @Bean
    @ConditionalOnProperty(name = "admin.bot.enabled", havingValue = "true", matchIfMissing = true)
    public TelegramClient adminCardsBotTelegramClient(@Value("${admin.bot.token}") String botToken) {
        return new OkHttpTelegramClient(botToken);
    }
}
