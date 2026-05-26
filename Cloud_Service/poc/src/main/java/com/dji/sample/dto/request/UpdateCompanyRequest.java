package com.dji.sample.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCompanyRequest {

    private String name;

    private String companyName;

    private String phoneNumber;

    private String email;

    private String address;

    private String description;

    private Boolean isActive;
}