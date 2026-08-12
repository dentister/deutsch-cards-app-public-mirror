package com.kniazev.cards.word.error.handler.exception;

public class WordCardException extends RuntimeException {
    private static final long serialVersionUID = 8126990654646161357L;

    public WordCardException() {
    }

    public WordCardException(String message) {
        super(message);
    }

    public WordCardException(Throwable cause) {
        super(cause);
    }

    public WordCardException(String message, Throwable cause) {
        super(message, cause);
    }

    public WordCardException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
