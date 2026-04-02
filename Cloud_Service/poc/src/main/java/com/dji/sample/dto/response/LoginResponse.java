package com.dji.sample.dto.response;

import java.util.List;

public record LoginResponse(
        String token,
        String tokenType,
        Long expiresIn,
        String username,
        String email,
        String userId,
        List<String> roles
) {
}