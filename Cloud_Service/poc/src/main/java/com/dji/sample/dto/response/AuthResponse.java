package com.dji.sample.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String tokenType;
    private String username;
    private String email;

    public AuthResponse(String accessToken, String tokenType, String username, String email) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.username = username;
        this.email = email;
    }
}