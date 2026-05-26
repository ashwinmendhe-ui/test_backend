package com.dji.sample.controller;

import com.dji.sample.dto.request.CreateCompanyRequest;
import com.dji.sample.dto.request.UpdateCompanyRequest;
import com.dji.sample.dto.response.ApiResponse;
import com.dji.sample.dto.response.CompanyResponse;
import com.dji.sample.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping("/search")
    public List<CompanyResponse> searchCompanies(
            @RequestParam(required = false) String keyword
    ) {
        return companyService.searchCompanies(keyword);
    }

    @GetMapping("/{id}")
    public CompanyResponse getCompanyById(@PathVariable UUID id) {
        return companyService.getCompanyById(id);
    }

    @PostMapping
    public ApiResponse<CompanyResponse> createCompany(@RequestBody CreateCompanyRequest request) {
        return ApiResponse.<CompanyResponse>builder()
                .success(true)
                .message("Company created successfully")
                .data(companyService.createCompany(request))
                .build();
    }

    @PostMapping("/update/{id}")
    public ApiResponse<CompanyResponse> updateCompany(
            @PathVariable UUID id,
            @RequestBody UpdateCompanyRequest request
    ) {
        return ApiResponse.<CompanyResponse>builder()
                .success(true)
                .message("Company updated successfully")
                .data(companyService.updateCompany(id, request))
                .build();
    }

    @PostMapping("/delete/{id}")
    public ApiResponse<Void> deleteCompany(@PathVariable UUID id) {
        companyService.deleteCompany(id);

        return ApiResponse.<Void>builder()
                .success(true)
                .message("Company deleted successfully")
                .data(null)
                .build();
    }
}