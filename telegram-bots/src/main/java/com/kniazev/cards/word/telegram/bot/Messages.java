package com.kniazev.cards.word.telegram.bot;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Messages {
    private static final String BASE_NAME = "i18n.messages";

    public static String get(String code, Locale locale) {
        return ResourceBundle.getBundle(BASE_NAME, locale != null ? locale : Locale.ROOT).getString(code);
    }

    public static String get(String code, Locale locale, Map<String, String> params) {
        return StringSubstitutor.replace(get(code, locale), params, "{", "}");
    }

    public static Locale resolveLocale(String languageCode) {
        if (StringUtils.isBlank(languageCode)) {
            return Locale.ROOT;
        }

        String language = Locale.forLanguageTag(languageCode).getLanguage();

        return Locale.forLanguageTag("uk".equals(language) ? "ru" : language);
    }
}
