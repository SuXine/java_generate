package com.libra.plugin.exception;

public class GenerateMarkDownException extends RuntimeException {
    public GenerateMarkDownException() {
    }

    public GenerateMarkDownException(String message) {
        super(message);
    }

    public GenerateMarkDownException(String message, Throwable cause) {
        super(message, cause);
    }

    public GenerateMarkDownException(Throwable cause) {
        super(cause);
    }
}
