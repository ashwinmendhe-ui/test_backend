package com.dji.sample.service;

import com.dji.sample.dto.request.CreateUserRequest;
import com.dji.sample.dto.request.UpdateUserRequest;
import com.dji.sample.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {

    List<UserResponse> searchUsers(String keyword);

    UserResponse getUserById(UUID userId);

    UserResponse createUser(CreateUserRequest request);

    UserResponse updateUser(UUID userId, UpdateUserRequest request);

    void deleteUser(UUID userId);
}