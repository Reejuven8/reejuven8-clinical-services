package com.reejuven8.common.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.time.Instant;

@Getter
@SuperBuilder
@NoArgsConstructor
public class ConsentGrantedEvent extends BaseEvent {
    private String patientId;
    private String doctorId;
    private String consentId;
    private Instant grantedAt;
    private Instant expiresAt;
}
