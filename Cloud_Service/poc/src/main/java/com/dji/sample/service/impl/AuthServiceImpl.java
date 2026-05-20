package com.dji.sample.service.impl;

import com.dji.sample.dto.request.LoginRequest;
import com.dji.sample.dto.request.RegisterRequest;
import com.dji.sample.dto.response.LoginResponse;
import com.dji.sample.entity.User;
import com.dji.sample.repository.UserRepository;
import com.dji.sample.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse register(RegisterRequest request) {

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setIsActive(true);

        User savedUser = userRepository.save(user);

        return new LoginResponse(
                "temp-register-token",
                "Bearer",
                3600L,
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getUserId().toString(),
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
                "",
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