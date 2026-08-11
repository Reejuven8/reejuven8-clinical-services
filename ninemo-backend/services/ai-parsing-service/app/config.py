from pydantic_settings import BaseSettings, SettingsConfigDict
from functools import lru_cache


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    app_name: str = "ai-parsing-service"
    app_version: str = "1.0.0"
    debug: bool = False

    # RabbitMQ
    rabbitmq_host: str = "localhost"
    rabbitmq_port: int = 5672
    rabbitmq_username: str = "reejuven8"
    rabbitmq_password: str = "dev_password"
    rabbitmq_vhost: str = "/"
    rabbitmq_queue: str = "document.unstructured.uploaded"
    rabbitmq_dlx_queue: str = "document.unstructured.uploaded.dlx"
    rabbitmq_prefetch_count: int = 1

    # Kafka
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_parsed_topic: str = "document.data.parsed"

    # AWS
    aws_region: str = "ap-south-1"
    aws_access_key_id: str = ""
    aws_secret_access_key: str = ""
    s3_health_bucket: str = "reejuven8-health"

    # Model config
    ner_model: str = "en_core_sci_sm"
    ocr_confidence_threshold: float = 0.6


@lru_cache
def get_settings() -> Settings:
    return Settings()
