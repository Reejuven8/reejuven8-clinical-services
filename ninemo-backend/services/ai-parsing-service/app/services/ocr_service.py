import asyncio
import structlog
import boto3
from botocore.exceptions import ClientError
from app.config import get_settings
from app.models.ocr_result import OcrResult

logger = structlog.get_logger()
settings = get_settings()


class OcrService:
    def __init__(self):
        self._textract = None

    @property
    def textract(self):
        if self._textract is None:
            self._textract = boto3.client("textract", region_name=settings.aws_region)
        return self._textract

    async def extract_text(
        self, s3_bucket: str, s3_key: str, document_id: str, patient_id: str
    ) -> OcrResult:
        try:
            result = await asyncio.get_event_loop().run_in_executor(
                None, self._call_textract, s3_bucket, s3_key
            )
            return OcrResult(
                document_id=document_id,
                patient_id=patient_id,
                s3_url=f"s3://{s3_bucket}/{s3_key}",
                raw_text=result["text"],
                confidence_score=result["confidence"],
                page_count=result["page_count"],
            )
        except ClientError as e:
            logger.error("Textract error", document_id=document_id, error=str(e))
            return OcrResult(
                document_id=document_id,
                patient_id=patient_id,
                s3_url=f"s3://{s3_bucket}/{s3_key}",
                raw_text="",
                confidence_score=0.0,
                error=str(e),
            )

    def _call_textract(self, bucket: str, key: str) -> dict:
        response = self.textract.detect_document_text(
            Document={"S3Object": {"Bucket": bucket, "Name": key}}
        )
        blocks = response.get("Blocks", [])
        lines = [b["Text"] for b in blocks if b["BlockType"] == "LINE"]
        confidences = [b["Confidence"] / 100.0 for b in blocks if b["BlockType"] == "LINE"]
        page_count = len({b.get("Page", 1) for b in blocks})
        avg_confidence = sum(confidences) / len(confidences) if confidences else 0.0
        return {
            "text": "\n".join(lines),
            "confidence": avg_confidence,
            "page_count": page_count,
        }
