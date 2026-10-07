package com.example.payment.payment;

import com.example.payment.chaos.ChaosState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final ChaosState chaos;

    public PaymentService(ChaosState chaos) {
        this.chaos = chaos;
    }

    public PaymentResponse process(PaymentRequest request) {
        log.info("Payment started orderId={} amount={}", request.orderId(), request.amount());

        simulateWork();

        if (chaos.shouldFail()) {
            log.error("Payment failed orderId={} reason=injected_failure", request.orderId());
            throw new PaymentFailedException("Payment gateway error");
        }

        String paymentId = UUID.randomUUID().toString();
        log.info("Payment approved orderId={} paymentId={}", request.orderId(), paymentId);
        return new PaymentResponse(paymentId, "APPROVED");
    }

    /** Pretends to talk to a card network: 50-300 ms, plus any injected latency. */
    private void simulateWork() {
        long delayMs = ThreadLocalRandom.current().nextLong(50, 301) + chaos.getLatencyMs();
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
