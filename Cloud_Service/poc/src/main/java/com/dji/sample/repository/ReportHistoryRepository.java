package com.dji.sample.repository;

import com.dji.sample.entity.ReportHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ReportHistoryRepository extends JpaRepository<ReportHistory, UUID>,
        JpaSpecificationExecutor<ReportHistory> {

    Optional<ReportHistory> findByDeviceSnAndPlaybackUrl(String deviceSn, String playbackUrl);
    List<ReportHistory> findByOrderByCreatedAtDesc(Pageable pageable);
}