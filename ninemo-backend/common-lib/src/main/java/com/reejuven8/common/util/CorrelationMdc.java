package com.reejuven8.common.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Restores a correlationId (carried in an event/message) into the MDC for the
 * duration of a consumer's processing. Use with try-with-resources:
 *
 * <pre>
 * try (var ignored = CorrelationMdc.restore(event.getCorrelationId())) { ... }
 * </pre>
 *
 * Generates a fresh UUID when the inbound id is null/blank so downstream logs
 * are still correlated with each other.
 */
public final class CorrelationMdc implements AutoCloseable {

    public static final String KEY = "correlationId";

    private CorrelationMdc() {
    }

    public static CorrelationMdc restore(String correlationId) {
        MDC.put(KEY, correlationId == null || correlationId.isBlank()
            ? UUID.randomUUID().toString()
            : correlationId);
        return new CorrelationMdc();
    }

    @Override
    public void close() {
        MDC.remove(KEY);
    }
}
