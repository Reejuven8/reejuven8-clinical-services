from datetime import timezone
from typing import Any
from app.models.parsed_observation import ParsedObservation
import structlog

logger = structlog.get_logger()


class FhirMapper:
    def to_fhir_observation(self, observation: ParsedObservation) -> dict[str, Any]:
        effective = observation.effective_datetime
        if effective.tzinfo is None:
            effective = effective.replace(tzinfo=timezone.utc)

        return {
            "resourceType": "Observation",
            "status": "final",
            "category": [
                {
                    "coding": [
                        {
                            "system": "http://terminology.hl7.org/CodeSystem/observation-category",
                            "code": observation.category,
                            "display": observation.category.capitalize(),
                        }
                    ]
                }
            ],
            "code": {
                "coding": [
                    {
                        "system": observation.loinc_code.system,
                        "code": observation.loinc_code.code,
                        "display": observation.loinc_code.display,
                    }
                ],
                "text": observation.loinc_code.display,
            },
            "subject": {"reference": f"Patient/{observation.patient_id}"},
            "effectiveDateTime": effective.isoformat(),
            "valueQuantity": {
                "value": observation.value_quantity.value,
                "unit": observation.value_quantity.unit,
                "system": observation.value_quantity.system,
                "code": observation.value_quantity.unit,
            },
            "derivedFrom": [{"reference": f"DocumentReference/{observation.source_document_id}"}],
            "extension": [
                {
                    "url": "http://reejuven8.com/fhir/extension/parsing-confidence",
                    "valueDecimal": observation.confidence_score,
                }
            ],
        }
