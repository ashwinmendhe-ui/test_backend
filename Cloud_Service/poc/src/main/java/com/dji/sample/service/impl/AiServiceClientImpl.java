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

@Service
@Slf4j
public class AiServiceClientImpl implements IAiServiceClient {

    @Value("${ai-service.base-url:http://10.17.14.114:7879}")
    private String aiServiceBaseUrl;

    @Value("${ai-service.stream-endpoint:/api/stream}")
    private String streamEndpoint;

    private final RestTemplate restTemplate;

    public AiServiceClientImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String registerStream(AiServiceStreamRequest request) {
        try {
            String url = aiServiceBaseUrl + streamEndpoint;

            log.info("Registering stream with AI service: url={}, stream_id={}", url, request.getStreamId());
            log.info(
                    "AI request fields: uri={}, vectorMapUri={}, streamId={}, deviceId={}, deviceName={}, companyId={}, companyName={}, siteId={}, siteName={}, missionId={}, missionName={}, userId={}, userName={}, sessionStartTime={}, emails={}",
                    request.getUri(),
                    request.getVectorMapUri(),
                    request.getStreamId(),
                    request.getDeviceId(),
                    request.getDeviceName(),
                    request.getCompanyId(),
                    request.getCompanyName(),
                    request.getSiteId(),
                    request.getSiteName(),
                    request.getMissionId(),
                    request.getMissionName(),
                    request.getUserId(),
                    request.getUserName(),
                    request.getSessionStartTime(),
                    request.getEmails()
            );

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

            log.info("AI register response: statusCode={}, body={}", response.getStatusCode(), result);

            if (result != null && "success".equalsIgnoreCase(result.getStatus())) {
                log.info(
                        "Stream registered successfully with AI service: stream_id={}, status={}, playbackUrl={}",
                        request.getStreamId(),
                        result.getStatus(),
                        result.getPlaybackUrl()
                );
                return result.getPlaybackUrl();
            }

            log.error("Failed to register stream with AI service: {}", result != null ? result.getMessage() : "Unknown error");
            return "";

        } catch (Exception e) {
            log.error("Error calling AI service for stream registration", e);
            return "";
        }
    }

    @Override
    public AiServiceStreamResponse unregisterStream(String streamId) {
        try {
            String url = aiServiceBaseUrl + streamEndpoint + "/" + streamId + "/delete";

            log.info("Unregistering stream from AI service: url={}, stream_id={}", url, streamId);

            ResponseEntity<AiServiceStreamResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    null,
                    AiServiceStreamResponse.class
            );

            AiServiceStreamResponse result = response.getBody();
            log.info("Stream unregistered from AI service: stream_id={}, response={}", streamId, result);

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

            log.info("Getting stream info from AI service: url={}, stream_id={}", url, streamId);

            ResponseEntity<AiServiceStreamResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    AiServiceStreamResponse.class
            );

            AiServiceStreamResponse result = response.getBody();
            log.info("Stream info retrieved: stream_id={}, response={}", streamId, result);

            return result;

        } catch (Exception e) {
            log.error("Error getting stream info from AI service: stream_id={}", streamId, e);
            return null;
        }
    }

    @Override
    public String getStreamInfoRaw(String streamId) {
        try {
            String url = aiServiceBaseUrl + streamEndpoint + "/" + streamId;

            log.info("Getting raw stream info from AI service: url={}, stream_id={}", url, streamId);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    String.class
            );

            log.info("Raw stream info retrieved: stream_id={}, body={}", streamId, response.getBody());
            return response.getBody();

        } catch (Exception e) {
            log.error("Error getting raw stream info from AI service: stream_id={}", streamId, e);
            return null;
        }
    }
}