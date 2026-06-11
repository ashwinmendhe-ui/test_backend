package com.dji.sample.robot.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RobotHealthHandler {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void handle(String deviceSn, String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode data = root.path("data");

            boolean online = data.path("online").asBoolean(false);

            String onlineKey = "online:" + deviceSn;

            if (online) {
                redisTemplate.opsForValue().set(onlineKey, "1", Duration.ofSeconds(60));
                log.info("Robot health updated ONLINE. deviceSn={}", deviceSn);
            } else {
                redisTemplate.delete(onlineKey);
                redisTemplate.delete("robot:" + deviceSn + ":telemetry");
                log.info("Robot health updated OFFLINE. deviceSn={}", deviceSn);
                return;
            }

            Map<String, Object> telemetry = new HashMap<>();

            telemetry.put("battery", data.path("battery").path("percent").asDouble());
            telemetry.put("voltage", data.path("battery").path("voltage").asDouble());
            telemetry.put("charging", data.path("battery").path("charging").asBoolean());

            telemetry.put("network", data.path("network").path("link").asText(null));
            telemetry.put("rssi", data.path("network").path("rssi").asInt());
            telemetry.put("rsrp", data.path("network").path("rsrp").asInt());
            telemetry.put("latencyMs", data.path("network").path("latency_ms").asInt());

            telemetry.put("gpsFix", data.path("gps").path("fix").asText(null));
            telemetry.put("latitude", data.path("gps").path("latitude").asDouble());
            telemetry.put("longitude", data.path("gps").path("longitude").asDouble());
            telemetry.put("altitude", data.path("gps").path("altitude").asDouble());

            telemetry.put("status", data.path("status").asText(null));
            telemetry.put("robotType", data.path("robot_type").asText(null));
            telemetry.put("firmwareVersion", data.path("firmware_version").asText(null));
            telemetry.put("appVersion", data.path("app_version").asText(null));
            telemetry.put("lastBootAt", data.path("last_boot_at").asText(null));
            telemetry.put("timestamp", root.path("timestamp").asText(null));

            redisTemplate.opsForValue().set(
                    "robot:" + deviceSn + ":telemetry",
                    objectMapper.writeValueAsString(telemetry),
                    Duration.ofSeconds(60)
            );

            log.info("Robot telemetry saved from health. deviceSn={}", deviceSn);

        } catch (Exception e) {
            log.error("Failed to handle robot health. deviceSn={}, payload={}", deviceSn, payload, e);
        }
    }
}