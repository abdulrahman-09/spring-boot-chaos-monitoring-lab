package com.example.order.payment;

/** payment-service answered with an error, was unreachable, or returned something unexpected. */
public class PaymentFailedException extends RuntimeException {

    public PaymentFailedException(String message) {
        super(message);
    }

    public PaymentFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
