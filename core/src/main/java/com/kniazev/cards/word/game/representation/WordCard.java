package com.kniazev.cards.word.game.representation;

import com.kniazev.cards.word.model.dictionary.Word;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@Getter
public class WordCard {

    private static final Pattern NON_LETTER_PATTERN = Pattern.compile("[^\\p{L}]+");

    private final UUID cardId;
    private final Word word;
    private final TaskEnum taskId;
    private final Supplier<String> rightAnswer;
    private String userAnswer;
    private boolean newWord;
    private Locale locale;

    private Boolean isCorrect;

    public WordCard(Word word, TaskEnum taskId, Supplier<String> rightAnswer) {
        this.cardId = UUID.randomUUID();
        this.word = word;
        this.taskId = taskId;
        this.rightAnswer = rightAnswer;
    }

    public Long getWordId() {
        return word.getId();
    }

    public void setUserAnswer(String userAnswer) {
        if (this.userAnswer != null) {
            throw new IllegalStateException("User answer was already accepted");
        }
        this.userAnswer = userAnswer;

        this.isCorrect = checkAnswer(userAnswer, rightAnswer.get());
    }

    public boolean isRightAnswered() {
        if (this.isCorrect == null) {
            throw new IllegalStateException("User did not give answer");
        }
        return this.isCorrect;
    }

    private boolean checkAnswer(String userAns, String rightAns) {
        String[] rightAnswerTokens = NON_LETTER_PATTERN.split(rightAns);
        String[] userAnswerTokens = NON_LETTER_PATTERN.split(userAns);

        boolean match = Arrays.equals(rightAnswerTokens, userAnswerTokens, String.CASE_INSENSITIVE_ORDER);

        if (!match) {
            log.info("User mistaken [rightAnswer={}, wrongAnswer={}]", rightAns, userAns);
        }

        return match;
    }
}
