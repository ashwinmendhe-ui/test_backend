package com.dji.sample.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceLoginResponse {

    private String accessToken;

    private String refreshToken;

    private Long expiresIn;

    private String tokenType;

    private String mqttHost;

    private Integer mqttPort;

    private Boolean mqttUseSsl;

    private String mqttUsername;

    private String deviceSn;
    private String workspaceId;
    private String username;
    private String userId;
}