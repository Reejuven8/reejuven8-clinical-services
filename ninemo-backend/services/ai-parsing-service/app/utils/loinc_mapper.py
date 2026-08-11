# Static LOINC lookup for common Indian lab test terms
LOINC_MAP: dict[str, dict] = {
    "hemoglobin": {"code": "718-7", "display": "Hemoglobin [Mass/volume] in Blood", "unit": "g/dL"},
    "hb": {"code": "718-7", "display": "Hemoglobin [Mass/volume] in Blood", "unit": "g/dL"},
    "bp systolic": {"code": "8480-6", "display": "Systolic blood pressure", "unit": "mm[Hg]"},
    "systolic bp": {"code": "8480-6", "display": "Systolic blood pressure", "unit": "mm[Hg]"},
    "systolic": {"code": "8480-6", "display": "Systolic blood pressure", "unit": "mm[Hg]"},
    "bp diastolic": {"code": "8462-4", "display": "Diastolic blood pressure", "unit": "mm[Hg]"},
    "diastolic bp": {"code": "8462-4", "display": "Diastolic blood pressure", "unit": "mm[Hg]"},
    "diastolic": {"code": "8462-4", "display": "Diastolic blood pressure", "unit": "mm[Hg]"},
    "tsh": {"code": "3016-3", "display": "TSH [Units/volume] in Serum or Plasma", "unit": "mIU/L"},
    "fasting glucose": {"code": "1558-6", "display": "Fasting glucose [Mass/volume] in Blood", "unit": "mg/dL"},
    "fbs": {"code": "1558-6", "display": "Fasting glucose [Mass/volume] in Blood", "unit": "mg/dL"},
    "blood glucose fasting": {"code": "1558-6", "display": "Fasting glucose [Mass/volume] in Blood", "unit": "mg/dL"},
    "fetal heart rate": {"code": "55283-6", "display": "Fetal Heart Rate", "unit": "/min"},
    "fhr": {"code": "55283-6", "display": "Fetal Heart Rate", "unit": "/min"},
    "weight": {"code": "29463-7", "display": "Body weight", "unit": "kg"},
    "body weight": {"code": "29463-7", "display": "Body weight", "unit": "kg"},
    "height": {"code": "8302-2", "display": "Body height", "unit": "cm"},
    "haemoglobin": {"code": "718-7", "display": "Hemoglobin [Mass/volume] in Blood", "unit": "g/dL"},
    "serum ferritin": {"code": "2276-4", "display": "Ferritin [Mass/volume] in Serum or Plasma", "unit": "ng/mL"},
    "ferritin": {"code": "2276-4", "display": "Ferritin [Mass/volume] in Serum or Plasma", "unit": "ng/mL"},
    "platelet count": {"code": "777-3", "display": "Platelets [#/volume] in Blood by Automated count", "unit": "10*3/uL"},
    "platelets": {"code": "777-3", "display": "Platelets [#/volume] in Blood by Automated count", "unit": "10*3/uL"},
    "creatinine": {"code": "2160-0", "display": "Creatinine [Mass/volume] in Serum or Plasma", "unit": "mg/dL"},
    "serum creatinine": {"code": "2160-0", "display": "Creatinine [Mass/volume] in Serum or Plasma", "unit": "mg/dL"},
    "uric acid": {"code": "3084-1", "display": "Urate [Mass/volume] in Serum or Plasma", "unit": "mg/dL"},
}


def lookup(term: str) -> dict | None:
    return LOINC_MAP.get(term.lower().strip())


def lookup_fuzzy(term: str) -> dict | None:
    """Substring match — used when exact lookup fails."""
    term_lower = term.lower().strip()
    result = LOINC_MAP.get(term_lower)
    if result:
        return result
    for key, value in LOINC_MAP.items():
        if key in term_lower or term_lower in key:
            return value
    return None
