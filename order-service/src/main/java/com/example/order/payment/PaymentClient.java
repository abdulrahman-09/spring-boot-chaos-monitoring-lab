package com.example.order.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.UUID;

/**
 * Calls payment-service over HTTP.
 * The RestClient is built from the injected RestClient.Builder (not RestClient.create())
 * so that metrics and tracing can be attached automatically later.
 */
@Component
public class PaymentClient {

    private final RestClient restClient;

    public PaymentClient(RestClient.Builder builder,
                         @Value("${payment.base-url}") String baseUrl,
                         @Value("${payment.connect-timeout-ms}") int connectTimeoutMs,
                         @Value("${payment.read-timeout-ms}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);

        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public PaymentResponse charge(UUID orderId, BigDecimal amount) {
        PaymentResponse response;
        try {
            response = restClient.post()
                    .uri("/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new PaymentRequest(orderId.toString(), amount))
                    .retrieve()
                    .body(PaymentResponse.class);
        } catch (RestClientResponseException e) {
            // payment-service answered, but with an HTTP error status (4xx/5xx)
            throw new PaymentFailedException(
                    "payment-service returned HTTP " + e.getStatusCode().value(), e);
        } catch (RestClientException | UncheckedIOException e) {
            // Everything else: no answer, connection refused, unreadable response...
            // Spring reports a timeout in several wrapper types (ResourceAccessException,
            // plain RestClientException, UncheckedIOException), so look at the root causes.
            if (isTimeout(e)) {
                throw new PaymentTimeoutException("payment-service did not answer in time", e);
            }
            throw new PaymentFailedException("payment-service call failed: " + e.getMessage(), e);
        }

        if (response == null || !"APPROVED".equals(response.status())) {
            throw new PaymentFailedException("payment-service returned an unexpected response");
        }
        return response;
    }

    private static boolean isTimeout(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }
}