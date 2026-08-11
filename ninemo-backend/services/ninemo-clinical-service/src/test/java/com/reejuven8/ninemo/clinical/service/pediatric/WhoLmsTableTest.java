package com.reejuven8.ninemo.clinical.service.pediatric;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class WhoLmsTableTest {

    // Z-score sanity checks ---------------------------------------------------

    @Test
    void zScore_medianMeasurement_isNearZero() {
        // A child whose weight equals the median should have Z ≈ 0
        WhoLmsTable.LmsPoint lms = WhoLmsTable.interpolate(WhoLmsTable.WAZ_BOYS, 6);
        double z = WhoLmsTable.zScore(lms, lms.m()); // measure exactly the median
        assertEquals(0.0, z, 0.01, "Z-score at median should be ≈ 0");
    }

    @Test
    void zScore_aboveMedian_isPositive() {
        WhoLmsTable.LmsPoint lms = WhoLmsTable.interpolate(WhoLmsTable.WAZ_BOYS, 12);
        double z = WhoLmsTable.zScore(lms, lms.m() * 1.2);
        assertTrue(z > 0, "Above-median measurement should give positive Z");
    }

    @Test
    void zScore_belowMedian_isNegative() {
        WhoLmsTable.LmsPoint lms = WhoLmsTable.interpolate(WhoLmsTable.WAZ_BOYS, 12);
        double z = WhoLmsTable.zScore(lms, lms.m() * 0.8);
        assertTrue(z < 0, "Below-median measurement should give negative Z");
    }

    // Interpolation -----------------------------------------------------------

    @Test
    void interpolate_exactAgeEntry_returnsExactEntry() {
        WhoLmsTable.LmsPoint lms = WhoLmsTable.interpolate(WhoLmsTable.WAZ_BOYS, 6);
        assertEquals(7.9340, lms.m(), 0.001, "Exact age 6m M value mismatch");
    }

    @Test
    void interpolate_betweenEntries_returnsIntermediateM() {
        WhoLmsTable.LmsPoint at3 = WhoLmsTable.interpolate(WhoLmsTable.WAZ_BOYS, 3);
        WhoLmsTable.LmsPoint at6 = WhoLmsTable.interpolate(WhoLmsTable.WAZ_BOYS, 6);
        WhoLmsTable.LmsPoint at4 = WhoLmsTable.interpolate(WhoLmsTable.WAZ_BOYS, 4);
        // M at 4 months should be between 3m and 6m medians
        assertTrue(at4.m() > at3.m() && at4.m() < at6.m(),
            "Interpolated M at 4m should be between 3m and 6m");
    }

    // Percentile --------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
        "0.0,  50",
        "1.96, 97",
        "-1.96, 3"
    })
    void percentile_knownZValues(double z, int expectedPct) {
        int pct = WhoLmsTable.percentile(z);
        assertTrue(Math.abs(pct - expectedPct) <= 2,
            "Percentile for Z=" + z + " expected ≈" + expectedPct + " but got " + pct);
    }

    @Test
    void percentile_clamped1To99() {
        assertTrue(WhoLmsTable.percentile(10.0) <= 99);
        assertTrue(WhoLmsTable.percentile(-10.0) >= 1);
    }

    // Girls tables ------------------------------------------------------------

    @Test
    void girlsWazTable_medianZIsNearZero() {
        WhoLmsTable.LmsPoint lms = WhoLmsTable.interpolate(WhoLmsTable.WAZ_GIRLS, 12);
        double z = WhoLmsTable.zScore(lms, lms.m());
        assertEquals(0.0, z, 0.01);
    }

    // Head circumference table (0–24m only) -----------------------------------

    @Test
    void headCircumference_outsideRange_usesLastEntry() {
        // Age > 24 months: should clamp to last table entry without throwing
        WhoLmsTable.LmsPoint lms = WhoLmsTable.interpolate(WhoLmsTable.HCZ_BOYS, 30);
        assertNotNull(lms);
    }
}
