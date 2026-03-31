package com.dji.sample.dto.response;

public record ErrorResponse(
        String code,
        String message
) {
}