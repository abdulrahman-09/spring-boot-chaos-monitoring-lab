package com.example.order.payment;

import java.math.BigDecimal;

public record PaymentRequest(String orderId, BigDecimal amount) {
}
