package com.innowise.paymentservice.exception;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) { super(message); }
}
