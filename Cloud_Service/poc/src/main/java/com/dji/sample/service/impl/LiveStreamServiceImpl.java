package com.dji.sample.service.impl;

import com.dji.sample.drone.service.DjiLivestreamService;
import com.dji.sample.dto.request.AiServiceStreamRequest;
import com.dji.sample.dto.request.StartStreamRequest;
import com.dji.sample.dto.request.StopStreamRequest;
import com.dji.sample.dto.response.StartStreamResponse;
import com.dji.sample.dto.response.StreamInfoResponse;
import com.dji.sample.dto.response.StreamStatusResponse;
import com.dji.sample.entity.Device;
import com.dji.sample.entity.LiveStreamSession;
import com.dji.sample.repository.DeviceRepository;
import com.dji.sample.repository.LiveStreamSessionRepository;
import com.dji.sample.robot.service.IRobotCommandService;
import com.dji.sample.security.CustomUserDetails;
import com.dji.sample.service.DeviceWebSocketPublisher;
import com.dji.sample.service.IAiServiceClient;
import com.dji.sample.service.IDeviceRedisService;
import com.dji.sample.service.LiveStreamService;
import com.dji.sample.service.StreamCleanupService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.dji.sample.drone.model.StreamSource;
import com.dji.sample.drone.service.StreamSourceResolver;
import com.dji.sample.entity.Mission;
import com.dji.sample.repository.MissionRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveStreamServiceImpl implements LiveStreamService {

    private final LiveStreamSessionRepository liveStreamSessionRepository;
    private final DeviceRepository deviceRepository;
    private final IDeviceRedisService deviceRedisService;
    private final IAiServiceClient aiServiceClient;
    private final IRobotCommandService robotCommandService;
    private final DjiLivestreamService djiLivestreamService;
    private final DeviceWebSocketPublisher webSocketPublisher;
    private final StreamCleanupService streamCleanupService;
    private final StreamSourceResolver streamSourceResolver;
    private final MissionRepository missionRepository;

    @Value("${ai-service.rtmp-url}")
    private String rtmpBaseUrl;

    @Value("${app.public-base-url:http://localhost:6789}")
    private String publicBaseUrl;

   @Override
public StartStreamResponse startStream(StartStreamRequest request) {

    Device device =
            deviceRepository
                    .findByDeviceSnAndDeletedAtIsNull(request.getDeviceSn())
                    .orElseThrow(() ->
                            new RuntimeException("Device not found")
                    );
    String missionName = "";

        if (request.getMissionId() != null) {
        Mission mission =
                missionRepository
                        .findByMissionIdAndDeletedAtIsNull(request.getMissionId())
                        .orElseThrow(() ->
                                new RuntimeException("Mission not found")
                        );

        missionName = mission.getMissionName();
        }
    StreamSource streamSource =
        streamSourceResolver.resolve(request, device);

    String requestDeviceSn = request.getDeviceSn();
    String physicalStreamSn = streamSource.streamDeviceSn();
    String aiStreamId = buildAiStreamId(physicalStreamSn);


    UUID currentUserId = getCurrentUserId();

/*
 * If this physical stream is already ACTIVE, do not start the
 * robot/drone/AI pipeline again.
 *
 * Reuse the existing ACTIVE session so another tab/user can
 * monitor the currently running mission.
 */
var activeSession =
        liveStreamSessionRepository
                .findFirstByDeviceSnAndSessionStatusOrderByStartedAtDesc(
                        physicalStreamSn,
                        "ACTIVE"
                );

if (activeSession.isPresent()) {

    LiveStreamSession existing =
            activeSession.get();

    /*
     * A viewer must join the mission that is currently running.
     */
    if (request.getMissionId() != null &&
            existing.getMissionId() != null &&
            !request.getMissionId().equals(existing.getMissionId())) {

        throw new RuntimeException(
                "Device already has an active stream for another mission"
        );
    }

    boolean isStarter =
            currentUserId.equals(existing.getUserId());

    String existingHlsUrl =
            buildBackendHlsUrl(existing.getId());

    log.info(
            "[StreamViewer] Joining existing active stream. deviceSn={}, sessionId={}, starterUserId={}, currentUserId={}, isStarter={}",
            physicalStreamSn,
            existing.getId(),
            existing.getUserId(),
            currentUserId,
            isStarter
    );

    return StartStreamResponse.builder()
            .sessionId(existing.getId())
            .streamId(existing.getId())
            .id(existing.getId())
            .playbackUrl(existingHlsUrl)
            .sessionStatus(existing.getSessionStatus())
            .status(existing.getSessionStatus())
            .viewerCount(1)
            .startTime(existing.getStartedAt())

            /*
             * Same user in another tab may still stop their stream.
             * A different user is observer-only.
             */
            .canStop(isStarter)

            /*
             * Only the original starter maintains heartbeat.
             * An observer must not own/extend another user's session.
             */
            .isSendHeartBeat(isStarter)
            .joinedExisting(true)
            .build();
}

/*
 * No ACTIVE stream exists.
 * Keep the existing protection against the same mission being used
 * by another physical device/session.
 */
if (request.getMissionId() != null) {

    boolean activeMissionSessionExists =
            liveStreamSessionRepository
                    .existsByMissionIdAndSessionStatus(
                            request.getMissionId(),
                            "ACTIVE"
                    );

    if (activeMissionSessionExists) {
        throw new RuntimeException(
                "Mission already has an active stream"
        );
    }
}
    String videoId = streamSource.videoId();
    String gatewaySn = streamSource.gatewaySn();
    String payloadIndex = streamSource.payloadIndex();
    String djiVideoType = streamSource.videoType();

    String rtmpUrl =
            rtmpBaseUrl + "/streams/" + physicalStreamSn;

    String vectorMapUrl =
            rtmpBaseUrl + "/streams/" +
                    physicalStreamSn +
                    "-vector";

    log.info(
            "Start stream deviceType={}, requestDeviceSn={}, physicalStreamSn={}, aiStreamId={}, videoId={}, rtmpUrl={}",
            device.getDeviceType(),
            requestDeviceSn,
            physicalStreamSn,
            aiStreamId,
            videoId,
            rtmpUrl
    );

    /*
     * Prepare the database session.
     * It is saved only after AI registration succeeds.
     */
    LiveStreamSession session = new LiveStreamSession();

    session.setDeviceSn(physicalStreamSn);
    session.setUserId(currentUserId);
    session.setSessionStatus("ACTIVE");

    session.setQuality(
            request.getVideoQuality() != null
                    ? String.valueOf(request.getVideoQuality())
                    : "HIGH"
    );

    session.setStartedAt(
            OffsetDateTime.now(ZoneOffset.UTC)
    );

    session.setLastHeartbeatAt(
            OffsetDateTime.now(ZoneOffset.UTC)
    );

    session.setMissionId(request.getMissionId());
    session.setVideoId(videoId);

    /*
     * DJI start path.
     */
    if (isDrone(device)) {

        log.info(
                "[DJI] Calling live_start_push. gatewaySn={}, droneSn={}, payloadIndex={}, videoType={}, rtmpUrl={}",
                gatewaySn,
                physicalStreamSn,
                payloadIndex,
                djiVideoType,
                rtmpUrl
        );

        djiLivestreamService.startPush(
                gatewaySn,
                physicalStreamSn,
                payloadIndex,
                djiVideoType,
                request.getVideoQuality() != null
                        ? request.getVideoQuality()
                        : 0,
                rtmpUrl
        );
    }

    /*
     * GO2 robot start path.
     *
     * Store the real physical robot jobId before publishing create_job.
     * Do not overwrite this jobId later with a livestream session ID.
     */
    if (isRobot(device)) {

        String robotJobId =
                UUID.randomUUID().toString();

        String robotCommandId =
                UUID.randomUUID().toString();

        deviceRedisService.setRobotJobState(
                requestDeviceSn,
                robotJobId,
                "STARTING",
                request.getMissionId() != null
                        ? request.getMissionId().toString()
                        : null
        );

        Map<String, Object> parameters =
                new HashMap<>();

        parameters.put(
                "camera_stream",
                rtmpUrl
        );

        parameters.put(
                "vector_map_stream",
                vectorMapUrl
        );

        parameters.put(
                "patrol_route",
                missionName
        );

        Map<String, Object> robotJobPayload =
                new HashMap<>();

        robotJobPayload.put(
                "job_type",
                "robopilot_test/patrol_test"
        );

        robotJobPayload.put(
                "parameters",
                parameters
        );

        try {
            robotCommandService.createJob(
                    requestDeviceSn,
                    robotCommandId,
                    robotJobId,
                    robotJobPayload
            );

            log.info(
                    "Robot create_job sent. deviceSn={}, commandId={}, jobId={}",
                    requestDeviceSn,
                    robotCommandId,
                    robotJobId
            );

        } catch (Exception e) {

            deviceRedisService.clearRobotJobState(
                    requestDeviceSn
            );

            throw new RuntimeException(
                    "Failed to start robot job: " +
                            e.getMessage(),
                    e
            );
        }
    }

    /*
     * Register the incoming RTMP stream with the AI service.
     */
    AiServiceStreamRequest aiRequest =
            AiServiceStreamRequest.builder()
                    .uri(rtmpUrl)
                    .vectorMapUri(vectorMapUrl)
                    .streamId(aiStreamId)
                    .deviceId(device.getDeviceId())
                    .deviceName(
                            device.getDeviceName() != null
                                    ? device.getDeviceName()
                                    : ""
                    )
                    .companyId(
                            device.getCompany() != null
                                    ? device.getCompany().getCompanyId()
                                    : null
                    )
                    .companyName(
                            device.getCompany() != null &&
                                    device.getCompany().getName() != null
                                    ? device.getCompany().getName()
                                    : ""
                    )
                    .siteId(
                            device.getSite() != null
                                    ? device.getSite().getSiteId()
                                    : null
                    )
                    .siteName(
                            device.getSite() != null &&
                                    device.getSite().getName() != null
                                    ? device.getSite().getName()
                                    : ""
                    )
                    .missionId(request.getMissionId())
                    .missionName(missionName)
                    .userId(currentUserId)
                    .userName("admin")
                    .emails(List.of())
                    .sessionStartTime(session.getStartedAt())
                    .build();

    String playbackUrl;

    try {
        playbackUrl =
                aiServiceClient.registerStream(aiRequest);

    } catch (Exception e) {

        /*
         * Roll back the robot job when AI registration fails.
         */
        if (isRobot(device)) {
            try {
                String robotJobId =
                        deviceRedisService.getRobotJobId(
                                requestDeviceSn
                        );

                if (robotJobId != null &&
                        !robotJobId.isBlank()) {

                    robotCommandService.cancelJob(
                            requestDeviceSn,
                            UUID.randomUUID().toString(),
                            robotJobId
                    );

                } else {
                    robotCommandService.cleanJob(
                            requestDeviceSn
                    );
                }

            } catch (Exception cleanupException) {
                log.warn(
                        "Robot rollback failed after AI registration error. deviceSn={}, error={}",
                        requestDeviceSn,
                        cleanupException.getMessage(),
                        cleanupException
                );
            }

            deviceRedisService.clearRobotJobState(
                    requestDeviceSn
            );
        }

        throw new RuntimeException(
                "AI service stream registration failed: " +
                        e.getMessage(),
                e
        );
    }

    if (playbackUrl == null ||
            playbackUrl.isBlank()) {

        if (isRobot(device)) {
            try {
                String robotJobId =
                        deviceRedisService.getRobotJobId(
                                requestDeviceSn
                        );

                if (robotJobId != null &&
                        !robotJobId.isBlank()) {

                    robotCommandService.cancelJob(
                            requestDeviceSn,
                            UUID.randomUUID().toString(),
                            robotJobId
                    );

                } else {
                    robotCommandService.cleanJob(
                            requestDeviceSn
                    );
                }

            } catch (Exception cleanupException) {
                log.warn(
                        "Robot rollback failed after blank AI playback URL. deviceSn={}, error={}",
                        requestDeviceSn,
                        cleanupException.getMessage(),
                        cleanupException
                );
            }

            deviceRedisService.clearRobotJobState(
                    requestDeviceSn
            );
        }

        throw new RuntimeException(
                "AI service stream registration failed"
        );
    }

    session.setPlaybackUrl(playbackUrl);

    LiveStreamSession saved =
            liveStreamSessionRepository.save(session);

    String hlsUrl =
            buildBackendHlsUrl(saved.getId());

    /*
     * Keep the Device table mission for compatibility.
     * Dashboard response should still expose it only when an ACTIVE
     * livestream session exists.
     */
    device.setMissionId(request.getMissionId());
    deviceRepository.save(device);

    /*
     * Important:
     * Do not call setRobotJobState() here using
     * "stream-" + saved.getId().
     *
     * That would overwrite the real physical robot jobId.
     * RobotJobStateHandler will update the job status from MQTT.
     */

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
        .joinedExisting(false)
        .build();
}
        
    
   @Override
public StreamInfoResponse stopStream(StopStreamRequest request) {

    StreamSource streamSource =
        streamSourceResolver.resolveForDeviceSn(
                request.getDeviceSn()
        );

    String physicalStreamSn =
            streamSource.streamDeviceSn();

    LiveStreamSession stoppedSession =
            streamCleanupService.cleanupStream(
                    request.getDeviceSn(),
                    physicalStreamSn,
                    "MANUAL_STOP",
                    true
            );

    if (stoppedSession != null) {
        return mapToResponse(stoppedSession);
    }

    return StreamInfoResponse.builder()
            .deviceSn(physicalStreamSn)
            .sessionStatus("STOPPED")
            .state("STOPPED")
            .build();
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

            StreamSource streamSource =
                streamSourceResolver.resolveForDeviceSn(streamId);

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

        StreamSource streamSource =
                streamSourceResolver.resolveForDeviceSn(deviceSn);

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

private String buildAiStreamId(String physicalStreamSn) {
    return "robopilot-" + physicalStreamSn;
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