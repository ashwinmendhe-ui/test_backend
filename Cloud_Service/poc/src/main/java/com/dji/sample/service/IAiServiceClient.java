package com.dji.sample.service;


import com.dji.sample.dto.request.AiServiceStreamRequest;
import com.dji.sample.dto.response.AiServiceStreamResponse;

/**
 * Service interface for AI Service integration
 * 
 * @author DHive Team
 * @date 2025-12-22
 */
public interface IAiServiceClient { 

    /**
     * Register stream with AI service for processing
     * 
     * @param request Stream registration request with RTMP URL and stream ID
     * @return Response containing WebRTC URL for frontend playback
     */
    String registerStream(AiServiceStreamRequest request);

    /**
     * Unregister stream from AI service
     * 
     * @param streamId Stream identifier to unregister
     * @return Response with unregistration status
     */
    AiServiceStreamResponse unregisterStream(String streamId);

    /**
     * Get stream information from AI service
     * 
     * @param streamId Stream identifier to query
     * @return Response containing stream status and info
     */
    AiServiceStreamResponse getStreamInfo(String streamId);

    /**
     * Get raw JSON response from AI service (forward as-is)
     * 
     * @param streamId Stream identifier to query
     * @return Raw JSON string from AI service, or null if not found/error
     */
    String getStreamInfoRaw(String streamId);
}
