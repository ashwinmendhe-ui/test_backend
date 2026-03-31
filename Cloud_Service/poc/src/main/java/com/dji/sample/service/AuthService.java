package com.dji.sample.service;

import com.dji.sample.dto.request.LoginRequest;
import com.dji.sample.dto.request.RegisterRequest;
import com.dji.sample.dto.response.LoginResponse;

public interface AuthService {

    LoginResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void logout(String token);
}