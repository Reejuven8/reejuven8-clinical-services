package com.reejuven8.ninemo.clinical.service.triage;

import com.reejuven8.ninemo.clinical.model.document.SymptomLog;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class PreeclampsiaRule implements TriageRule {

    private static final Set<String> NEUROLOGICAL_SYMPTOMS = Set.of("headache", "blurred_vision", "visual_disturbances");

    @Override
    public boolean evaluate(SymptomLog log) {
        if (log.getGestationalWeekAtLog() < 20) {
            return false;
        }
        Map<String, Object> vitals = log.getVitalsAtLog();
        if (vitals == null) return false;

        Number systolic = (Number) vitals.get("blood_pressure_systolic");
        Number diastolic = (Number) vitals.get("blood_pressure_diastolic");
        if (systolic == null || diastolic == null) return false;
        if (systolic.intValue() < 140 || diastolic.intValue() < 90) return false;

        List<Map<String, Object>> symptoms = log.getSymptoms();
        if (symptoms == null) return false;
        return symptoms.stream()
            .anyMatch(s -> NEUROLOGICAL_SYMPTOMS.contains(s.get("name")));
    }

    @Override
    public String getRuleName() {
        return "PreeclampsiaRule";
    }
}
