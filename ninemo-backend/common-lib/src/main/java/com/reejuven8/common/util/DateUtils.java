package com.reejuven8.common.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class DateUtils {

    private DateUtils() {}

    public static LocalDate eddFromLmp(LocalDate lmpDate) {
        return lmpDate.plusDays(280);
    }

    public static LocalDate eddFromIvfDay5Transfer(LocalDate transferDate) {
        return transferDate.plusDays(266);
    }

    public static int gestationalWeek(LocalDate edd) {
        LocalDate today = LocalDate.now();
        long daysFromLmp = ChronoUnit.DAYS.between(edd.minusDays(280), today);
        return (int) (daysFromLmp / 7);
    }

    public static int gestationalDay(LocalDate edd) {
        LocalDate today = LocalDate.now();
        long daysFromLmp = ChronoUnit.DAYS.between(edd.minusDays(280), today);
        return (int) (daysFromLmp % 7);
    }

    public static int trimester(int gestationalWeek) {
        if (gestationalWeek <= 13) return 1;
        if (gestationalWeek <= 27) return 2;
        return 3;
    }
}
