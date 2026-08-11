package com.reejuven8.common.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.util.List;

@Getter
@SuperBuilder
@NoArgsConstructor
public class DocumentParsedEvent extends BaseEvent {
    private String patientId;
    private String sourceDocumentId;
    private List<Object> observations;
}
