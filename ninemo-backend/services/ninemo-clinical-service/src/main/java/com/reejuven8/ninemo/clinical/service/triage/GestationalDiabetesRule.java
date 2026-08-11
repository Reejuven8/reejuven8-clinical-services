package com.reejuven8.ninemo.clinical.service.triage;

import com.reejuven8.ninemo.clinical.model.document.SymptomLog;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GestationalDiabetesRule implements TriageRule {

    @Override
    public boolean evaluate(SymptomLog log) {
        int week = log.getGestationalWeekAtLog();
        if (week < 24 || week > 28) return false;

        Map<String, Object> vitals = log.getVitalsAtLog();
        if (vitals == null) return false;
        Number glucose = (Number) vitals.get("fasting_glucose_mg_dl");
        return glucose != null && glucose.doubleValue() >= 92.0;
    }

    @Override
    public String getRuleName() {
        return "GestationalDiabetesRule";
    }
}
