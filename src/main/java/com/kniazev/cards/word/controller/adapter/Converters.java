package com.kniazev.cards.word.controller.adapter;

import org.modelmapper.AbstractConverter;
import org.modelmapper.Converter;

import java.time.OffsetDateTime;

public class Converters {
    public static final Converter<String, OffsetDateTime> stringToOffsetDateTimeConverter = new AbstractConverter<String, OffsetDateTime>() {
        @Override
        public OffsetDateTime convert(String source) {
            return OffsetDateTime.parse(source);
        }
    };
}
