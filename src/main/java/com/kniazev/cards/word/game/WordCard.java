package com.kniazev.cards.word.game;

import com.kniazev.cards.word.db.model.word.Noun;
import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.ui.configuration.ChatMessage.RightAnswer.Color;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.UUID;
import java.util.function.Supplier;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@Getter
public class WordCard {

    private final UUID cardId;
    private final Word word;
    private final Supplier<String> rightAnswer;
    private final Color color;
    private final TaskMessage taskMsg;
    private String userAnswer;
    private AnswerMessage answerMsg;
    private boolean newWord;

    public WordCard(Word word, TaskEnum taskId, Supplier<String> rightAnswer) {
        this.cardId = UUID.randomUUID();
        this.taskMsg = new TaskMessage(taskId, word);
        this.rightAnswer = rightAnswer;
        this.word = word;
        this.color = getCardColor(word);
    }

    public Long getWordId() {
        return word.getId();
    }

    public String getTaskText(TaskTextFormat taskTextFormat) {
        return taskTextFormat == TaskTextFormat.TELEGRAM ? taskMsg.getTaskTextTg() : taskMsg.getTaskTextWeb();
    }

    public void setUserAnswer(String userAnswer) {
        if (this.userAnswer != null) {
            throw new IllegalStateException("User answer was already accepted");
        } else {
            this.userAnswer = userAnswer;
        }

        answerMsg = new AnswerMessage(word, isRightAnswered(), color, userAnswer, rightAnswer.get());
    }

    public boolean isRightAnswered() {
        if (this.userAnswer == null) {
            throw new IllegalStateException("User did not give answer");
        }

        String[] rightAnswerTokens = rightAnswer.get().split("[^a-zA-Z]+");
        String[] userAnswerTokens = userAnswer.split("[^a-zA-Z]+");

        boolean equals = Arrays.equals(rightAnswerTokens, userAnswerTokens,
                (s1, s2) -> StringUtils.equalsIgnoreCase(s1, s2) ? 0 : 1);

        if (!equals) {
            log.info("User mistaken [rightAnswer={}, wrongAnswer={}]", rightAnswer.get(), userAnswer);
        }

        return equals;
    }

    private static Color getCardColor(Word w) {
        if (w instanceof Noun n) {
            return switch (n.getGender()) {
                case F:
                    yield Color.RED;
                case M:
                    yield Color.BLUE;
                case N:
                    yield Color.GREEN;
                case PL:
                    yield Color.RED;
            };
        } else {
            return Color.DEFAULT;
        }
    }
}
