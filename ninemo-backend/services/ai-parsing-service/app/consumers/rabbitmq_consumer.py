import asyncio
import json
import aio_pika
import structlog
from tenacity import retry, stop_after_attempt, wait_exponential, before_sleep_log
import logging
from app.config import get_settings
from app.services import pipeline

logger = structlog.get_logger()
settings = get_settings()


@retry(
    stop=stop_after_attempt(10),
    wait=wait_exponential(multiplier=1, min=2, max=30),
    before_sleep=before_sleep_log(logging.getLogger(__name__), logging.WARNING),
)
async def _connect() -> aio_pika.abc.AbstractRobustConnection:
    url = (
        f"amqp://{settings.rabbitmq_username}:{settings.rabbitmq_password}"
        f"@{settings.rabbitmq_host}:{settings.rabbitmq_port}{settings.rabbitmq_vhost}"
    )
    return await aio_pika.connect_robust(url)


async def _process_message(message: aio_pika.IncomingMessage) -> None:
    async with message.process(requeue=True):
        try:
            body = json.loads(message.body.decode())
            logger.info("RabbitMQ message received", routing_key=message.routing_key)
            await pipeline.run(body)
        except Exception as e:
            logger.error("Pipeline error — message will be nacked to DLX", error=str(e))
            raise


async def start_consumer() -> None:
    connection = await _connect()
    channel = await connection.channel()
    await channel.set_qos(prefetch_count=settings.rabbitmq_prefetch_count)

    queue = await channel.declare_queue(
        settings.rabbitmq_queue,
        durable=True,
        arguments={
            "x-dead-letter-exchange": "document.unstructured.dlx",
            "x-dead-letter-routing-key": settings.rabbitmq_queue,
        },
    )

    await queue.consume(_process_message)
    logger.info("RabbitMQ consumer ready", queue=settings.rabbitmq_queue)

    # Keep running — caller (lifespan) holds this coroutine alive
    await asyncio.Future()
