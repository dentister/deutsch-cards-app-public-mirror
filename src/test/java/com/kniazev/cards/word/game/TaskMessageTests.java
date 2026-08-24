package com.kniazev.cards.word.game;

import static org.assertj.core.api.Assertions.assertThat;

import com.kniazev.cards.word.db.model.word.Adjective;
import com.kniazev.cards.word.db.model.word.Word.WordLevel;

import org.junit.jupiter.api.Test;

import java.util.Locale;

class TaskMessageTests {

    @Test
    void getTaskTextRendersInTheGivenLocale() {
        Adjective word = Adjective.builder().de("schnell").ru("быстрый").level(WordLevel.A1).build();
        TaskMessage taskMessage = new TaskMessage(TaskEnum.ADJECTIVE, word);

        assertThat(taskMessage.getTaskText(Locale.forLanguageTag("ru"))).isEqualTo("Далее прилагательное: *быстрый* (A1)");
        assertThat(taskMessage.getTaskText(Locale.ROOT)).isEqualTo("Next adjective: *быстрый* (A1)");
    }

    @Test
    void getTaskTextFallsBackToDefaultLocaleWhenNoneGiven() {
        Adjective word = Adjective.builder().de("schnell").ru("быстрый").level(WordLevel.A1).build();
        TaskMessage taskMessage = new TaskMessage(TaskEnum.ADJECTIVE, word);

        assertThat(taskMessage.getTaskText(null)).isEqualTo("Next adjective: *быстрый* (A1)");
    }

    @Test
    void getTaskTextSubstitutesTheLevelPlaceholder() {
        Adjective word = Adjective.builder().de("laut").ru("громкий").level(WordLevel.C1).build();
        TaskMessage taskMessage = new TaskMessage(TaskEnum.ADJECTIVE, word);

        assertThat(taskMessage.getTaskText(Locale.ROOT)).contains("C1");
    }
}
