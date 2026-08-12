package com.kniazev.cards.word.error.handler.exception;

public class GameNotStartedException extends RuntimeException {

    public GameNotStartedException() {
        super();
    }

    public GameNotStartedException(String message, Throwable cause) {
        super(message, cause);
    }

    public GameNotStartedException(String message) {
        super(message);
    }

    public GameNotStartedException(Throwable cause) {
        super(cause);
    }

}
