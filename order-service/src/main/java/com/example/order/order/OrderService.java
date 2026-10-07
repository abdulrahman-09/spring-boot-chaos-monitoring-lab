package com.example.order.order;

import com.example.order.payment.PaymentClient;
import com.example.order.payment.PaymentFailedException;
import com.example.order.payment.PaymentTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Deliberately NOT @Transactional: each repository call runs in its own short
 * transaction, so no DB connection is held while we wait for payment-service.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final PaymentClient paymentClient;

    public OrderService(OrderRepository orders, PaymentClient paymentClient) {
        this.orders = orders;
        this.paymentClient = paymentClient;
    }

    public Order placeOrder(CreateOrderRequest request) {
        Order order = orders.save(new Order(request.customerId(), request.amount()));
        log.info("Order created orderId={} customerId={} amount={}",
                order.getId(), order.getCustomerId(), order.getAmount());

        try {
            paymentClient.charge(order.getId(), order.getAmount());
        } catch (PaymentTimeoutException e) {
            // In a real system this state is "unknown": the payment may still have gone through.
            updateStatus(order, OrderStatus.PAYMENT_FAILED);
            log.error("Payment timed out orderId={} reason={}", order.getId(), e.getMessage());
            throw new OrderPaymentException(order.getId(), HttpStatus.GATEWAY_TIMEOUT, "Payment timed out");
        } catch (PaymentFailedException e) {
            updateStatus(order, OrderStatus.PAYMENT_FAILED);
            log.error("Payment failed orderId={} reason={}", order.getId(), e.getMessage());
            throw new OrderPaymentException(order.getId(), HttpStatus.BAD_GATEWAY, "Payment failed");
        }

        order = updateStatus(order, OrderStatus.PAID);
        log.info("Order completed orderId={} status={}", order.getId(), order.getStatus());
        return order;
    }

    public Order get(UUID id) {
        return orders.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    private Order updateStatus(Order order, OrderStatus status) {
        order.setStatus(status);
        return orders.save(order);
    }
}
