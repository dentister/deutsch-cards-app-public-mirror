package com.kniazev.cards.word.error.exception;


public class InconsistentDataException extends RuntimeException {

    public InconsistentDataException() {
        super();
    }

    public InconsistentDataException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public InconsistentDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public InconsistentDataException(String message) {
        super(message);
    }

    public InconsistentDataException(Throwable cause) {
        super(cause);
    }

}
