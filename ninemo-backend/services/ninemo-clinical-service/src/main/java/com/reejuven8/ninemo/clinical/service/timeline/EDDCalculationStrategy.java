package com.reejuven8.ninemo.clinical.service.timeline;

import java.time.LocalDate;

public interface EDDCalculationStrategy {
    LocalDate calculateEdd(LocalDate inputDate);
}
