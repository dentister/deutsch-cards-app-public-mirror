package com.kniazev.cards.word.game;

import com.kniazev.cards.word.model.dictionary.Word;

/**
 * Lets the game ask for a word to be completed (usage example, English translation) without knowing who does it.
 * The AI integration provides the implementation; the call returns at once and the work is done in the background.
 */
public interface WordEnrichment {

    void ensureEnriched(Word word);
}
