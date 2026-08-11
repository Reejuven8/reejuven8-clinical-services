import pytest
from app.services.ner_service import NerService


@pytest.fixture
def ner():
    return NerService()


def test_extracts_hemoglobin(ner):
    text = "Haemoglobin: 10.5 g/dL"
    observations = ner.extract_observations(text)
    hb = next((o for o in observations if "haemoglobin" in o.parameter_name.lower()), None)
    assert hb is not None
    assert abs(hb.value - 10.5) < 0.01
    assert hb.unit == "g/dL"


def test_extracts_glucose(ner):
    text = "Fasting Blood Glucose: 92 mg/dL"
    observations = ner.extract_observations(text)
    glucose = next((o for o in observations if "glucose" in o.parameter_name.lower()), None)
    assert glucose is not None
    assert abs(glucose.value - 92.0) < 0.01


def test_extracts_tsh(ner):
    text = "TSH: 2.5 mIU/L"
    observations = ner.extract_observations(text)
    tsh = next((o for o in observations if "tsh" in o.parameter_name.lower()), None)
    assert tsh is not None
    assert abs(tsh.value - 2.5) < 0.01


def test_extracts_multiple(ner):
    text = "Hb: 11.2 g/dL\nTSH: 2.5 mIU/L\nGlucose: 88 mg/dL"
    observations = ner.extract_observations(text)
    assert len(observations) >= 3


def test_empty_text_returns_empty_list(ner):
    assert ner.extract_observations("") == []


def test_no_match_returns_empty_list(ner):
    result = ner.extract_observations("Patient name: John. DOB: 01/01/1990.")
    assert result == []
