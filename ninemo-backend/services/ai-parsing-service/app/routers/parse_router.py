from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from app.services import pipeline
import structlog

logger = structlog.get_logger()
router = APIRouter(prefix="/api/v1/ai", tags=["parsing"])


class ParseRequest(BaseModel):
    patientId: str
    s3Key: str
    documentId: str | None = None


@router.get("/status")
async def status():
    return {"status": "ok", "pipeline": "active"}


@router.post("/parse")
async def trigger_parse(request: ParseRequest):
    """Manual trigger for the parsing pipeline (admin/test use)."""
    try:
        await pipeline.run({
            "patientId": request.patientId,
            "s3Key": request.s3Key,
            "documentId": request.documentId or request.s3Key,
        })
        return {"status": "queued", "patientId": request.patientId, "s3Key": request.s3Key}
    except Exception as e:
        logger.error("Manual parse trigger failed", error=str(e))
        raise HTTPException(status_code=500, detail=str(e))
