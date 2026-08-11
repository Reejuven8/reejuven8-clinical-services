import boto3
from app.config import get_settings

settings = get_settings()


def get_s3_client():
    return boto3.client("s3", region_name=settings.aws_region)


def download_file(bucket: str, key: str) -> bytes:
    client = get_s3_client()
    response = client.get_object(Bucket=bucket, Key=key)
    return response["Body"].read()
