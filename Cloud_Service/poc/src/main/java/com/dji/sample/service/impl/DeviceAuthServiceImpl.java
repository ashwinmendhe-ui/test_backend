package com.dji.sample.service.impl;

import com.dji.sample.dto.auth.DeviceLoginRequest;
import com.dji.sample.dto.auth.DeviceLoginResponse;
import com.dji.sample.entity.Device;
import com.dji.sample.entity.User;
import com.dji.sample.repository.DeviceRepository;
import com.dji.sample.repository.UserRepository;
import com.dji.sample.security.JwtService;
import com.dji.sample.service.DeviceAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceAuthServiceImpl implements DeviceAuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private static final UUID ZERO_UUID =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${mqtt.public-host}")
    private String mqttPublicHost;

    @Value("${mqtt.port:1883}")
    private Integer mqttPort;

    @Value("${mqtt.ssl-port:8883}")
    private Integer mqttSslPort;

    @Value("${mqtt.use-ssl:false}")
    private Boolean mqttUseSsl;

    @Value("${dji.workspace-id:}")
    private String djiWorkspaceId;

    @Override
    @Transactional(readOnly = true)
    public DeviceLoginResponse login(DeviceLoginRequest request) {

        String username = request.getUsername().trim();
        String deviceSn = request.getDeviceSn().trim();

        User user = userRepository
                .findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid username or password"
                ));

        validateUser(user, request.getPassword());

        Device device = deviceRepository
                .findByDeviceSnAndDeletedAtIsNull(deviceSn)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Device not found"
                ));

        validateCompanyOwnership(user, device);

        UUID companyId = device.getCompany() != null
                ? device.getCompany().getCompanyId()
                : null;

        String accessToken = jwtService.generateDeviceAccessToken(
                device.getDeviceId(),
                device.getDeviceSn(),
                companyId,
                getDevicePermissions(device)
        );

        String refreshToken = jwtService.generateDeviceRefreshToken(
                device.getDeviceId(),
                device.getDeviceSn()
        );

        return DeviceLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getMqttAccessTokenExpirationSeconds())
                .tokenType(TOKEN_TYPE)

                .workspaceId(resolveWorkspaceId(user))
                .username(user.getUsername())
                .userId(user.getUserId().toString())

                .mqttHost(mqttPublicHost)
                .mqttPort(Boolean.TRUE.equals(mqttUseSsl) ? mqttSslPort : mqttPort)
                .mqttUseSsl(mqttUseSsl)
                .mqttUsername(deviceSn)
                .deviceSn(deviceSn)
                .build();
    }

    private void validateUser(User user, String rawPassword) {
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User account is inactive"
            );
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid username or password"
            );
        }
    }

    private String resolveWorkspaceId(User user) {
        UUID companyId = user.getCompanyId();

        if (companyId == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Company ID is missing"
            );
        }

        // Normal companies continue using their existing company UUID.
        if (!ZERO_UUID.equals(companyId)) {
            return companyId.toString();
        }

        // D.Hive uses the all-zero company UUID in existing production data.
        // DJI must receive a stable, non-zero workspace ID instead.
        if (djiWorkspaceId == null || djiWorkspaceId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "DJI workspace ID is not configured"
            );
        }

        UUID workspaceId;

        try {
            workspaceId = UUID.fromString(djiWorkspaceId.trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Invalid DJI workspace ID configuration"
            );
        }

        if (ZERO_UUID.equals(workspaceId)) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "DJI workspace ID must not be the all-zero UUID"
            );
        }

        return workspaceId.toString();
    }

    private void validateCompanyOwnership(User user, Device device) {
        if (user.getCompanyId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User is not assigned to a company"
            );
        }

        if (device.getCompany() == null
                || device.getCompany().getCompanyId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Device is not assigned to a company"
            );
        }

        UUID deviceCompanyId = device.getCompany().getCompanyId();

        if (!user.getCompanyId().equals(deviceCompanyId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User is not authorized to access this device"
            );
        }
    }

    private List<String> getDevicePermissions(Device device) {
        return List.of(
                "mqtt:connect",
                "mqtt:publish",
                "mqtt:subscribe"
        );
    }
}