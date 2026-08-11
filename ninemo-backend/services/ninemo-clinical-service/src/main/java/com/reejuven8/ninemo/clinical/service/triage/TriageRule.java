package com.reejuven8.ninemo.clinical.service.triage;

import com.reejuven8.ninemo.clinical.model.document.SymptomLog;

public interface TriageRule {
    boolean evaluate(SymptomLog log);
    String getRuleName();
}
