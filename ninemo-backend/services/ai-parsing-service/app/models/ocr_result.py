from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime


class OcrResult(BaseModel):
    document_id: str
    patient_id: str
    s3_url: str
    raw_text: str
    confidence_score: float = Field(ge=0.0, le=1.0)
    page_count: int = 1
    parsed_at: datetime = Field(default_factory=datetime.utcnow)
    error: Optional[str] = None
