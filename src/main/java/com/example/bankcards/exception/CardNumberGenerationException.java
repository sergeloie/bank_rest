package com.example.bankcards.exception;

public class CardNumberGenerationException extends RuntimeException{
    public CardNumberGenerationException(String message) {
        super(message);
    }

    public CardNumberGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
