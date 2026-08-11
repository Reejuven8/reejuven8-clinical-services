import asyncio
import json
import uuid
from datetime import datetime, timezone
from typing import Any
from confluent_kafka import Producer, KafkaException
import structlog
from app.config import get_settings

logger = structlog.get_logger()
settings = get_settings()

_producer: Producer | None = None


def _get_producer() -> Producer:
    global _producer
    if _producer is None:
        _producer = Producer({"bootstrap.servers": settings.kafka_bootstrap_servers})
    return _producer


def _delivery_report(err, msg):
    if err:
        logger.error("Kafka delivery failed", error=str(err))
    else:
        logger.debug("Kafka message delivered", topic=msg.topic(), partition=msg.partition())


async def publish_parsed_document(
    patient_id: str,
    source_document_id: str,
    observations: list[dict[str, Any]],
    correlation_id: str | None = None,
) -> None:
    event = {
        "eventId": str(uuid.uuid4()),
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "source": "ai-parsing-service",
        "correlationId": correlation_id,
        "patientId": patient_id,
        "sourceDocumentId": source_document_id,
        "observations": observations,
    }
    payload = json.dumps(event).encode("utf-8")
    producer = _get_producer()

    await asyncio.get_event_loop().run_in_executor(
        None,
        lambda: producer.produce(
            settings.kafka_parsed_topic,
            key=patient_id.encode(),
            value=payload,
            callback=_delivery_report,
        ),
    )
    producer.poll(0)
    logger.info(
        "Published DocumentParsedEvent",
        patient_id=patient_id,
        source_document_id=source_document_id,
        observation_count=len(observations),
    )
