package com.dji.sample.service.impl;

import com.dji.sample.dto.request.LoginRequest;
import com.dji.sample.dto.request.RefreshTokenRequest;
import com.dji.sample.dto.request.RegisterRequest;
import com.dji.sample.dto.response.LoginResponse;
import com.dji.sample.entity.User;
import com.dji.sample.entity.UserRole;
import com.dji.sample.repository.RoleRepository;
import com.dji.sample.repository.UserRepository;
import com.dji.sample.repository.UserRoleRepository;
import com.dji.sample.security.JwtService;
import com.dji.sample.service.AuthService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    @Override
    public LoginResponse register(RegisterRequest request) {

        if (userRepository.findByEmailAndDeletedAtIsNull(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setIsActive(true);

        User savedUser = userRepository.save(user);

        List<String> roles = getRoleKeys(savedUser.getUserId());

        String token = jwtService.generateAccessToken(
                savedUser.getUserId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                roles
        );

        String refreshToken = jwtService.generateRefreshToken(
                savedUser.getUserId(),
                savedUser.getUsername(),
                savedUser.getEmail()
        );

        return buildLoginResponse(savedUser, token, refreshToken, roles);
    }

    @Override
    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmailAndDeletedAtIsNull(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        assertActiveUser(user);

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        List<String> roles = getRoleKeys(user.getUserId());

        String token = jwtService.generateAccessToken(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                roles
        );

        String refreshToken = jwtService.generateRefreshToken(
                user.getUserId(),
                user.getUsername(),
                user.getEmail()
        );

        return buildLoginResponse(user, token, refreshToken, roles);
    }

    @Override
    public LoginResponse refreshToken(RefreshTokenRequest request) {

        if (request.refreshToken() == null || request.refreshToken().isBlank()) {
            throw new IllegalArgumentException("Refresh token is required");
        }

        Claims claims;

        try {
            claims = jwtService.parseToken(request.refreshToken());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String tokenType = claims.get("token_type", String.class);
        if (!"REFRESH".equals(tokenType)) {
            throw new IllegalArgumentException("Token provided is not a refresh token");
        }

        UUID userId;

        try {
            userId = UUID.fromString(claims.getSubject());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        assertActiveUser(user);

        List<String> roles = getRoleKeys(user.getUserId());

        String newAccessToken = jwtService.generateAccessToken(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                roles
        );

        String newRefreshToken = jwtService.generateRefreshToken(
                user.getUserId(),
                user.getUsername(),
                user.getEmail()
        );

        return buildLoginResponse(user, newAccessToken, newRefreshToken, roles);
    }

    @Override
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Authorization token is required");
        }
    }

    private void assertActiveUser(User user) {
        if (user.getDeletedAt() != null || !Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("User account is inactive");
        }
    }

    private List<String> getRoleKeys(UUID userId) {

        List<Integer> roleIds = userRoleRepository.findByUserId(userId)
                .stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());

        if (roleIds.isEmpty()) {
            return List.of("COMPANY_USER");
        }

        return roleRepository.findRoleKeysByIds(roleIds);
    }

    private LoginResponse buildLoginResponse(
            User user,
            String token,
            String refreshToken,
            List<String> roles
    ) {
        return new LoginResponse(
                token,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationMs(),
                jwtService.getRefreshTokenExpirationMs(),
                user.getUsername(),
                user.getEmail(),
                user.getUserId().toString(),
                roles
        );
    }
}