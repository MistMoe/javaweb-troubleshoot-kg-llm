package com.mistmoe.troubleshoot.llm;

import org.springframework.stereotype.Component;
import java.time.Clock;

@Component
public class ModelCircuitBreaker {
    private final Clock clock;
    private int failures;
    private long openUntil;
    private boolean probing;
    public ModelCircuitBreaker() { this(Clock.systemUTC()); }
    ModelCircuitBreaker(Clock clock) { this.clock = clock; }
    public synchronized boolean acquire() {
        if (openUntil == 0) return true;
        if (clock.millis() < openUntil || probing) return false;
        probing = true;
        return true;
    }
    public synchronized void success() { failures = 0; openUntil = 0; probing = false; }
    public synchronized void cancel() { probing = false; }
    public synchronized void failure() {
        if (++failures >= 3 || probing) openUntil = clock.millis() + 30_000;
        probing = false;
    }
}
