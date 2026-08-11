package com.reejuven8.ninemo.clinical.service.timeline;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class UltrasoundCalculationStrategy implements EDDCalculationStrategy {

    private static final int FULL_TERM_DAYS = 280;

    /**
     * A scan date alone cannot yield an EDD — the sonographer's gestational-age reading at
     * that scan is required. Callers must use
     * {@link #calculateEdd(LocalDate, int, int)}. NM-B-167 validates this before dispatch,
     * so this path is only hit by a programming error.
     */
    @Override
    public LocalDate calculateEdd(LocalDate ultrasoundDate) {
        throw new UnsupportedOperationException(
            "Ultrasound EDD calculation requires the gestational age measured at the scan");
    }

    /**
     * EDD = scanDate + (280 days - gestational age at scan).
     * e.g. a scan at 12w3d on 2026-03-01 → 280 - 87 = 193 days later → 2026-09-10.
     */
    public LocalDate calculateEdd(LocalDate ultrasoundDate, int gestationalAgeWeeks, int gestationalAgeDays) {
        if (gestationalAgeWeeks < 0 || gestationalAgeWeeks > 42) {
            throw new IllegalArgumentException("ultrasoundGestationalAgeWeeks must be between 0 and 42");
        }
        if (gestationalAgeDays < 0 || gestationalAgeDays > 6) {
            throw new IllegalArgumentException("ultrasoundGestationalAgeDays must be between 0 and 6");
        }
        int gestationalAgeAtScan = gestationalAgeWeeks * 7 + gestationalAgeDays;
        return ultrasoundDate.plusDays((long) FULL_TERM_DAYS - gestationalAgeAtScan);
    }
}
