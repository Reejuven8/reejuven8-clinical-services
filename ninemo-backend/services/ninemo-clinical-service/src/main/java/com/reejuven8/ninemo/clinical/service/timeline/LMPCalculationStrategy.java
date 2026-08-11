package com.reejuven8.ninemo.clinical.service.timeline;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class LMPCalculationStrategy implements EDDCalculationStrategy {

    @Override
    public LocalDate calculateEdd(LocalDate lmpDate) {
        // Naegele's rule: LMP + 280 days
        return lmpDate.plusDays(280);
    }
}
