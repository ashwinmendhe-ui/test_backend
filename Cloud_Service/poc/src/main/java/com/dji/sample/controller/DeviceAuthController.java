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
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/v1/device/auth")
@RequiredArgsConstructor
@Slf4j
public class DeviceAuthController {

    private final DeviceAuthService deviceAuthService;

    @PostMapping("/login")
    public ResponseEntity<DeviceLoginResponse> login(
            @Valid @RequestBody DeviceLoginRequest request
    ) {
        DeviceLoginResponse response = deviceAuthService.login(request);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/diagnostic")
    public ResponseEntity<Void> diagnostic(
            @RequestParam String event,
            @RequestParam(required = false) String deviceSn,
            @RequestParam(required = false) String timestamp
    ) {

        log.warn(
                "[DJI_LIFECYCLE] event={} deviceSn={} timestamp={}",
                event,
                deviceSn,
                timestamp
        );

        return ResponseEntity.noContent().build();
    }
}