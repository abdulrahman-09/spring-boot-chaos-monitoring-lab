package com.example.order.config;

import com.example.order.order.OrderNotFoundException;
import com.example.order.order.OrderPaymentException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(OrderNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "order_not_found", "message", e.getMessage()));
    }

    /** The order exists (status PAYMENT_FAILED) but payment did not go through: 502 or 504. */
    @ExceptionHandler(OrderPaymentException.class)
    public ResponseEntity<Map<String, Object>> paymentProblem(OrderPaymentException e) {
        return ResponseEntity.status(e.getStatus())
                .body(Map.of(
                        "error", "payment_problem",
                        "message", e.getMessage(),
                        "orderId", e.getOrderId().toString()));
    }
}
