package com.dji.sample.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateSiteRequest {

    private UUID companyId;

    private String name;

    private String address;

    private String description;

    private Boolean isActive;

    private String phoneNumber;

    private String email;
}