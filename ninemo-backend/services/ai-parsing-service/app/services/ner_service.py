import re
from datetime import datetime, timezone
from typing import List
from app.models.parsed_observation import ParsedObservation, LoincCode, QuantityValue
from app.utils.loinc_mapper import lookup_fuzzy
import structlog

logger = structlog.get_logger()

# Pattern: "label : value unit" — covers most Indian lab report formats
_LAB_PATTERN = re.compile(
    r"([A-Za-z][A-Za-z\s\(\)/]+?)\s*:?\s*(\d+\.?\d*)\s*([A-Za-z%/\[\].*]+)",
    re.IGNORECASE,
)

# Normalise extracted unit strings to UCUM-ish equivalents
_UNIT_ALIASES = {
    "g/dl": "g/dL",
    "g/dL": "g/dL",
    "mg/dl": "mg/dL",
    "mg/dL": "mg/dL",
    "miu/l": "mIU/L",
    "mIU/L": "mIU/L",
    "ng/ml": "ng/mL",
    "/min": "/min",
    "bpm": "/min",
    "kg": "kg",
    "cm": "cm",
    "mm[hg]": "mm[Hg]",
    "mmhg": "mm[Hg]",
}


def _normalise_unit(raw: str) -> str:
    return _UNIT_ALIASES.get(raw.lower(), raw)


class NerService:
    def __init__(self):
        self._nlp = None

    def _load_model(self):
        """Lazy-load SciSpacy model; silently skip if not installed."""
        if self._nlp is not None:
            return
        try:
            import spacy
            self._nlp = spacy.load("en_core_sci_sm")
            logger.info("SciSpacy model loaded")
        except (ImportError, OSError):
            logger.warning("en_core_sci_sm not available — using regex NER only")
            self._nlp = False  # sentinel: tried and failed

    def extract_observations(
        self, raw_text: str, patient_id: str, document_id: str
    ) -> List[ParsedObservation]:
        self._load_model()
        observations: List[ParsedObservation] = []

        if self._nlp:
            observations = self._spacy_extract(raw_text, patient_id, document_id)

        # Regex pass always runs — merges unique codes
        existing_codes = {obs.loinc_code.code for obs in observations}
        regex_obs = self._regex_extract(raw_text, patient_id, document_id)
        for obs in regex_obs:
            if obs.loinc_code.code not in existing_codes:
                observations.append(obs)
                existing_codes.add(obs.loinc_code.code)

        logger.info(
            "NER complete",
            patient_id=patient_id,
            document_id=document_id,
            observations=len(observations),
        )
        return observations

    def _regex_extract(
        self, text: str, patient_id: str, document_id: str
    ) -> List[ParsedObservation]:
        results = []
        for match in _LAB_PATTERN.finditer(text):
            label = match.group(1).strip()
            raw_value = match.group(2)
            raw_unit = match.group(3).strip()

            loinc = lookup_fuzzy(label)
            if loinc is None:
                continue

            try:
                value = float(raw_value)
            except ValueError:
                continue

            unit = _normalise_unit(raw_unit) or loinc.get("unit", raw_unit)

            results.append(ParsedObservation(
                patient_id=patient_id,
                source_document_id=document_id,
                loinc_code=LoincCode(
                    code=loinc["code"],
                    display=loinc["display"],
                ),
                value_quantity=QuantityValue(value=value, unit=unit),
                effective_datetime=datetime.now(timezone.utc),
                confidence_score=0.75,
                raw_text_excerpt=match.group(0),
            ))
        return results

    def _spacy_extract(
        self, text: str, patient_id: str, document_id: str
    ) -> List[ParsedObservation]:
        """Use SciSpacy NER entities then pair with regex values."""
        doc = self._nlp(text)
        results = []
        for ent in doc.ents:
            loinc = lookup_fuzzy(ent.text)
            if loinc is None:
                continue
            # Search for a numeric value near the entity in the sentence
            sent_text = ent.sent.text
            num_match = re.search(r"(\d+\.?\d*)\s*([A-Za-z%/\[\]]+)?", sent_text[ent.start_char - ent.sent.start_char:])
            if not num_match:
                continue
            try:
                value = float(num_match.group(1))
            except ValueError:
                continue
            unit = _normalise_unit(num_match.group(2) or "") or loinc.get("unit", "")
            results.append(ParsedObservation(
                patient_id=patient_id,
                source_document_id=document_id,
                loinc_code=LoincCode(code=loinc["code"], display=loinc["display"]),
                value_quantity=QuantityValue(value=value, unit=unit),
                effective_datetime=datetime.now(timezone.utc),
                confidence_score=0.85,
                raw_text_excerpt=ent.sent.text[:200],
            ))
        return results
