package com.dji.sample.service.impl;

import com.dji.sample.dto.request.StartStreamRequest;
import com.dji.sample.dto.request.StopStreamRequest;
import com.dji.sample.dto.response.StartStreamResponse;
import com.dji.sample.dto.response.StreamInfoResponse;
import com.dji.sample.dto.response.StreamStatusResponse;
import com.dji.sample.entity.Device;
import com.dji.sample.entity.LiveStreamSession;
import com.dji.sample.repository.DeviceRepository;
import com.dji.sample.repository.LiveStreamSessionRepository;
import com.dji.sample.security.CustomUserDetails;
import com.dji.sample.service.LiveStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.dji.sample.service.IDeviceRedisService;
import com.dji.sample.dto.request.AiServiceStreamRequest;
import com.dji.sample.service.IAiServiceClient;
import org.springframework.beans.factory.annotation.Value;
import com.dji.sample.dto.request.CreateHistoryRequest;
import com.dji.sample.service.HistoryService;
import com.dji.sample.robot.service.IRobotCommandService;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LiveStreamServiceImpl implements LiveStreamService {

    private final LiveStreamSessionRepository liveStreamSessionRepository;
    private final DeviceRepository deviceRepository;
    private final IDeviceRedisService deviceRedisService;
    private final HistoryService historyService;
   
        private final IAiServiceClient aiServiceClient;
        private final IRobotCommandService robotCommandService;

        @Value("${ai-service.rtmp-url}")
        private String rtmpBaseUrl;
    
       @Override
        public StartStreamResponse startStream(StartStreamRequest request) {

        boolean activeDeviceSessionExists =
                liveStreamSessionRepository.existsByDeviceSnAndSessionStatus(
                        request.getDeviceSn(),
                        "ACTIVE"
                );

        if (activeDeviceSessionExists) {
                throw new RuntimeException("Device already has an active stream");
        }

        if (request.getMissionId() != null) {
                boolean activeMissionSessionExists =
                        liveStreamSessionRepository.existsByMissionIdAndSessionStatus(
                                request.getMissionId(),
                                "ACTIVE"
                        );

                if (activeMissionSessionExists) {
                throw new RuntimeException("Mission already has an active stream");
                }
        }

        Device device = deviceRepository.findByDeviceSnAndDeletedAtIsNull(request.getDeviceSn())
                .orElseThrow(() -> new RuntimeException("Device not found"));

        LiveStreamSession session = new LiveStreamSession();

        session.setDeviceSn(request.getDeviceSn());
       UUID currentUserId = getCurrentUserId();
        session.setUserId(currentUserId);
        session.setUserId(currentUserId);
        session.setSessionStatus("ACTIVE");
        session.setQuality(
                request.getVideoQuality() != null
                        ? String.valueOf(request.getVideoQuality())
                        : "HIGH"
        );
        session.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC));
        session.setLastHeartbeatAt(OffsetDateTime.now(ZoneOffset.UTC));
        session.setMissionId(request.getMissionId());
        session.setVideoId(
                request.getVideoId() != null
                        ? request.getVideoId().toString()
                        : null
        );

        // String streamObjectKey =
        //         "streams/1581F7FVC25A700DF473/2026-04-02_11-59-11/index.m3u8";

        // String playbackUrl =
        //         s3PresignService.createStreamDownloadUrl(streamObjectKey);


        String rtmpUrl = rtmpBaseUrl + "/streams/" + request.getDeviceSn();
        String vectorMapUrl = rtmpBaseUrl + "/streams/" + request.getDeviceSn() + "-vector";


        if ("Robot".equalsIgnoreCase(device.getDeviceType())) {
                String jobId = UUID.randomUUID().toString();

                Map<String, Object> parameters = new HashMap<>();
                parameters.put("camera_stream", rtmpUrl);
                parameters.put("vector_map_stream", vectorMapUrl);
                parameters.put("patrol_route", "TestforGO2");

                Map<String, Object> robotJobPayload = new HashMap<>();
                robotJobPayload.put("job_type", "robopilot_test/patrol_test");
                robotJobPayload.put("parameters", parameters);
                robotCommandService.createJob(
                        request.getDeviceSn(),
                        UUID.randomUUID().toString(),
                        jobId,
                        robotJobPayload
                );
                }

        // UUID currentUserId = getCurrentUserId();

        AiServiceStreamRequest aiRequest = AiServiceStreamRequest.builder()
                .uri(rtmpUrl)
                .vectorMapUri(vectorMapUrl)
                .streamId(request.getDeviceSn())

                .deviceId(device.getDeviceId())
                .deviceName(device.getDeviceName() != null ? device.getDeviceName() : "")

                .companyId(device.getCompany() != null ? device.getCompany().getCompanyId() : null)
                .companyName(device.getCompany() != null && device.getCompany().getName() != null
                        ? device.getCompany().getName()
                        : "")

                .siteId(device.getSite() != null ? device.getSite().getSiteId() : null)
                .siteName(device.getSite() != null && device.getSite().getName() != null
                        ? device.getSite().getName()
                        : "")

                .missionId(request.getMissionId())
                .missionName(request.getMissionId() != null ? request.getMissionId().toString() : "")

                .userId(currentUserId)
                .userName("admin")
                .emails(List.of())

                .sessionStartTime(session.getStartedAt())
                .build();

        String playbackUrl = aiServiceClient.registerStream(aiRequest);

        if (playbackUrl == null || playbackUrl.isBlank()) {
        throw new RuntimeException("AI service stream registration failed");
        }

        session.setPlaybackUrl(playbackUrl);

        // Keep original S3 presigned URL in DB
        session.setPlaybackUrl(playbackUrl);

        LiveStreamSession saved = liveStreamSessionRepository.save(session);

        String hlsUrl = buildBackendHlsUrl(saved.getId());

        device.setMissionId(request.getMissionId());
        deviceRepository.save(device);
        
        if (request.getMissionId() != null) {
                deviceRedisService.setRobotJobState(
                        request.getDeviceSn(),
                        "stream-" + saved.getId(),
                        "WORKING",
                        request.getMissionId().toString()
                );
                }

        return StartStreamResponse.builder()
                .sessionId(saved.getId())
                .streamId(saved.getId())
                .id(saved.getId())
                .playbackUrl(hlsUrl)
                .sessionStatus(saved.getSessionStatus())
                .status(saved.getSessionStatus())

                // reference compatibility
                .viewerCount(1)
                .startTime(saved.getStartedAt())
                .canStop(true)
                .isSendHeartBeat(true)

                .build();
        }
       
        @Override
        public StreamInfoResponse stopStream(StopStreamRequest request) {

        LiveStreamSession session =
                liveStreamSessionRepository
                        .findFirstByDeviceSnAndSessionStatusOrderByStartedAtDesc(
                                request.getDeviceSn(),
                                "ACTIVE"
                        )
                        .orElseThrow(() ->
                                new RuntimeException("Active stream not found")
                        );

        try {
        aiServiceClient.unregisterStream(request.getDeviceSn());
        } catch (Exception e) {
        throw new RuntimeException("AI service stream unregister failed: " + e.getMessage(), e);
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        session.setSessionStatus("STOPPED");
        session.setStoppedAt(now);

        LiveStreamSession saved = liveStreamSessionRepository.save(session);

        Device device = deviceRepository.findByDeviceSnAndDeletedAtIsNull(request.getDeviceSn())
                .orElseThrow(() -> new RuntimeException("Device not found"));

        
        device.setMissionId(null);
        deviceRepository.save(device);

        // clear stream/job running state from Redis after stop
        deviceRedisService.clearRobotJobState(request.getDeviceSn());

        // also clear old Redis status key if used by dashboard/device status
        deviceRedisService.clearDeviceStatus(request.getDeviceSn());
        CreateHistoryRequest historyRequest = new CreateHistoryRequest();
        historyRequest.setDeviceSn(saved.getDeviceSn());
        historyRequest.setPlaybackUrl(saved.getPlaybackUrl());
        historyRequest.setMissionId(saved.getMissionId());

        historyService.createHistory(historyRequest);

        return mapToResponse(saved);
        }
       
        @Override
        public StreamInfoResponse getStreamInfo(String streamId) {

        LiveStreamSession session;

        try {
                UUID sessionId = UUID.fromString(streamId);

                session = liveStreamSessionRepository
                        .findById(sessionId)
                        .orElseThrow(() ->
                                new RuntimeException("Stream not found")
                        );

        } catch (IllegalArgumentException e) {

                session = liveStreamSessionRepository
                        .findFirstByDeviceSnAndSessionStatusOrderByStartedAtDesc(
                                streamId,
                                "ACTIVE"
                        )
                        .orElseThrow(() ->
                                new RuntimeException("Active stream not found for device")
                        );
        }

        return mapToResponse(session);
        }

        @Override
        public StreamInfoResponse heartbeat(UUID sessionId) {

        LiveStreamSession session =
        liveStreamSessionRepository
        .findByIdAndSessionStatus(sessionId, "ACTIVE")
        .orElseThrow(() ->
        new RuntimeException("Active stream not found")
        );

        session.setLastHeartbeatAt(OffsetDateTime.now(ZoneOffset.UTC));

        LiveStreamSession saved = liveStreamSessionRepository.save(session);

        return mapToResponse(saved);
        }
    
    @Override
        public StreamStatusResponse getStreamStatus(String deviceSn) {

        var activeSession =
                liveStreamSessionRepository
                        .findFirstByDeviceSnAndSessionStatusOrderByStartedAtDesc(
                                deviceSn,
                                "ACTIVE"
                        );

        boolean active = activeSession.isPresent();

        return StreamStatusResponse.builder()
                .active(active)
                .streaming(active)
                .deviceSn(deviceSn)
                .sessionStatus(active ? "ACTIVE" : "STOPPED")
                .missionId(
                        activeSession
                                .map(LiveStreamSession::getMissionId)
                                .orElse(null)
                )
                .build();
        }

    
    
    private UUID getCurrentUserId() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Authenticated user not found");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUserId();
        }

        throw new RuntimeException("Invalid authenticated user principal");
    }

    
    private String buildBackendHlsUrl(UUID sessionId) {
        return "http://localhost:6789/api/v1/live/hls/" + sessionId + "/index.m3u8";
        }

    private StreamInfoResponse mapToResponse(LiveStreamSession session) {
        String hlsUrl = buildBackendHlsUrl(session.getId());

        return StreamInfoResponse.builder()
                .id(session.getId())
                .deviceSn(session.getDeviceSn())
                .userId(session.getUserId())
                .sessionStatus(session.getSessionStatus())
                .state(
                        "ACTIVE".equals(session.getSessionStatus())
                                ? "RUNNING"
                                : session.getSessionStatus()
                )

                .quality(session.getQuality())
                .startedAt(session.getStartedAt())
                .lastHeartbeatAt(session.getLastHeartbeatAt())
                .stoppedAt(session.getStoppedAt())

                .playbackUrl(hlsUrl)
                .playback_url(hlsUrl)

                .mapUrl(hlsUrl)
                .map_url(hlsUrl)

                .url(hlsUrl)
                .streamUrl(hlsUrl)
                .liveUrl(hlsUrl)
                .cameraUrl(hlsUrl)

                .missionId(session.getMissionId())
                .videoId(session.getVideoId())
                .build();
        }
}