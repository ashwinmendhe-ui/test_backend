package com.dji.sample.service;

import com.dji.sample.dto.response.PlaybackListResponse;
import com.dji.sample.dto.response.PlaybackTelemetryResponse;
import com.dji.sample.entity.LiveStreamSession;
import com.dji.sample.entity.ReportHistory;
import com.dji.sample.repository.DeviceTelemetryHistoryRepository;
import com.dji.sample.repository.LiveStreamSessionRepository;
import com.dji.sample.repository.ReportHistoryRepository;
import com.dji.sample.util.DateTimeUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlaybackService {

    private final ReportHistoryRepository reportHistoryRepository;
    private final DeviceTelemetryHistoryRepository deviceTelemetryHistoryRepository;
    private final LiveStreamSessionRepository liveStreamSessionRepository;

    public List<PlaybackListResponse> getList(
            String companyId,
            String siteId,
            String deviceSn,
            String missionId
    ) {
        Specification<ReportHistory> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isNotNull(root.get("playbackUrl")));
            predicates.add(cb.notEqual(root.get("playbackUrl"), ""));

            if (companyId != null && !companyId.isBlank()) {
                predicates.add(cb.equal(root.get("companyId"), UUID.fromString(companyId)));
            }

            if (siteId != null && !siteId.isBlank()) {
                predicates.add(cb.equal(root.get("siteId"), UUID.fromString(siteId)));
            }

            if (deviceSn != null && !deviceSn.isBlank()) {
                predicates.add(cb.equal(root.get("deviceSn"), deviceSn));
            }

            if (missionId != null && !missionId.isBlank()) {
                predicates.add(cb.equal(root.get("missionId"), UUID.fromString(missionId)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return reportHistoryRepository
                .findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<PlaybackTelemetryResponse> getTelemetry(UUID sessionId) {

        LiveStreamSession session = liveStreamSessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Stream session not found"));

        OffsetDateTime startedAt = session.getStartedAt();

        return deviceTelemetryHistoryRepository
                .findBySessionIdOrderByRecordedAtAsc(sessionId)
                .stream()
                .map(item -> PlaybackTelemetryResponse.builder()
                        .recordedAt(item.getRecordedAt())
                        .offsetMs(
                                startedAt != null
                                        ? java.time.Duration
                                            .between(startedAt, item.getRecordedAt())
                                            .toMillis()
                                        : 0L
                        )
                        .status(item.getStatus())
                        .battery(item.getBattery())
                        .network(item.getNetwork())
                        .gps(item.getGps())
                        .latitude(item.getLatitude())
                        .longitude(item.getLongitude())
                        .altitude(item.getAltitude())
                        .speed(item.getSpeed())
                        .build()
                )
                .toList();
    }
    
    private PlaybackListResponse toResponse(ReportHistory history) {
        String segment = DateTimeUtil.formatKst(history.getCreatedAt());

        return PlaybackListResponse.builder()
                .segment(segment)
                .url(history.getPlaybackUrl())
                .sessionId(history.getSessionId())
                .build();
    }
}