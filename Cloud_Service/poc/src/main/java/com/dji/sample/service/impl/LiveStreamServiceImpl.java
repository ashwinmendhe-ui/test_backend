package com.dji.sample.service.impl;

import com.dji.sample.drone.service.DjiLivestreamService;
import com.dji.sample.dto.request.AiServiceStreamRequest;
import com.dji.sample.dto.request.CreateHistoryRequest;
import com.dji.sample.dto.request.StartStreamRequest;
import com.dji.sample.dto.request.StopStreamRequest;
import com.dji.sample.dto.response.HistoryDetailResponse;
import com.dji.sample.dto.response.StartStreamResponse;
import com.dji.sample.dto.response.StreamInfoResponse;
import com.dji.sample.dto.response.StreamStatusResponse;
import com.dji.sample.entity.Device;
import com.dji.sample.entity.LiveStreamSession;
import com.dji.sample.entity.SubDevice;
import com.dji.sample.repository.DeviceRepository;
import com.dji.sample.repository.LiveStreamSessionRepository;
import com.dji.sample.repository.SubDeviceRepository;
import com.dji.sample.robot.service.IRobotCommandService;
import com.dji.sample.security.CustomUserDetails;
import com.dji.sample.service.DeviceWebSocketPublisher;
import com.dji.sample.service.HistoryService;
import com.dji.sample.service.IAiServiceClient;
import com.dji.sample.service.IDeviceRedisService;
import com.dji.sample.service.LiveStreamService;
import com.dji.sample.service.SlackNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveStreamServiceImpl implements LiveStreamService {

    private final LiveStreamSessionRepository liveStreamSessionRepository;
    private final DeviceRepository deviceRepository;
    private final IDeviceRedisService deviceRedisService;
    private final HistoryService historyService;
    private final IAiServiceClient aiServiceClient;
    private final IRobotCommandService robotCommandService;
    private final DjiLivestreamService djiLivestreamService;
    private final DeviceWebSocketPublisher webSocketPublisher;
    private final SlackNotificationService slackNotificationService;
    private final SubDeviceRepository subDeviceRepository;

    @Value("${ai-service.rtmp-url}")
    private String rtmpBaseUrl;

    @Value("${app.public-base-url:http://localhost:6789}")
    private String publicBaseUrl;

    @Override
    public StartStreamResponse startStream(StartStreamRequest request) {

        Device device = deviceRepository.findByDeviceSnAndDeletedAtIsNull(request.getDeviceSn())
                .orElseThrow(() -> new RuntimeException("Device not found"));

        StreamSource streamSource = resolveStreamSource(request, device);

        boolean activeDeviceSessionExists =
                liveStreamSessionRepository.existsByDeviceSnAndSessionStatus(
                        streamSource.streamDeviceSn(),
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

        UUID currentUserId = getCurrentUserId();

        String requestDeviceSn = request.getDeviceSn();
        String streamId = streamSource.streamDeviceSn();
        String videoId = streamSource.videoId();
        String gatewaySn = streamSource.gatewaySn();
        String payloadIndex = streamSource.payloadIndex();
        String djiVideoType = streamSource.videoType();

        String rtmpUrl = rtmpBaseUrl + "/streams/" + streamId;
        String vectorMapUrl = rtmpBaseUrl + "/streams/" + streamId + "-vector";

        log.info("Start stream deviceType={}, requestDeviceSn={}, streamId={}, videoId={}, rtmpUrl={}",
                device.getDeviceType(), requestDeviceSn, streamId, videoId, rtmpUrl);

        LiveStreamSession session = new LiveStreamSession();
        session.setDeviceSn(streamId);
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
        session.setVideoId(videoId);

        if (isDrone(device)) {
            log.info("[DJI] Calling live_start_push. gatewaySn={}, droneSn={}, payloadIndex={}, videoType={}, rtmpUrl={}",
                    gatewaySn, streamId, payloadIndex, djiVideoType, rtmpUrl);

            djiLivestreamService.startPush(
                    gatewaySn,
                    streamId,
                    payloadIndex,
                    djiVideoType,
                    request.getVideoQuality() != null ? request.getVideoQuality() : 0,
                    rtmpUrl
            );
        }

        if (isRobot(device)) {
            String jobId = UUID.randomUUID().toString();

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("camera_stream", rtmpUrl);
            parameters.put("vector_map_stream", vectorMapUrl);
            parameters.put("patrol_route", "TestforGO2");

            Map<String, Object> robotJobPayload = new HashMap<>();
            robotJobPayload.put("job_type", "robopilot_test/patrol_test");
            robotJobPayload.put("parameters", parameters);

            robotCommandService.createJob(
                    requestDeviceSn,
                    UUID.randomUUID().toString(),
                    jobId,
                    robotJobPayload
            );
        }

        AiServiceStreamRequest aiRequest = AiServiceStreamRequest.builder()
                .uri(rtmpUrl)
                .vectorMapUri(vectorMapUrl)
                .streamId(streamId)
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

        LiveStreamSession saved = liveStreamSessionRepository.save(session);

        String hlsUrl = buildBackendHlsUrl(saved.getId());

        device.setMissionId(request.getMissionId());
        deviceRepository.save(device);

        if (request.getMissionId() != null) {
            deviceRedisService.setRobotJobState(
                    requestDeviceSn,
                    "stream-" + saved.getId(),
                    "WORKING",
                    request.getMissionId().toString()
            );
        }

        webSocketPublisher.publishDashboardStatus(
                requestDeviceSn,
                "WORKING",
                "stream-start"
        );

        return StartStreamResponse.builder()
                .sessionId(saved.getId())
                .streamId(saved.getId())
                .id(saved.getId())
                .playbackUrl(hlsUrl)
                .sessionStatus(saved.getSessionStatus())
                .status(saved.getSessionStatus())
                .viewerCount(1)
                .startTime(saved.getStartedAt())
                .canStop(true)
                .isSendHeartBeat(true)
                .build();
    }

    @Override
    public StreamInfoResponse stopStream(StopStreamRequest request) {

        StreamSource streamSource = resolveStreamSourceForDeviceSn(request.getDeviceSn());

        LiveStreamSession session =
                liveStreamSessionRepository
                        .findFirstByDeviceSnAndSessionStatusOrderByStartedAtDesc(
                                streamSource.streamDeviceSn(),
                                "ACTIVE"
                        )
                        .orElseThrow(() ->
                                new RuntimeException("Active stream not found")
                        );

        try {
            aiServiceClient.unregisterStream(streamSource.streamDeviceSn());
        } catch (Exception e) {
            throw new RuntimeException("AI service stream unregister failed: " + e.getMessage(), e);
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        session.setSessionStatus("STOPPED");
        session.setStoppedAt(now);

        LiveStreamSession saved = liveStreamSessionRepository.save(session);

        try {
            CreateHistoryRequest historyRequest = new CreateHistoryRequest();
            historyRequest.setDeviceSn(request.getDeviceSn());
            historyRequest.setMissionId(saved.getMissionId());
            historyRequest.setPlaybackUrl(saved.getPlaybackUrl());
            historyRequest.setSessionId(saved.getId());

            HistoryDetailResponse report = historyService.createHistory(historyRequest);
            slackNotificationService.notifyAiDetectionReport(report);

        } catch (Exception e) {
            log.warn(
                    "[History/Slack] Failed to create history or send report. deviceSn={}, error={}",
                    request.getDeviceSn(),
                    e.getMessage(),
                    e
            );
        }

        Device device = deviceRepository.findByDeviceSnAndDeletedAtIsNull(request.getDeviceSn())
                .orElseThrow(() -> new RuntimeException("Device not found"));

        device.setMissionId(null);
        deviceRepository.save(device);

        deviceRedisService.clearRobotJobState(request.getDeviceSn());
        deviceRedisService.clearDeviceStatus(request.getDeviceSn());

        webSocketPublisher.publishDashboardRefresh(
                request.getDeviceSn(),
                "stream-stop"
        );

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

            StreamSource streamSource = resolveStreamSourceForDeviceSn(streamId);

            session = liveStreamSessionRepository
                    .findFirstByDeviceSnAndSessionStatusOrderByStartedAtDesc(
                            streamSource.streamDeviceSn(),
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

        StreamSource streamSource = resolveStreamSourceForDeviceSn(deviceSn);

        var activeSession =
                liveStreamSessionRepository
                        .findFirstByDeviceSnAndSessionStatusOrderByStartedAtDesc(
                                streamSource.streamDeviceSn(),
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
        return publicBaseUrl + "/api/v1/live/hls/" + sessionId + "/index.m3u8";    }

    private StreamSource resolveStreamSource(StartStreamRequest request, Device device) {
        String requestDeviceSn = request.getDeviceSn();

        if (!isDrone(device)) {
            return new StreamSource(
                    requestDeviceSn,
                    requestDeviceSn,
                    requestDeviceSn,
                    requestDeviceSn,
                    "99-0-0",
                    "normal",
                    requestDeviceSn
            );
        }

        

        SubDevice subDevice = subDeviceRepository
                .findFirstByDeviceSnAndDeletedAtIsNullOrderByIdAsc(requestDeviceSn)
                .orElse(null);

        log.info("[LIVE][SUB_DEVICE] requestDeviceSn={}, found={}, subSn={}",
                requestDeviceSn,
                subDevice != null,
                subDevice != null ? subDevice.getSn() : null);

        String streamDeviceSn = requestDeviceSn;
        String gatewaySn = requestDeviceSn;
        String payloadIndex = resolvePayloadIndexFromRequest(request);
        String videoType = resolveDjiVideoTypeFromRequest(request);

        if (subDevice != null && subDevice.getSn() != null && !subDevice.getSn().isBlank()) {
            streamDeviceSn = subDevice.getSn();

            Integer type = subDevice.getType() != null ? subDevice.getType() : 99;
            Integer subType = subDevice.getSubType() != null ? subDevice.getSubType() : 0;

            payloadIndex = type + "-" + subType + "-0";
        } else if (request.getVideoId() != null
                && request.getVideoId().getDroneSn() != null
                && !request.getVideoId().getDroneSn().isBlank()) {
            streamDeviceSn = request.getVideoId().getDroneSn();
        }

        String videoId = streamDeviceSn + "/" + payloadIndex + "/" + videoType + "-0";

        log.info("[LIVE][STREAM_SOURCE] requestDeviceSn={}, streamDeviceSn={}, gatewaySn={}, payloadIndex={}, videoType={}, videoId={}",
                requestDeviceSn, streamDeviceSn, gatewaySn, payloadIndex, videoType, videoId);

        return new StreamSource(
                requestDeviceSn,
                streamDeviceSn,
                gatewaySn,
                streamDeviceSn,
                payloadIndex,
                videoType,
                videoId
        );
    }

    private StreamSource resolveStreamSourceForDeviceSn(String deviceSn) {
        SubDevice subDevice = subDeviceRepository
                .findFirstByDeviceSnAndDeletedAtIsNullOrderByIdAsc(deviceSn)
                .orElse(null);

        if (subDevice != null && subDevice.getSn() != null && !subDevice.getSn().isBlank()) {
            Integer type = subDevice.getType() != null ? subDevice.getType() : 99;
            Integer subType = subDevice.getSubType() != null ? subDevice.getSubType() : 0;
            String payloadIndex = type + "-" + subType + "-0";
            String videoType = "normal";
            String videoId = subDevice.getSn() + "/" + payloadIndex + "/" + videoType + "-0";

            return new StreamSource(
                    deviceSn,
                    subDevice.getSn(),
                    deviceSn,
                    subDevice.getSn(),
                    payloadIndex,
                    videoType,
                    videoId
            );
        }

        return new StreamSource(
                deviceSn,
                deviceSn,
                deviceSn,
                deviceSn,
                "99-0-0",
                "normal",
                deviceSn
        );
    }

    private String resolvePayloadIndexFromRequest(StartStreamRequest request) {
        if (request.getVideoId() != null && request.getVideoId().getPayloadIndex() != null) {
            return request.getVideoId().getPayloadIndex().getType()
                    + "-"
                    + request.getVideoId().getPayloadIndex().getSubType()
                    + "-"
                    + request.getVideoId().getPayloadIndex().getPosition();
        }

        return "99-0-0";
    }

    private String resolveDjiVideoTypeFromRequest(StartStreamRequest request) {
        if (request.getVideoId() != null
                && request.getVideoId().getVideoType() != null
                && !request.getVideoId().getVideoType().isBlank()) {
            return request.getVideoId().getVideoType();
        }

        return "normal";
    }

    private record StreamSource(
            String requestDeviceSn,
            String streamDeviceSn,
            String gatewaySn,
            String droneSn,
            String payloadIndex,
            String videoType,
            String videoId
    ) {}

    private boolean isDrone(Device device) {
        String type = device.getDeviceType();
        return "Drone".equalsIgnoreCase(type)
                || "드론".equals(type);
        }

        private boolean isRobot(Device device) {
        String type = device.getDeviceType();
        return "Robot".equalsIgnoreCase(type)
                || "4족보행 로봇".equals(type)
                || "로봇".equals(type);
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
                .mapUrl(null)
                .map_url(null)
                .url(hlsUrl)
                .streamUrl(hlsUrl)
                .liveUrl(hlsUrl)
                .cameraUrl(hlsUrl)
                .missionId(session.getMissionId())
                .videoId(session.getVideoId())
                .build();
    }
}