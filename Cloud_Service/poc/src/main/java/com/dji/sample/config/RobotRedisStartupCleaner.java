package com.dji.sample.config;

import com.dji.sample.service.IDeviceRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RobotRedisStartupCleaner {

    private final IDeviceRedisService deviceRedisService;

    @EventListener(ApplicationReadyEvent.class)
    public void clearStaleRobotJobState() {
        log.info("[ROBOT][REDIS] Clearing stale robot job state on startup");
        deviceRedisService.clearAllRobotJobState();
    }
}