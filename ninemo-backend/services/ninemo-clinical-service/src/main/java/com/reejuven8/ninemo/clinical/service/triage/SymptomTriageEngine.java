package com.reejuven8.ninemo.clinical.service.triage;

import com.reejuven8.ninemo.clinical.model.document.SymptomLog;
import com.reejuven8.ninemo.clinical.model.enums.SeverityFlag;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SymptomTriageEngine {

    private final List<TriageRule> rules;
    private final MeterRegistry meterRegistry;

    public List<String> evaluate(SymptomLog log) {
        List<String> triggered = rules.stream()
            .filter(rule -> rule.evaluate(log))
            .map(TriageRule::getRuleName)
            .toList();

        SeverityFlag severity;
        if (triggered.isEmpty()) {
            severity = SeverityFlag.NORMAL;
        } else if (triggered.contains("PreeclampsiaRule")
                || triggered.contains("PrematureLaborRule")
                || triggered.contains("ReducedFetalMovementRule")) {
            severity = SeverityFlag.CRITICAL;
        } else {
            severity = SeverityFlag.WARNING;
        }
        log.setSeverityFlag(severity);

        Counter.builder("ninemo_symptom_triage_total")
            .description("Triage invocations by severity")
            .tag("severity", severity.name())
            .register(meterRegistry)
            .increment();

        return triggered;
    }
}
