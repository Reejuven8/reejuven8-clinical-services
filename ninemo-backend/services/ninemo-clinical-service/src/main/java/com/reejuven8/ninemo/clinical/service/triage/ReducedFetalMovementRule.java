package com.reejuven8.ninemo.clinical.service.triage;

import com.reejuven8.ninemo.clinical.model.document.SymptomLog;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ReducedFetalMovementRule implements TriageRule {

    private static final Set<String> REDUCED_FM_SYMPTOMS = Set.of("reduced_fetal_movement", "no_fetal_movement");

    @Override
    public boolean evaluate(SymptomLog log) {
        if (log.getGestationalWeekAtLog() < 28) return false;

        List<Map<String, Object>> symptoms = log.getSymptoms();
        if (symptoms == null) return false;
        return symptoms.stream()
            .anyMatch(s -> REDUCED_FM_SYMPTOMS.contains(s.get("name")));
    }

    @Override
    public String getRuleName() {
        return "ReducedFetalMovementRule";
    }
}
