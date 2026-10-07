package com.example.payment.chaos;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** In-memory switches for breaking payment-service on purpose. */
@Component
public class ChaosState {

    private volatile long latencyMs = 0;
    private volatile double errorRate = 0.0;
    private final List<byte[]> heldMemory = new ArrayList<>();

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = Math.max(0, latencyMs);
    }

    public void setErrorRate(double errorRate) {
        this.errorRate = Math.min(1.0, Math.max(0.0, errorRate));
    }

    public boolean shouldFail() {
        return ThreadLocalRandom.current().nextDouble() < errorRate;
    }

    /** Allocates and keeps {@code mb} megabytes on the heap. */
    public synchronized void holdMemory(int mb) {
        for (int i = 0; i < mb; i++) {
            byte[] chunk = new byte[1024 * 1024];
            Arrays.fill(chunk, (byte) 1);
            heldMemory.add(chunk);
        }
    }

    public synchronized void reset() {
        latencyMs = 0;
        errorRate = 0.0;
        heldMemory.clear();
    }

    public synchronized Map<String, Object> snapshot() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("latencyMs", latencyMs);
        state.put("errorRate", errorRate);
        state.put("heldMemoryMb", heldMemory.size());
        return state;
    }
}
