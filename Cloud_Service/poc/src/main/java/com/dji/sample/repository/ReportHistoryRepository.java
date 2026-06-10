package com.dji.sample.repository;

import com.dji.sample.entity.ReportHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ReportHistoryRepository extends JpaRepository<ReportHistory, UUID>,
        JpaSpecificationExecutor<ReportHistory> {

    Optional<ReportHistory> findByDeviceSnAndPlaybackUrl(String deviceSn, String playbackUrl);
}