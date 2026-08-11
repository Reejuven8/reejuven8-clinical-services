package com.reejuven8.ninemo.clinical.service.timeline;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TimelineServiceTest {

    // Gestational week from EDD -----------------------------------------------

    @Test
    void gestationalWeek_whenEddIsToday_returns41() {
        // EDD=today → LMP = today-280d → daysSinceLMP=280 → floor(280/7)+1 = 41
        // (ordinal "which week", not "completed weeks")
        LocalDate edd = LocalDate.now();
        assertEquals(41, TimelineService.computeGestationalWeek(edd));
    }

    @Test
    void gestationalWeek_when10WeeksRemaining_returns31() {
        // EDD = today+70d → daysSinceLMP = 280-70 = 210 → floor(210/7)+1 = 31
        LocalDate edd = LocalDate.now().plusWeeks(10);
        assertEquals(31, TimelineService.computeGestationalWeek(edd));
    }

    @Test
    void gestationalWeek_clampedToMin1() {
        // EDD far in future (conception not yet happened)
        LocalDate edd = LocalDate.now().plusDays(300);
        int week = TimelineService.computeGestationalWeek(edd);
        assertTrue(week >= 1, "Week should never be < 1, got " + week);
    }

    @Test
    void gestationalWeek_clampedToMax42() {
        // EDD far in past (overdue)
        LocalDate edd = LocalDate.now().minusWeeks(5);
        int week = TimelineService.computeGestationalWeek(edd);
        assertEquals(42, week);
    }

    // Trimester ---------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
        "1,  1",
        "8,  1",
        "13, 1",
        "14, 2",
        "20, 2",
        "27, 2",
        "28, 3",
        "36, 3",
        "40, 3",
        "42, 3"
    })
    void computeTrimester_correctByWeek(int week, int expectedTrimester) {
        assertEquals(expectedTrimester, TimelineService.computeTrimester(week));
    }
}
