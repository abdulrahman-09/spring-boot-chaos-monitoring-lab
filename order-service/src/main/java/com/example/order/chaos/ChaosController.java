package com.example.order.chaos;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/chaos")
@ConditionalOnProperty(name = "chaos.enabled", havingValue = "true")
public class ChaosController {

    private final ChaosState chaos;
    private final DataSource dataSource;

    public ChaosController(ChaosState chaos, DataSource dataSource) {
        this.chaos = chaos;
        this.dataSource = dataSource;
    }

    @GetMapping
    public Map<String, Object> state() {
        return chaos.snapshot();
    }

    /**
     * Holds DB connections so the pool (max 10) runs dry.
     * Use connections=10 to starve it completely, 8-9 for partial saturation.
     */
    @PostMapping("/db-leak")
    public Map<String, Object> dbLeak(@RequestParam(defaultValue = "10") int connections,
                                      @RequestParam(defaultValue = "60") int seconds) {
        int acquired = chaos.leakConnections(dataSource, connections, seconds);
        Map<String, Object> result = new LinkedHashMap<>(chaos.snapshot());
        result.put("acquiredNow", acquired);
        result.put("releasedAfterSeconds", seconds);
        return result;
    }

    /** Allocates and holds memory so heap usage and GC activity grow. */
    @PostMapping("/memory")
    public Map<String, Object> memory(@RequestParam(defaultValue = "100") int mb) {
        chaos.holdMemory(mb);
        return chaos.snapshot();
    }

    @PostMapping("/reset")
    public Map<String, Object> reset() {
        chaos.reset();
        return chaos.snapshot();
    }
}
