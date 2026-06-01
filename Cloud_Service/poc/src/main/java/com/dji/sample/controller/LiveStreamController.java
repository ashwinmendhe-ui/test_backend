package com.dji.sample.controller;

import com.dji.sample.dto.request.StartStreamRequest;
import com.dji.sample.dto.request.StopStreamRequest;
import com.dji.sample.dto.response.ApiResponse;
import com.dji.sample.dto.response.StartStreamResponse;
import com.dji.sample.dto.response.StreamInfoResponse;
import com.dji.sample.dto.response.StreamStatusResponse;
import com.dji.sample.service.LiveStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/live")
@RequiredArgsConstructor
public class LiveStreamController {

    private final LiveStreamService liveStreamService;

    @PostMapping("/streams/start")
    public ApiResponse<StartStreamResponse> startStream(
            @RequestBody StartStreamRequest request
    ) {
        return ApiResponse.<StartStreamResponse>builder()
                .success(true)
                .message("Stream started successfully")
                .data(liveStreamService.startStream(request))
                .build();
    }

    @PostMapping("/streams/stop")
    public ApiResponse<StreamInfoResponse> stopStream(
            @RequestBody StopStreamRequest request
    ) {
        return ApiResponse.<StreamInfoResponse>builder()
                .success(true)
                .message("Stream stopped successfully")
                .data(liveStreamService.stopStream(request))
                .build();
    }

    @GetMapping("/stream-info/{streamId}")
    public ApiResponse<StreamInfoResponse> getStreamInfo(
            @PathVariable UUID streamId
    ) {
        return ApiResponse.<StreamInfoResponse>builder()
                .success(true)
                .message("Stream info fetched successfully")
                .data(liveStreamService.getStreamInfo(streamId))
                .build();
    }

    @PostMapping("/streams/heartbeat")
    public ApiResponse<StreamInfoResponse> heartbeat(
            @RequestParam UUID sessionId
    ) {
        return ApiResponse.<StreamInfoResponse>builder()
                .success(true)
                .message("Stream heartbeat updated successfully")
                .data(liveStreamService.heartbeat(sessionId))
                .build();
    }

    @GetMapping("/streams/status")
    public ApiResponse<StreamStatusResponse> getStreamStatus(
            @RequestParam String deviceSn
    ) {
        return ApiResponse.<StreamStatusResponse>builder()
                .success(true)
                .message("Stream status fetched successfully")
                .data(liveStreamService.getStreamStatus(deviceSn))
                .build();
    }
}