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

@Service
public class S3PresignService {

    private final S3Presigner presigner;

    @Value("${aws.s3.mission-bucket}")
    private String missionBucket;

    public S3PresignService(@Value("${aws.region}") String region) {
        this.presigner = S3Presigner.builder()
                .region(Region.of(region))
                .build();
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
}