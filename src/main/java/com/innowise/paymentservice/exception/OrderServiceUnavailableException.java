package com.innowise.paymentservice.exception;

public class OrderServiceUnavailableException extends RuntimeException {
    public OrderServiceUnavailableException(String message) { super(message); }
}
