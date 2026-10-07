package com.example.order.order;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/** The order was saved, but the payment step did not succeed. */
public class OrderPaymentException extends RuntimeException {

    private final UUID orderId;
    private final HttpStatus status;

    public OrderPaymentException(UUID orderId, HttpStatus status, String message) {
        super(message);
        this.orderId = orderId;
        this.status = status;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
