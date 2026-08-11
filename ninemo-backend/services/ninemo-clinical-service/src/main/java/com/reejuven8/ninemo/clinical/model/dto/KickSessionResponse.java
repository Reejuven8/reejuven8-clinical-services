package com.reejuven8.ninemo.clinical.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data @Builder
public class KickSessionResponse {
    private String sessionId;
    private String patientId;
    private int gestationalWeek;
    private Instant sessionStart;
    private Instant sessionEnd;
    private int totalKicks;
    private Integer durationTo10KicksMinutes;
    private boolean isConcerning;
}
