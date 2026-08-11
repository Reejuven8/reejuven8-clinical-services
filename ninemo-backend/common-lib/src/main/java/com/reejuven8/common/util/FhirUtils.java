package com.reejuven8.common.util;

public final class FhirUtils {

    private FhirUtils() {}

    public static final String FHIR_R4_BASE_URL = "http://hl7.org/fhir/R4";
    public static final String LOINC_SYSTEM = "http://loinc.org";
    public static final String UCUM_SYSTEM = "http://unitsofmeasure.org";

    // LOINC codes for common Indian lab vitals
    public static final String LOINC_HEMOGLOBIN = "718-7";
    public static final String LOINC_BP_SYSTOLIC = "8480-6";
    public static final String LOINC_BP_DIASTOLIC = "8462-4";
    public static final String LOINC_TSH = "3016-3";
    public static final String LOINC_FASTING_GLUCOSE = "1558-6";
    public static final String LOINC_FETAL_HEART_RATE = "55283-6";
}
