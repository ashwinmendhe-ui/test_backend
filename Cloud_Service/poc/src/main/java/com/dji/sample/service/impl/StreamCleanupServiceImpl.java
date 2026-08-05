package com.dji.sample.service.impl;

import com.dji.sample.dto.request.CreateHistoryRequest;
import com.dji.sample.dto.response.AiServiceStreamResponse;
import com.dji.sample.dto.response.HistoryDetailResponse;
import com.dji.sample.entity.Device;
import com.dji.sample.entity.LiveStreamSession;
import com.dji.sample.repository.DeviceRepository;
import com.dji.sample.repository.LiveStreamSessionRepository;
import com.dji.sample.robot.service.IRobotCommandService;
import com.dji.sample.service.DeviceWebSocketPublisher;
import com.dji.sample.service.HistoryService;
import com.dji.sample.service.IAiServiceClient;
import com.dji.sample.service.IDeviceRedisService;
import com.dji.sample.service.SlackNotificationService;
import com.dji.sample.service.StreamCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dji.sample.drone.model.StreamSource;
import com.dji.sample.drone.service.StreamSourceResolver;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import com.dji.sample.drone.service.DjiLivestreamService;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamCleanupServiceImpl implements StreamCleanupService {

    private final LiveStreamSessionRepository liveStreamSessionRepository;
    private final DeviceRepository deviceRepository;
    private final IAiServiceClient aiServiceClient;
    private final IDeviceRedisService deviceRedisService;
    private final IRobotCommandService robotCommandService;
    private final HistoryService historyService;
    private final SlackNotificationService slackNotificationService;
    private final DeviceWebSocketPublisher webSocketPublisher;
    private final DjiLivestreamService djiLivestreamService;
    private final StreamSourceResolver streamSourceResolver;
    

    @Override
    @Transactional
    public LiveStreamSession cleanupStream(
                String requestDeviceSn,
                String physicalStreamSn,
                String reason,
                boolean createHistory
        ) {

        log.warn(
                "[STREAM_CLEANUP] Started. requestDeviceSn={}, physicalStreamSn={}, reason={}, createHistory={}",
                requestDeviceSn,
                physicalStreamSn,
                reason,
                createHistory
        );

        String aiStreamId = buildAiStreamId(physicalStreamSn);

        LiveStreamSession session =
                liveStreamSessionRepository
                        .findFirstByDeviceSnAndSessionStatusOrderByStartedAtDesc(
                                physicalStreamSn,
                                "ACTIVE"
                        )
                        .orElse(null);

        Device device =
                deviceRepository
                        .findByDeviceSnAndDeletedAtIsNull(requestDeviceSn)
                        .orElse(null);

        /*
        * 1. Stop the physical device runtime.
        *
        * Robot and DJI cleanup must be mutually exclusive.
        */
        if (device == null) {
                log.warn(
                        "[STREAM_CLEANUP] Device not found. Skipping physical device cleanup. requestDeviceSn={}, reason={}",
                        requestDeviceSn,
                        reason
                );
        } else if (isRobot(device)) {
                cleanupRobotJob(requestDeviceSn, reason);
        } else {
                cleanupDroneStream(requestDeviceSn);
        }

        /*
        * 2. Always try to remove the AI stream.
        *
        * This must run even when no ACTIVE DB session exists because the
        * AI registration itself may be stale.
        */
        cleanupAiStream(aiStreamId, requestDeviceSn, reason);

        /*
        * 3. Mark the DB session as stopped.
        */
        LiveStreamSession savedSession = session;

        if (session != null) {
                OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

                session.setSessionStatus("STOPPED");
                session.setStoppedAt(now);

                savedSession = liveStreamSessionRepository.save(session);

                log.info(
                        "[STREAM_CLEANUP] Session marked STOPPED. sessionId={}, deviceSn={}, reason={}",
                        savedSession.getId(),
                        physicalStreamSn,
                        reason
                );
        } else {
                log.warn(
                        "[STREAM_CLEANUP] No ACTIVE DB session found. AI and runtime cleanup still executed. physicalStreamSn={}, reason={}",
                        physicalStreamSn,
                        reason
                );
        }

        /*
        * 4. Create report/history only when requested and when an active
        * session was actually found.
        */
        if (createHistory && savedSession != null) {
                createHistoryAndNotify(
                        requestDeviceSn,
                        savedSession,
                        reason
                );
        }

        /*
        * 5. Clear mission from Device.
        */
        if (device != null) {
                device.setMissionId(null);
                deviceRepository.save(device);
        }

        /*
        * 6. Clear stream/job runtime state.
        *
        * Do not remove online:{deviceSn}; that key represents device health.
        */
        try {
                deviceRedisService.clearRobotJobState(requestDeviceSn);
        } catch (Exception exception) {
                log.warn(
                        "[STREAM_CLEANUP] Failed to clear robot job state. deviceSn={}, reason={}, error={}",
                        requestDeviceSn,
                        reason,
                        exception.getMessage(),
                        exception
                );
        }

        try {
                deviceRedisService.clearDeviceStatus(requestDeviceSn);
        } catch (Exception exception) {
                log.warn(
                        "[STREAM_CLEANUP] Failed to clear device status. deviceSn={}, reason={}, error={}",
                        requestDeviceSn,
                        reason,
                        exception.getMessage(),
                        exception
                );
        }

        /*
        * 7. Refresh dashboard state.
        */
        try {
                webSocketPublisher.publishDashboardRefresh(
                        requestDeviceSn,
                        "stream-cleanup-" + reason.toLowerCase()
                );
        } catch (Exception exception) {
                log.warn(
                        "[STREAM_CLEANUP] Dashboard refresh failed. deviceSn={}, reason={}, error={}",
                        requestDeviceSn,
                        reason,
                        exception.getMessage(),
                        exception
                );
        }

        log.warn(
                "[STREAM_CLEANUP] Completed. requestDeviceSn={}, physicalStreamSn={}, reason={}, sessionId={}",
                requestDeviceSn,
                physicalStreamSn,
                reason,
                savedSession != null ? savedSession.getId() : null
        );

        return savedSession;
        }
        private void cleanupRobotJob(
                String deviceSn,
                String reason
        ) {
                String jobId = null;

                try {
                jobId = deviceRedisService.getRobotJobId(deviceSn);

                if (jobId != null && !jobId.isBlank()) {
                        robotCommandService.cancelJob(
                                deviceSn,
                                UUID.randomUUID().toString(),
                                jobId
                        );

                        log.info(
                                "[STREAM_CLEANUP] Robot cancel_job sent. deviceSn={}, jobId={}, reason={}",
                                deviceSn,
                                jobId,
                                reason
                        );
                } else {
                        robotCommandService.cleanJob(deviceSn);

                        log.warn(
                                "[STREAM_CLEANUP] Robot jobId missing; clean_job sent. deviceSn={}, reason={}",
                                deviceSn,
                                reason
                        );
                }
                } catch (Exception exception) {
                log.warn(
                        "[STREAM_CLEANUP] Robot job cleanup failed. deviceSn={}, jobId={}, reason={}, error={}",
                        deviceSn,
                        jobId,
                        reason,
                        exception.getMessage(),
                        exception
                );
                }
        }

        private void cleanupAiStream(
                String aiStreamId,
                String deviceSn,
                String reason
        ) {
                try {
                AiServiceStreamResponse response =
                        aiServiceClient.unregisterStream(aiStreamId);

                if (response == null) {
                        log.warn(
                                "[STREAM_CLEANUP] AI unregister returned null. deviceSn={}, aiStreamId={}, reason={}",
                                deviceSn,
                                aiStreamId,
                                reason
                        );
                        return;
                }

                /*
                * AiServiceClientImpl currently catches HTTP errors and returns an
                * error response instead of throwing. Therefore, inspect the
                * returned response as well as catching exceptions.
                */
                if ("error".equalsIgnoreCase(response.getState())) {
                        log.warn(
                                "[STREAM_CLEANUP] AI unregister returned error. deviceSn={}, aiStreamId={}, reason={}, message={}",
                                deviceSn,
                                aiStreamId,
                                reason,
                                response.getMessage()
                        );
                        return;
                }

                log.info(
                        "[STREAM_CLEANUP] AI stream unregistered. deviceSn={}, aiStreamId={}, reason={}, response={}",
                        deviceSn,
                        aiStreamId,
                        reason,
                        response
                );

                } catch (Exception exception) {
                log.warn(
                        "[STREAM_CLEANUP] AI unregister failed. deviceSn={}, aiStreamId={}, reason={}, error={}",
                        deviceSn,
                        aiStreamId,
                        reason,
                        exception.getMessage(),
                        exception
                );
                }
        }

        private void createHistoryAndNotify(
                String requestDeviceSn,
                LiveStreamSession session,
                String reason
        ) {
                try {
                CreateHistoryRequest historyRequest =
                        new CreateHistoryRequest();

                historyRequest.setDeviceSn(requestDeviceSn);
                historyRequest.setMissionId(session.getMissionId());
                historyRequest.setPlaybackUrl(session.getPlaybackUrl());
                historyRequest.setSessionId(session.getId());

                HistoryDetailResponse report =
                        historyService.createHistory(historyRequest);

                slackNotificationService.notifyAiDetectionReport(report);

                log.info(
                        "[STREAM_CLEANUP] History/report created. sessionId={}, deviceSn={}, reason={}",
                        session.getId(),
                        requestDeviceSn,
                        reason
                );

                } catch (Exception exception) {
                log.warn(
                        "[STREAM_CLEANUP] History or Slack notification failed. sessionId={}, deviceSn={}, reason={}, error={}",
                        session.getId(),
                        requestDeviceSn,
                        reason,
                        exception.getMessage(),
                        exception
                );
                }
        }

        private String buildAiStreamId(String physicalStreamSn) {
                return "robopilot-" + physicalStreamSn;
        }

        private boolean isRobot(Device device) {
                String type = device.getDeviceType();

                return "Robot".equalsIgnoreCase(type)
                        || "4족보행 로봇".equals(type)
                        || "로봇".equals(type);
        }


    private void cleanupDroneStream(String requestDeviceSn) {

        try {

                StreamSource streamSource =
                        streamSourceResolver.resolveForDeviceSn(requestDeviceSn);

                log.info(
                        "[DJI] Calling live_stop_push. gatewaySn={}, droneSn={}, payloadIndex={}, videoType={}",
                        streamSource.gatewaySn(),
                        streamSource.droneSn(),
                        streamSource.payloadIndex(),
                        streamSource.videoType()
                );

                djiLivestreamService.stopPush(
                        streamSource.gatewaySn(),
                        streamSource.droneSn(),
                        streamSource.payloadIndex(),
                        streamSource.videoType()
                );

        } catch (Exception e) {

                log.warn(
                        "[DJI] Failed to stop livestream. deviceSn={}, error={}",
                        requestDeviceSn,
                        e.getMessage(),
                        e
                );
        }
        }
}