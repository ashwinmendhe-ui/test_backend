package com.dji.sample.service.impl;

import com.dji.sample.dto.request.LoginRequest;
import com.dji.sample.dto.request.RegisterRequest;
import com.dji.sample.dto.response.LoginResponse;
import com.dji.sample.service.AuthService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    @Override
    public LoginResponse register(RegisterRequest request) {
        return new LoginResponse(
                "temp-register-token",
                "Bearer",
                3600L,
                request.username(),
                request.email(),
                UUID.randomUUID().toString(),
                List.of("COMPANY_USER")
        );
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        return new LoginResponse(
                "temp-login-token",
                "Bearer",
                3600L,
                "demo-user",
                request.email(),
                UUID.randomUUID().toString(),
                List.of("COMPANY_USER")
        );
    }

    @Override
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Authorization token is required");
        }
    }
}