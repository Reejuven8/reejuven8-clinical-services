import pytest
from app.services.fhir_mapper import FhirMapper
from app.models.parsed_observation import ParsedObservation


@pytest.fixture
def mapper():
    return FhirMapper()


@pytest.fixture
def hb_observation():
    return ParsedObservation(
        parameter_name="Haemoglobin",
        value=10.5,
        unit="g/dL",
        loinc_code="718-7",
        confidence=0.95,
    )


def test_builds_fhir_observation(mapper, hb_observation):
    fhir = mapper.build_observation(hb_observation, patient_id="patient-uuid-123")
    assert fhir["resourceType"] == "Observation"
    assert fhir["status"] == "final"


def test_fhir_subject_reference(mapper, hb_observation):
    fhir = mapper.build_observation(hb_observation, patient_id="patient-uuid-123")
    assert fhir["subject"]["reference"] == "Patient/patient-uuid-123"


def test_fhir_has_loinc_coding(mapper, hb_observation):
    fhir = mapper.build_observation(hb_observation, patient_id="patient-uuid-123")
    codings = fhir["code"]["coding"]
    loinc = next((c for c in codings if c["system"] == "http://loinc.org"), None)
    assert loinc is not None
    assert loinc["code"] == "718-7"


def test_fhir_value_quantity(mapper, hb_observation):
    fhir = mapper.build_observation(hb_observation, patient_id="patient-uuid-123")
    vq = fhir["valueQuantity"]
    assert abs(vq["value"] - 10.5) < 0.01
    assert vq["unit"] == "g/dL"


def test_fhir_display_name(mapper, hb_observation):
    fhir = mapper.build_observation(hb_observation, patient_id="patient-uuid-123")
    codings = fhir["code"]["coding"]
    loinc = next((c for c in codings if c["system"] == "http://loinc.org"), None)
    assert "display" in loinc
