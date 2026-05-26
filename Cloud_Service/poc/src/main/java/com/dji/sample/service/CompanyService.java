package com.dji.sample.service;

import com.dji.sample.dto.request.CreateCompanyRequest;
import com.dji.sample.dto.request.UpdateCompanyRequest;
import com.dji.sample.dto.response.CompanyResponse;

import java.util.List;
import java.util.UUID;

public interface CompanyService {

    List<CompanyResponse> searchCompanies(String keyword);

    CompanyResponse getCompanyById(UUID id);

    CompanyResponse createCompany(CreateCompanyRequest request);

    CompanyResponse updateCompany(UUID id, UpdateCompanyRequest request);

    void deleteCompany(UUID id);
}