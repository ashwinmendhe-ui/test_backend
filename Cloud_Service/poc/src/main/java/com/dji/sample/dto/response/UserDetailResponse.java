package com.dji.sample.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserDetailResponse {

    private List<Long> roles;

    private UserResponse user;
}