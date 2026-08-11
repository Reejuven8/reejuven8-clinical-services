from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime


class QuantityValue(BaseModel):
    value: float
    unit: str
    system: str = "http://unitsofmeasure.org"


class LoincCode(BaseModel):
    system: str = "http://loinc.org"
    code: str
    display: str


class ParsedObservation(BaseModel):
    patient_id: str
    source_document_id: str
    resource_type: str = "Observation"
    loinc_code: LoincCode
    value_quantity: QuantityValue
    effective_datetime: datetime = Field(default_factory=datetime.utcnow)
    confidence_score: float = Field(ge=0.0, le=1.0)
    raw_text_excerpt: Optional[str] = None
    category: str = "laboratory"
