from minio import Minio

from app import config

_client = Minio(
    config.MINIO_ENDPOINT,
    access_key=config.MINIO_ACCESS_KEY,
    secret_key=config.MINIO_SECRET_KEY,
    secure=config.MINIO_SECURE,
)


def download_video(storage_ref: str, dest_path: str) -> None:
    """storage_ref is the object key in the bucket - it's literally videoId.toString(),
    see MinioVideoStorageAdapter.presignedUploadUrl in video-intel-service."""
    _client.fget_object(config.MINIO_BUCKET, storage_ref, dest_path)
