package com.dji.sample.controller;

import com.dji.sample.dto.auth.DeviceLoginRequest;
import com.dji.sample.dto.auth.DeviceLoginResponse;
import com.dji.sample.service.DeviceAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/device/auth")
@RequiredArgsConstructor
public class DeviceAuthController {

    private final DeviceAuthService deviceAuthService;

    @PostMapping("/login")
    public ResponseEntity<DeviceLoginResponse> login(
            @Valid @RequestBody DeviceLoginRequest request
    ) {
        DeviceLoginResponse response = deviceAuthService.login(request);
        return ResponseEntity.ok(response);
    }
}