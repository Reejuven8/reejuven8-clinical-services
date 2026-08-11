package com.reejuven8.ninemo.clinical.service.triage;

import com.reejuven8.ninemo.clinical.model.document.SymptomLog;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AnemiaRule implements TriageRule {

    @Override
    public boolean evaluate(SymptomLog log) {
        int week = log.getGestationalWeekAtLog();
        if (week < 14) return false; // only T2/T3

        Map<String, Object> vitals = log.getVitalsAtLog();
        if (vitals == null) return false;
        Number hb = (Number) vitals.get("hemoglobin_g_dl");
        return hb != null && hb.doubleValue() < 11.0;
    }

    @Override
    public String getRuleName() {
        return "AnemiaRule";
    }
}
