import asyncio
from contextlib import asynccontextmanager
import structlog
from fastapi import FastAPI
from app.config import get_settings
from app.routers import parse_router
from app.consumers import rabbitmq_consumer

logger = structlog.get_logger()
settings = get_settings()

_consumer_task: asyncio.Task | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global _consumer_task
    logger.info("ai-parsing-service starting", version=settings.app_version)
    _consumer_task = asyncio.create_task(
        rabbitmq_consumer.start_consumer(),
        name="rabbitmq-consumer",
    )
    yield
    if _consumer_task and not _consumer_task.done():
        _consumer_task.cancel()
        try:
            await _consumer_task
        except asyncio.CancelledError:
            pass
    logger.info("ai-parsing-service shut down")


app = FastAPI(
    title="NineMo AI Parsing Service",
    version=settings.app_version,
    description="OCR, Medical NER, and LOINC mapping pipeline",
    lifespan=lifespan,
)

app.include_router(parse_router.router)


@app.get("/health")
async def health():
    return {"status": "ok", "service": settings.app_name, "version": settings.app_version}
