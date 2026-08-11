package com.reejuven8.ninemo.clinical.model.dto;

import com.reejuven8.ninemo.clinical.model.entity.ChildProfile;
import com.reejuven8.ninemo.clinical.model.enums.BiologicalSex;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * NM-B-169. ageInMonths is server-computed: growth Z-scores and WHO milestone check-ins are
 * keyed on it, and the client must not derive clinical values (thin-client rule).
 */
@Value
@Builder
public class ChildProfileResponse {
    UUID id;
    UUID pregnancyProfileId;
    UUID parentUserId;
    String childName;
    LocalDate dateOfBirth;
    int ageInMonths;
    BigDecimal birthWeightKg;
    BigDecimal birthHeightCm;
    BigDecimal headCircumferenceCm;
    BiologicalSex biologicalSex;
    String bloodGroup;
    boolean active;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;

    public static ChildProfileResponse from(ChildProfile c) {
        return ChildProfileResponse.builder()
            .id(c.getId())
            .pregnancyProfileId(c.getPregnancyProfileId())
            .parentUserId(c.getParentUserId())
            .childName(c.getChildName())
            .dateOfBirth(c.getDateOfBirth())
            .ageInMonths((int) ChronoUnit.MONTHS.between(c.getDateOfBirth(), LocalDate.now()))
            .birthWeightKg(c.getBirthWeightKg())
            .birthHeightCm(c.getBirthHeightCm())
            .headCircumferenceCm(c.getHeadCircumferenceCm())
            .biologicalSex(c.getBiologicalSex())
            .bloodGroup(c.getBloodGroup())
            .active(c.isActive())
            .createdAt(c.getCreatedAt())
            .updatedAt(c.getUpdatedAt())
            .build();
    }
}
