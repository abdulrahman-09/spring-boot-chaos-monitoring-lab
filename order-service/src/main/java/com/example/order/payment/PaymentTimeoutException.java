package com.example.order.payment;

/** payment-service did not answer within the configured connect/read timeout. */
public class PaymentTimeoutException extends RuntimeException {

    public PaymentTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
