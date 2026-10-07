package com.example.payment.chaos;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/chaos")
@ConditionalOnProperty(name = "chaos.enabled", havingValue = "true")
public class ChaosController {

    private final ChaosState chaos;

    public ChaosController(ChaosState chaos) {
        this.chaos = chaos;
    }

    @GetMapping
    public Map<String, Object> state() {
        return chaos.snapshot();
    }

    /** Every payment takes {@code ms} longer. */
    @PostMapping("/latency")
    public Map<String, Object> latency(@RequestParam long ms) {
        chaos.setLatencyMs(ms);
        return chaos.snapshot();
    }

    /** A fraction (0.0 - 1.0) of payments fail with HTTP 500. */
    @PostMapping("/errors")
    public Map<String, Object> errors(@RequestParam double rate) {
        chaos.setErrorRate(rate);
        return chaos.snapshot();
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
