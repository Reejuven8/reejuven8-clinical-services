package com.reejuven8.common.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.time.Instant;
import java.util.UUID;

@Getter
@SuperBuilder
@NoArgsConstructor
public abstract class BaseEvent {
    @lombok.Builder.Default
    private final String eventId = UUID.randomUUID().toString();
    @lombok.Builder.Default
    private final Instant timestamp = Instant.now();
    private String correlationId;
    private String source;
}
