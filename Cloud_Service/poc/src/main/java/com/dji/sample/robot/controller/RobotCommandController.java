package com.dji.sample.robot.controller;

import com.dji.sample.robot.service.IRobotCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/robot/commands")
public class RobotCommandController {

    private final IRobotCommandService robotCommandService;
    private final ObjectMapper objectMapper;

    @PostMapping("/create-job")
    public ResponseEntity<?> createJob(
            @RequestParam String robotId,
            @RequestParam String jobId,
            @RequestParam(required = false) String payload
    ) throws Exception {
        String commandId = UUID.randomUUID().toString();

        Map<String, Object> payloadMap = null;
        if (payload != null && !payload.isBlank()) {
            payloadMap = objectMapper.readValue(payload, Map.class);
        }

        robotCommandService.createJob(robotId, commandId, jobId, payloadMap);
        return ResponseEntity.ok("Job created successfully");
    }

    @PostMapping("/start-job")
    public ResponseEntity<?> startJob(@RequestParam String robotId,
                                      @RequestParam String jobId) {
        robotCommandService.startJob(robotId, UUID.randomUUID().toString(), jobId);
        return ResponseEntity.ok("Job started successfully");
    }

    @PostMapping("/cancel-job")
    public ResponseEntity<?> cancelJob(@RequestParam String robotId,
                                       @RequestParam String jobId) {
        robotCommandService.cancelJob(robotId, UUID.randomUUID().toString(), jobId);
        return ResponseEntity.ok("Job cancelled successfully");
    }

    @PostMapping("/ack-job")
    public ResponseEntity<?> ackJob(@RequestParam String robotId,
                                    @RequestParam String jobId) {
        robotCommandService.ackJob(robotId, UUID.randomUUID().toString(), jobId);
        return ResponseEntity.ok("Job acknowledged successfully");
    }

    @PostMapping("/clean-job")
    public ResponseEntity<?> cleanJob(@RequestParam String robotId) {
        robotCommandService.cleanJob(robotId);
        return ResponseEntity.ok("Job cleaned successfully");
    }
}