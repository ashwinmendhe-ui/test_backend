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

    // FE-compatible single role
    private Long role;

    private UUID companyId;

    private String companyName;

    // FE-compatible alias
    private String company;

    private Boolean isActive;

    private String createdAt;
    private String updatedAt;
    private List<UUID> companyIds;
    private List<Long> roles;
    private List<String> companies;

    private List<UUID> siteIds;

    private List<UUID> deviceIds;

    private List<UUID> missionIds;

    private List<UserSiteResponse> sites;
}