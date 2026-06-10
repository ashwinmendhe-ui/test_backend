package com.dji.sample.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO from AI Service Stream Processing
 *
 * @author DHive Team
 * @date 2025-12-22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiServiceStreamResponse {

    /**
     * Stream identifier returned from AI service
     */
    @JsonProperty("stream_id")
    private String streamId;

    /**
     * Status of stream registration
     */
    @JsonProperty("state")
    private String state;

    /**
     * Error message if any
     */
    @JsonProperty("message")
    private String message;

    /**
     * Indicates if stream is currently running
     */
    @JsonProperty("status")
    private String status;

    @JsonProperty("playbackUrl")
    private String playbackUrl;
}
