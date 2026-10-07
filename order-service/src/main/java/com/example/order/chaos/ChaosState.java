package com.example.order.chaos;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** In-memory switches for breaking order-service on purpose. */
@Component
public class ChaosState {

    private final List<byte[]> heldMemory = new ArrayList<>();
    private final List<Connection> leakedConnections = new ArrayList<>();
    private final ScheduledExecutorService releaser = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "chaos-connection-releaser");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Borrows up to {@code count} connections from the pool and keeps them for {@code seconds}.
     * Returns how many were actually acquired.
     */
    public synchronized int leakConnections(DataSource dataSource, int count, int seconds) {
        List<Connection> batch = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            try {
                batch.add(dataSource.getConnection());
            } catch (SQLException e) {
                break; // pool already exhausted
            }
        }
        leakedConnections.addAll(batch);
        releaser.schedule(() -> release(batch), seconds, TimeUnit.SECONDS);
        return batch.size();
    }

    private synchronized void release(List<Connection> batch) {
        for (Connection connection : batch) {
            try {
                connection.close(); // returns it to the pool
            } catch (SQLException ignored) {
                // nothing useful to do
            }
        }
        leakedConnections.removeAll(batch);
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
        release(new ArrayList<>(leakedConnections));
        heldMemory.clear();
    }

    public synchronized Map<String, Object> snapshot() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("leakedConnections", leakedConnections.size());
        state.put("heldMemoryMb", heldMemory.size());
        return state;
    }
}
