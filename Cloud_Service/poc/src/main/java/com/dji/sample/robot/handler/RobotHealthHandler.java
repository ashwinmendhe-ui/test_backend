package com.dji.sample.robot.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RobotHealthHandler {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void handle(String deviceSn, String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);

            boolean online = root.path("data").path("online").asBoolean(false);

            String key = "online:" + deviceSn;

            if (online) {
                redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(60));
                log.info("Robot health updated ONLINE. deviceSn={}", deviceSn);
            } else {
                redisTemplate.delete(key);
                log.info("Robot health updated OFFLINE. deviceSn={}", deviceSn);
            }

        } catch (Exception e) {
            log.error("Failed to handle robot health. deviceSn={}, payload={}", deviceSn, payload, e);
        }
    }
}