package com.dji.sample.service.impl;

import com.dji.sample.dto.request.CreateCompanyRequest;
import com.dji.sample.dto.request.UpdateCompanyRequest;
import com.dji.sample.dto.response.CompanyResponse;
import com.dji.sample.entity.Company;
import com.dji.sample.entity.Site;
import com.dji.sample.entity.User;
import com.dji.sample.repository.CompanyRepository;
import com.dji.sample.repository.SiteRepository;
import com.dji.sample.repository.UserRepository;
import com.dji.sample.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dji.sample.entity.Mission;
import com.dji.sample.repository.MissionRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final SiteRepository siteRepository;
    private final MissionRepository missionRepository;

    @Override
    public List<CompanyResponse> searchCompanies(String keyword) {
        List<Company> companies;

        if (keyword == null || keyword.isBlank()) {
            companies = companyRepository.findAll();
        } else {
            companies = companyRepository.findByCompanyNameContainingIgnoreCase(keyword);
        }

        return companies.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public CompanyResponse getCompanyById(UUID id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        return mapToResponse(company);
    }

    @Override
    public CompanyResponse createCompany(CreateCompanyRequest request) {
        String companyName = request.getCompanyName() != null
                ? request.getCompanyName()
                : request.getName();

        if (companyName == null || companyName.isBlank()) {
            throw new RuntimeException("Company name is required");
        }

        companyName = companyName.trim();

        if (companyRepository.existsByCompanyName(companyName)) {
            throw new RuntimeException("Company name already exists");
        }

        Company company = Company.builder()
                .companyName(companyName)
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .address(request.getAddress())
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return mapToResponse(companyRepository.save(company));
    }

    @Override
    public CompanyResponse updateCompany(UUID id, UpdateCompanyRequest request) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        String companyName = request.getCompanyName() != null
                ? request.getCompanyName()
                : request.getName();

        if (companyName != null && !companyName.isBlank()) {
            company.setCompanyName(companyName.trim());
        }

        company.setPhoneNumber(request.getPhoneNumber());
        company.setEmail(request.getEmail());
        company.setAddress(request.getAddress());
        company.setDescription(request.getDescription());

        if (request.getIsActive() != null) {
            company.setIsActive(request.getIsActive());
        }

        return mapToResponse(companyRepository.save(company));
    }

    @Override
    @Transactional
    public void deleteCompany(UUID id) {

    Company company = companyRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Company not found"));

    // cleanup users linked to company
    List<User> users = userRepository.findByCompanyId(id);
    for (User user : users) {
        user.setCompanyId(null);
        user.setCompanyName(null);
        userRepository.save(user);
    }

    // cleanup sites linked to company
    List<Site> sites = siteRepository.findByCompanyIdAndIsActiveTrueOrderByCreatedAtDesc(id);
    for (Site site : sites) {
        site.setCompanyId(null);
        site.setCompanyName(null);
        siteRepository.save(site);
    }

    // cleanup missions linked to company
    List<Mission> missions = missionRepository.findByCompanyIdAndIsActiveTrue(id);
    for (Mission mission : missions) {
        mission.setCompanyId(null);
        mission.setCompanyName(null);
        mission.setSiteId(null);
        mission.setSiteName(null);
        missionRepository.save(mission);
    }

    companyRepository.delete(company);
}
    


private String formatKst(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        return dateTime
                .atZoneSameInstant(java.time.ZoneId.of("Asia/Seoul"))
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private CompanyResponse mapToResponse(Company company) {
        return CompanyResponse.builder()
                .companyId(company.getCompanyId())
                .name(company.getCompanyName())
                .companyName(company.getCompanyName())
                .phoneNumber(company.getPhoneNumber())
                .email(company.getEmail())
                .address(company.getAddress())
                .description(company.getDescription())
                .isActive(company.getIsActive())
                .createdAt(formatKst(company.getCreatedAt()))
                .updatedAt(formatKst(company.getUpdatedAt()))
                .build();
    }
}