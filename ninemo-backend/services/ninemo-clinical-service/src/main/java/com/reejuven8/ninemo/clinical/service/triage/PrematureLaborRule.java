package com.reejuven8.ninemo.clinical.service.triage;

import com.reejuven8.ninemo.clinical.model.document.SymptomLog;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class PrematureLaborRule implements TriageRule {

    private static final Set<String> LABOR_SYMPTOMS = Set.of("regular_contractions", "pelvic_pressure", "lower_back_pain", "vaginal_discharge");

    @Override
    public boolean evaluate(SymptomLog log) {
        int week = log.getGestationalWeekAtLog();
        if (week >= 37) return false;

        List<Map<String, Object>> symptoms = log.getSymptoms();
        if (symptoms == null) return false;
        return symptoms.stream()
            .anyMatch(s -> LABOR_SYMPTOMS.contains(s.get("name")));
    }

    @Override
    public String getRuleName() {
        return "PrematureLaborRule";
    }
}
