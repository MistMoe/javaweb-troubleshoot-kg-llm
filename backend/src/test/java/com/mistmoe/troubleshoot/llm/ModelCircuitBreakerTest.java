package com.mistmoe.troubleshoot.llm;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModelCircuitBreakerTest {
    @Test void opensAfterThreeFailuresAndAllowsOnlyOneRecoveryProbe() {
        Clock clock = mock(Clock.class);
        when(clock.millis()).thenReturn(1000L);
        var circuit = new ModelCircuitBreaker(clock);
        for (int i = 0; i < 3; i++) { assertThat(circuit.acquire()).isTrue(); circuit.failure(); }
        assertThat(circuit.acquire()).isFalse();
        when(clock.millis()).thenReturn(31_001L);
        assertThat(circuit.acquire()).isTrue();
        assertThat(circuit.acquire()).isFalse();
        circuit.success();
        assertThat(circuit.acquire()).isTrue();
    }
    @Test void cancelledProbeDoesNotPermanentlyBlockRecovery() {
        Clock clock = mock(Clock.class);
        when(clock.millis()).thenReturn(1000L);
        var circuit = new ModelCircuitBreaker(clock);
        circuit.failure(); circuit.failure(); circuit.failure();
        when(clock.millis()).thenReturn(32_000L);
        assertThat(circuit.acquire()).isTrue();
        circuit.cancel();
        assertThat(circuit.acquire()).isTrue();
        circuit.failure();
        assertThat(circuit.acquire()).isFalse();
    }
}
