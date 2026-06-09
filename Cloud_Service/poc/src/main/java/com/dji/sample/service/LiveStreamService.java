package com.dji.sample.service;

import com.dji.sample.dto.request.StartStreamRequest;
import com.dji.sample.dto.request.StopStreamRequest;
import com.dji.sample.dto.response.StartStreamResponse;
import com.dji.sample.dto.response.StreamInfoResponse;
import com.dji.sample.dto.response.StreamStatusResponse;

import java.util.UUID;

public interface LiveStreamService {

    StartStreamResponse startStream(StartStreamRequest request);

    StreamInfoResponse stopStream(StopStreamRequest request);

    StreamInfoResponse getStreamInfo(String streamId);

    StreamInfoResponse heartbeat(UUID sessionId);

    StreamStatusResponse getStreamStatus(String deviceSn);
}