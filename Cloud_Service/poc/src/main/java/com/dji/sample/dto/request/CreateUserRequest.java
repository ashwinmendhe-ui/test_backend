package com.dji.sample.dto.request;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateUserRequest {

    private String username;

    private String email;

    private String password;

    private String fullName;

    private String phone;

    private String description;

    private List<Long> roleIds;

    private Boolean isActive;
    private UUID companyId;
    private String companyName;
    private Long role;
    private List<UUID> siteIds;

    private List<UUID> deviceIds;

    private List<UUID> missionIds;
}