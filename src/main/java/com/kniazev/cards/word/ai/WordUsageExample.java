package com.kniazev.cards.word.ai;

public record WordUsageExample(String sentence, String translationRu, String translationEn,
        String wordEn /* temp solution to translate words lazy to english */ ) {
}
