package com.dji.sample.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class CompanyResponse {

    private UUID companyId;

    private String companyName;

    private String description;

    private Boolean isActive;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
    private String name;

    private String phoneNumber;

    private String email;

    private String address;
}