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


    public InputStream getStreamObject(String objectKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(streamBucket)
                .key(objectKey)
                .build();

        return s3Client.getObject(request);
        }

        public boolean streamObjectExists(String objectKey) {
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