from typing import Any
from app.services.ocr_service import OcrService
from app.services.ner_service import NerService
from app.services.fhir_mapper import FhirMapper
from app.producers import kafka_producer
from app.config import get_settings
import structlog

logger = structlog.get_logger()
settings = get_settings()

_ocr = OcrService()
_ner = NerService()
_mapper = FhirMapper()


async def run(message: dict[str, Any]) -> None:
    patient_id: str = message["patientId"]
    s3_key: str = message["s3Key"]
    document_id: str = message.get("documentId", s3_key)
    correlation_id: str | None = message.get("correlationId")

    log = logger.bind(correlation_id=correlation_id) if correlation_id else logger
    log.info("Pipeline start", patient_id=patient_id, s3_key=s3_key)

    ocr_result = await _ocr.extract_text(
        s3_bucket=settings.s3_health_bucket,
        s3_key=s3_key,
        document_id=document_id,
        patient_id=patient_id,
    )

    if ocr_result.error or not ocr_result.raw_text:
        log.warning("OCR failed or empty", patient_id=patient_id, error=ocr_result.error)
        return

    if ocr_result.confidence_score < settings.ocr_confidence_threshold:
        log.warning(
            "OCR confidence too low",
            score=ocr_result.confidence_score,
            threshold=settings.ocr_confidence_threshold,
        )
        return

    observations = _ner.extract_observations(ocr_result.raw_text, patient_id, document_id)
    if not observations:
        log.info("No observations extracted", patient_id=patient_id, document_id=document_id)
        return

    fhir_resources = [_mapper.to_fhir_observation(obs) for obs in observations]

    await kafka_producer.publish_parsed_document(
        patient_id=patient_id,
        source_document_id=document_id,
        observations=fhir_resources,
        correlation_id=correlation_id,
    )
    log.info(
        "Pipeline complete",
        patient_id=patient_id,
        document_id=document_id,
        published=len(fhir_resources),
    )
