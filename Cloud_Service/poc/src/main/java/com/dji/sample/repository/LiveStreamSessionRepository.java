package com.dji.sample.repository;

import com.dji.sample.entity.LiveStreamSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LiveStreamSessionRepository extends JpaRepository<LiveStreamSession, UUID> {

    Optional<LiveStreamSession> findByIdAndSessionStatus(UUID id, String sessionStatus);

    Optional<LiveStreamSession> findFirstByDeviceSnAndSessionStatusOrderByStartedAtDesc(
            String deviceSn,
            String sessionStatus
    );

    boolean existsByDeviceSnAndSessionStatus(
            String deviceSn,
            String sessionStatus
    );

    boolean existsByMissionIdAndSessionStatus(UUID missionId, String sessionStatus);

    Optional<LiveStreamSession> findFirstByDeviceSnAndMissionIdOrderByStartedAtDesc(
            String deviceSn,
            UUID missionId
    );
}