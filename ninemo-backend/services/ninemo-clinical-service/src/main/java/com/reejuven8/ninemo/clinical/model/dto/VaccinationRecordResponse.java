package com.reejuven8.ninemo.clinical.model.dto;

import com.reejuven8.ninemo.clinical.model.enums.VaccinationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data @Builder
public class VaccinationRecordResponse {
    private UUID id;
    private UUID childId;
    private String vaccineName;
    private String vaccineCode;
    private int doseNumber;
    private LocalDate scheduledDate;
    private LocalDate administeredDate;
    private VaccinationStatus status;
    private boolean overdue;
}
