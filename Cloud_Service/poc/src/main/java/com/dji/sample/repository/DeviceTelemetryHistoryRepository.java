package com.dji.sample.repository;

import com.dji.sample.entity.DeviceTelemetryHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeviceTelemetryHistoryRepository
        extends JpaRepository<DeviceTelemetryHistory, UUID> {

    List<DeviceTelemetryHistory>
            findBySessionIdOrderByRecordedAtAsc(UUID sessionId);

    void deleteBySessionId(UUID sessionId);
}