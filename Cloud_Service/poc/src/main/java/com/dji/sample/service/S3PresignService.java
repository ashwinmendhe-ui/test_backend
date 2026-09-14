package com.dji.sample.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;

@Service
public class S3PresignService {

    private final S3Presigner presigner;
    private final S3Client s3Client;

    @Value("${aws.s3.mission-bucket}")
    private String missionBucket;

    public S3PresignService(@Value("${aws.region}") String region) {
        this.presigner = S3Presigner.builder()
                .region(Region.of(region))
                .build();

        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .build();
        }


    @Value("${app.storage.local-stream-dir:}")
    private String localStreamDir;

    private Path localStreamFile(String key) {
        if (localStreamDir == null || localStreamDir.isBlank() || key == null) return null;
        try {
            Path root = Path.of(localStreamDir).toRealPath();
            Path candidate = root.resolve(key).normalize();
            if (!candidate.startsWith(root) || !Files.isRegularFile(candidate)) return null;
            Path real = candidate.toRealPath();
            return real.startsWith(root) ? real : null;
        } catch (IOException ex) { return null; }
    }

    public InputStream getStreamObject(String objectKey) {
        Path local = localStreamFile(objectKey);
        if (local != null) {
            try { return Files.newInputStream(local); }
            catch (IOException ex) { throw new UncheckedIOException(ex); }
        }
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(streamBucket)
                .key(objectKey)
                .build();

        return s3Client.getObject(request);
        }

        public boolean streamObjectExists(String objectKey) {
        if (localStreamFile(objectKey) != null) return true;
        try {
                HeadObjectRequest request = HeadObjectRequest.builder()
                        .bucket(streamBucket)
                        .key(objectKey)
                        .build();

                s3Client.headObject(request);
                return true;
        } catch (Exception e) {
                return false;
        }
        }

    public String createUploadUrl(String objectKey) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(missionBucket)
                .key(objectKey)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .putObjectRequest(putObjectRequest)
                .build();

        return presigner.presignPutObject(presignRequest)
                .url()
                .toString();
    }

    public String createDownloadUrl(String objectKey, String fileName) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(missionBucket)
                .key(objectKey)
                .responseContentDisposition(
                        "attachment; filename=\"" + fileName + "\""
                )
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(getObjectRequest)
                .build();

        return presigner.presignGetObject(presignRequest)
                .url()
                .toString();
    }

    @Value("${aws.s3.stream-bucket}")
        private String streamBucket;

        public String createStreamDownloadUrl(String objectKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(streamBucket)
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(getObjectRequest)
                .build();

        return presigner.presignGetObject(presignRequest)
                .url()
                .toString();
        }
}