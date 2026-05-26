package com.dji.sample.controller;

import com.dji.sample.dto.request.CreateUserRequest;
import com.dji.sample.dto.request.UpdateUserRequest;
import com.dji.sample.dto.response.ApiResponse;
import com.dji.sample.dto.response.UserResponse;
import com.dji.sample.service.UserService;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.dji.sample.dto.response.UserDetailResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/search")
    public List<UserResponse> searchUsers(
            @RequestParam(required = false) String keyword
    ) {
        return userService.searchUsers(keyword);
    }

    @GetMapping("/{id}")
    public UserDetailResponse getUserById(@PathVariable UUID id) {
        UserResponse user = userService.getUserById(id);

        return UserDetailResponse.builder()
                .roles(user.getRoleIds())
                .user(user)
                .build();
    }

    @PostMapping
    public ApiResponse<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        return ApiResponse.<UserResponse>builder()
                .success(true)
                .message("User created successfully")
                .data(userService.createUser(request))
                .build();
    }

    @PostMapping("/update/{id}")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable UUID id,
            @RequestBody UpdateUserRequest request
    ) {
        return ApiResponse.<UserResponse>builder()
                .success(true)
                .message("User updated successfully")
                .data(userService.updateUser(id, request))
                .build();
    }

    @PostMapping("/delete/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);

        return ApiResponse.<Void>builder()
                .success(true)
                .message("User deleted successfully")
                .data(null)
                .build();
    }
}