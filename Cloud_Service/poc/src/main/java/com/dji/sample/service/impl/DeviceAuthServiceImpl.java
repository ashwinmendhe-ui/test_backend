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

        int selectedMqttPort = Boolean.TRUE.equals(mqttUseSsl)
                ? mqttSslPort
                : mqttPort;

        return DeviceLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getMqttAccessTokenExpirationSeconds())
                .tokenType(TOKEN_TYPE)
                .mqttHost(mqttPublicHost)
                .mqttPort(selectedMqttPort)
                .mqttUseSsl(Boolean.TRUE.equals(mqttUseSsl))
                .mqttUsername(device.getDeviceSn())
                .deviceSn(device.getDeviceSn())
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