package com.dji.sample.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class UserResponse {

    private UUID userId;

    // FE-compatible alias
    private UUID id;

    private String username;

    // FE-compatible alias
    private String name;

    private String email;

    private String fullName;

    private String phone;

    private String description;

    private List<Long> roleIds;

    private List<String> roleNames;

    private UUID companyId;

    private String companyName;

    private Boolean isActive;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}