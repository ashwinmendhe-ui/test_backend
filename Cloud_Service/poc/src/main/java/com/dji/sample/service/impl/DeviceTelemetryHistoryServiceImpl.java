package com.dji.sample.service.impl;

import com.dji.sample.entity.DeviceTelemetryHistory;
import com.dji.sample.entity.LiveStreamSession;
import com.dji.sample.repository.DeviceTelemetryHistoryRepository;
import com.dji.sample.repository.LiveStreamSessionRepository;
import com.dji.sample.service.DeviceTelemetryHistoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceTelemetryHistoryServiceImpl
        implements DeviceTelemetryHistoryService {

    private static final long MIN_SAVE_INTERVAL_MS = 1000L;

    private final DeviceTelemetryHistoryRepository telemetryRepository;
    private final LiveStreamSessionRepository liveStreamSessionRepository;

    /*
     * Prevent high-frequency DJI OSD packets from creating excessive
     * database records.
     *
     * Key = livestream session ID.
     */
    private final Map<UUID, Long> lastSavedAtBySession =
            new ConcurrentHashMap<>();

    @Override
    @Transactional
    public void recordTelemetry(
            String deviceSn,
            Map<String, Object> telemetry
    ) {

        if (deviceSn == null ||
                deviceSn.isBlank() ||
                telemetry == null ||
                telemetry.isEmpty()) {
            return;
        }

        LiveStreamSession session =
                liveStreamSessionRepository
                        .findFirstByDeviceSnAndSessionStatusOrderByStartedAtDesc(
                                deviceSn,
                                "ACTIVE"
                        )
                        .orElse(null);

        /*
         * Do not persist telemetry when there is no active stream.
         */
        if (session == null) {
            return;
        }

        long now = System.currentTimeMillis();

        Long lastSavedAt =
                lastSavedAtBySession.get(session.getId());

        if (lastSavedAt != null &&
                now - lastSavedAt < MIN_SAVE_INTERVAL_MS) {
            return;
        }

        DeviceTelemetryHistory history =
                DeviceTelemetryHistory.builder()
                        .sessionId(session.getId())
                        .deviceSn(deviceSn)
                        .recordedAt(OffsetDateTime.now())

                        .status(asString(telemetry.get("status")))
                        .battery(asDouble(telemetry.get("battery")))
                        .network(asString(telemetry.get("network")))

                        .gps(firstNonBlank(
                                asString(telemetry.get("gps")),
                                asString(telemetry.get("gpsFix"))
                        ))

                        .latitude(asDouble(telemetry.get("latitude")))
                        .longitude(asDouble(telemetry.get("longitude")))
                        .altitude(asDouble(telemetry.get("altitude")))
                        .speed(asDouble(telemetry.get("speed")))

                        .build();

        telemetryRepository.save(history);

        lastSavedAtBySession.put(
                session.getId(),
                now
        );

        log.debug(
                "[PLAYBACK_TELEMETRY] Saved. sessionId={}, deviceSn={}, battery={}, status={}, lat={}, lon={}",
                session.getId(),
                deviceSn,
                history.getBattery(),
                history.getStatus(),
                history.getLatitude(),
                history.getLongitude()
        );
    }

    private Double asDouble(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        try {
            return Double.parseDouble(
                    String.valueOf(value)
            );
        } catch (Exception e) {
            return null;
        }
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }

        String result = String.valueOf(value);

        return result.isBlank()
                ? null
                : result;
    }

    private String firstNonBlank(
            String first,
            String second
    ) {
        if (first != null && !first.isBlank()) {
            return first;
        }

        return second;
    }
}