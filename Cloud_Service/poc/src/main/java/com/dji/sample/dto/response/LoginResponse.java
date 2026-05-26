package com.dji.sample.dto.response;

import java.util.List;

public record LoginResponse(
        String token,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        Long refreshExpiresIn,
        String username,
        String email,
        String userId,
        List<String> roles
) {
}