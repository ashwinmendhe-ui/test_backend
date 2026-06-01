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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import com.dji.sample.service.S3PresignService;

@Service
@RequiredArgsConstructor
public class LiveStreamServiceImpl implements LiveStreamService {

    private final LiveStreamSessionRepository liveStreamSessionRepository;
    private final DeviceRepository deviceRepository;
    private final S3PresignService s3PresignService;

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

        Device device = deviceRepository.findByDeviceSn(request.getDeviceSn())
                .orElseThrow(() -> new RuntimeException("Device not found"));

        LiveStreamSession session = new LiveStreamSession();

        session.setDeviceSn(request.getDeviceSn());
        session.setUserId(getCurrentUserId());
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
        String streamObjectKey =
                "streams/1581F7FVC25A700DF473/2026-04-02_11-59-11/index.m3u8";

        String playbackUrl =
                s3PresignService.createStreamDownloadUrl(streamObjectKey);      

        session.setPlaybackUrl(playbackUrl);

        LiveStreamSession saved = liveStreamSessionRepository.save(session);

        device.setStatus("WORKING");
        deviceRepository.save(device);

        return StartStreamResponse.builder()
                .sessionId(saved.getId())
                .streamId(saved.getId())
                .id(saved.getId())
                .playbackUrl(saved.getPlaybackUrl())
                .sessionStatus(saved.getSessionStatus())
                .status(saved.getSessionStatus())
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

        session.setSessionStatus("STOPPED");
        session.setStoppedAt(OffsetDateTime.now(ZoneOffset.UTC));

        LiveStreamSession saved = liveStreamSessionRepository.save(session);

        Device device = deviceRepository.findByDeviceSn(request.getDeviceSn())
                .orElseThrow(() -> new RuntimeException("Device not found"));

        device.setStatus("INACTIVE");
        deviceRepository.save(device);

        return mapToResponse(saved);
    }

    @Override
    public StreamInfoResponse getStreamInfo(UUID streamId) {

        LiveStreamSession session =
                liveStreamSessionRepository
                        .findById(streamId)
                        .orElseThrow(() ->
                                new RuntimeException("Stream not found")
                        );

        return mapToResponse(session);
    }

    @Override
    public StreamInfoResponse heartbeat(UUID sessionId) {

        LiveStreamSession session =
                liveStreamSessionRepository
                        .findById(sessionId)
                        .orElseThrow(() ->
                                new RuntimeException("Stream not found")
                        );

        session.setLastHeartbeatAt(OffsetDateTime.now(ZoneOffset.UTC));

        LiveStreamSession saved = liveStreamSessionRepository.save(session);

        return mapToResponse(saved);
    }

    @Override
    public StreamStatusResponse getStreamStatus(String deviceSn) {

        boolean active =
                liveStreamSessionRepository
                        .existsByDeviceSnAndSessionStatus(
                                deviceSn,
                                "ACTIVE"
                        );

        return StreamStatusResponse.builder()
                .active(active)
                .deviceSn(deviceSn)
                .sessionStatus(active ? "ACTIVE" : "STOPPED")
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

    private StreamInfoResponse mapToResponse(LiveStreamSession session) {
        return StreamInfoResponse.builder()
                .id(session.getId())
                .deviceSn(session.getDeviceSn())
                .userId(session.getUserId())
                .sessionStatus(session.getSessionStatus())
                .quality(session.getQuality())
                .startedAt(session.getStartedAt())
                .lastHeartbeatAt(session.getLastHeartbeatAt())
                .stoppedAt(session.getStoppedAt())

                .playbackUrl(session.getPlaybackUrl())
                .playback_url(session.getPlaybackUrl())

                .mapUrl(session.getPlaybackUrl())
                .map_url(session.getPlaybackUrl())

                .url(session.getPlaybackUrl())
                .streamUrl(session.getPlaybackUrl())
                .liveUrl(session.getPlaybackUrl())
                .cameraUrl(session.getPlaybackUrl())

                .missionId(session.getMissionId())
                .videoId(session.getVideoId())
                .build();
    }
}