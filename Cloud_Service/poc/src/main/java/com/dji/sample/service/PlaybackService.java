package com.dji.sample.service;

import com.dji.sample.dto.response.PlaybackListResponse;
import com.dji.sample.entity.ReportHistory;
import com.dji.sample.repository.ReportHistoryRepository;
import com.dji.sample.util.DateTimeUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlaybackService {

    private final ReportHistoryRepository reportHistoryRepository;

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

    private PlaybackListResponse toResponse(ReportHistory history) {
        String segment = DateTimeUtil.formatKst(history.getCreatedAt());

        return PlaybackListResponse.builder()
                .segment(segment)
                .url(history.getPlaybackUrl())
                .build();
    }
}