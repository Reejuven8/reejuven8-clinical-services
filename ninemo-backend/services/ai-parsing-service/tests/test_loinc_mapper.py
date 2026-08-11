import pytest
from app.utils.loinc_mapper import LoincMapper


@pytest.fixture
def mapper():
    return LoincMapper()


def test_haemoglobin_exact_match(mapper):
    code, confidence = mapper.map("Haemoglobin")
    assert code == "718-7"
    assert confidence >= 0.9


def test_hemoglobin_variant(mapper):
    code, confidence = mapper.map("Hemoglobin")
    assert code == "718-7"
    assert confidence >= 0.75


def test_hb_abbreviation(mapper):
    code, confidence = mapper.map("Hb")
    assert code == "718-7"
    assert confidence >= 0.75


def test_tsh_maps_correctly(mapper):
    code, confidence = mapper.map("TSH")
    assert code == "3016-3"
    assert confidence >= 0.9


def test_fasting_glucose_maps_correctly(mapper):
    code, confidence = mapper.map("Fasting Blood Glucose")
    assert code == "2339-0"
    assert confidence >= 0.75


def test_unknown_term_returns_none(mapper):
    code, confidence = mapper.map("xyzabc123")
    assert code is None
    assert confidence == 0.0
