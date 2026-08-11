package com.reejuven8.ninemo.clinical.service.timeline;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class IVFCalculationStrategy implements EDDCalculationStrategy {

    @Override
    public LocalDate calculateEdd(LocalDate ivfDay5TransferDate) {
        // Day-5 blastocyst transfer: add 261 days (266 days - 5 days already elapsed)
        return ivfDay5TransferDate.plusDays(261);
    }
}
