package com.dji.sample.service.impl;

import com.dji.sample.dto.request.AiServiceStreamRequest;
import com.dji.sample.dto.response.AiServiceStreamResponse;
import com.dji.sample.service.IAiServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of AI Service Client for stream processing integration
 * 
 * @author DHive Team
 * @date 2025-12-22
 */
@Service
@Slf4j
public class AiServiceClientImpl implements IAiServiceClient {

    @Value("${ai-service.base-url:http://10.17.14.114:7879}")
    private String aiServiceBaseUrl;

    @Value("${ai-service.stream-endpoint:/api/stream}")
    private String streamEndpoint;

    @Value("${ai-service.timeout:10000}")
    private int timeout;

    private final RestTemplate restTemplate;

    public AiServiceClientImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String registerStream(AiServiceStreamRequest request) {
        try {
            String url = aiServiceBaseUrl + streamEndpoint;
            
            log.info("Registering stream with AI service: url={}, stream_id={}", url, request.getStreamId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<AiServiceStreamRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<AiServiceStreamResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                AiServiceStreamResponse.class
            );

            AiServiceStreamResponse result = response.getBody();
            if (result != null && "success".equalsIgnoreCase(result.getStatus())) {
                log.info("Stream registered successfully with AI service: stream_id={}, status: {}, playbackUrl : {}", request.getStreamId(), result.getStatus(), result.getPlaybackUrl());
                return result.getPlaybackUrl();
            } else {
                log.error("Failed to register stream with AI service: {}", result != null ? result.getMessage() : "Unknown error");
                return "";
            }

        } catch (Exception e) {
            log.error("Error calling AI service for stream registration", e);
            return "";
        }
    }

    @Override
    public AiServiceStreamResponse unregisterStream(String streamId) {
        try {
            String url = aiServiceBaseUrl + streamEndpoint + "/" + streamId + "/delete";
            
            log.info("Unregistering stream from AI service: stream_id={}", streamId);

            ResponseEntity<AiServiceStreamResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                null,
                AiServiceStreamResponse.class
            );

            AiServiceStreamResponse result = response.getBody();
            log.info("Stream unregistered from AI service: stream_id={}", streamId);
            
            return result;

        } catch (Exception e) {
            log.error("Error calling AI service for stream unregistration", e);
            return AiServiceStreamResponse.builder()
                .state("error")
                .message("Failed to unregister from AI service: " + e.getMessage())
                .build();
        }
    }

    @Override
    public AiServiceStreamResponse getStreamInfo(String streamId) {
        try {
            String url = aiServiceBaseUrl + streamEndpoint + "/" + streamId;
            log.info("Getting stream info from AI service: stream_id={}", streamId);
            ResponseEntity<AiServiceStreamResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                AiServiceStreamResponse.class
            );
            AiServiceStreamResponse result = response.getBody();
            if (result != null) {
                log.info("Stream info retrieved: stream_id={}, status={}", streamId, result.getStatus());
                return result;
            } else {
                return null;
            }
        } catch (Exception e) {
           return null;
        }
    }

    @Override
    public String getStreamInfoRaw(String streamId) {
        try {
            String url = aiServiceBaseUrl + streamEndpoint + "/" + streamId;
            log.info("Getting raw stream info from AI service: stream_id={}", streamId);
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                String.class
            );
            log.info("Stream {} info : {} ", streamId, response.getBody());
            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting raw stream info from AI service", e);
            return null;
        }
    }
}
