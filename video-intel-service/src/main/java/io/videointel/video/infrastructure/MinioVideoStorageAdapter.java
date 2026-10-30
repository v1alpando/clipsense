package io.videointel.video.infrastructure;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import io.videointel.video.domain.VideoStorageException;
import io.videointel.video.domain.VideoStoragePort;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
class MinioVideoStorageAdapter implements VideoStoragePort {

    private final MinioClient minioClient;
    private final MinioProperties properties;

    @PostConstruct
    void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(properties.getBucket()).build()
            );
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.getBucket()).build());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to ensure MinIO bucket exists: " + properties.getBucket(), e);
        }
    }

    @Override
    public String presignedUploadUrl(UUID videoId) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(properties.getBucket())
                            .object(videoId.toString())
                            .expiry(properties.getPresignedUrlExpiryMinutes(), TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            throw new VideoStorageException("Failed to generate upload URL for video " + videoId, e);
        }
    }

    @Override
    public String presignedPlaybackUrl(String storageRef) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(properties.getBucket())
                            .object(storageRef)
                            .expiry(properties.getPresignedUrlExpiryMinutes(), TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            throw new VideoStorageException("Failed to generate playback URL for storage ref " + storageRef, e);
        }
    }

    @Override
    public boolean exists(String storageRef) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder().bucket(properties.getBucket()).object(storageRef).build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return false;
            }
            throw new VideoStorageException("Failed to check storage object " + storageRef, e);
        } catch (Exception e) {
            throw new VideoStorageException("Failed to check storage object " + storageRef, e);
        }
    }
}
