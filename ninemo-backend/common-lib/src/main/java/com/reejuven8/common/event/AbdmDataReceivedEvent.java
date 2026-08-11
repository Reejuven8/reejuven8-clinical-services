package com.reejuven8.common.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
public class AbdmDataReceivedEvent extends BaseEvent {
    private String patientId;
    private Object fhirBundle;
    private String source;
}
