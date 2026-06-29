package com.dji.sample.service.impl;

import com.dji.sample.dto.request.CreateCompanyRequest;
import com.dji.sample.dto.request.UpdateCompanyRequest;
import com.dji.sample.dto.response.CompanyResponse;
import com.dji.sample.entity.Company;
import com.dji.sample.entity.Mission;
import com.dji.sample.entity.Site;
import com.dji.sample.entity.User;
import com.dji.sample.repository.CompanyRepository;
import com.dji.sample.repository.MissionRepository;
import com.dji.sample.repository.SiteRepository;
import com.dji.sample.repository.UserRepository;
import com.dji.sample.entity.Device;
import com.dji.sample.repository.DeviceRepository;
import com.dji.sample.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final DeviceRepository deviceRepository;

    @Override
    public List<CompanyResponse> searchCompanies(String keyword) {
        List<Company> companies = (keyword == null || keyword.isBlank())
                ? companyRepository.findByDeletedAtIsNull()
                : companyRepository.findByNameContainingIgnoreCaseAndDeletedAtIsNull(keyword.trim());

        return companies.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public CompanyResponse getCompanyById(UUID id) {
        Company company = companyRepository.findByCompanyIdAndDeletedAtIsNull(id)
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

        if (companyRepository.existsByNameAndDeletedAtIsNull(companyName)) {
            throw new RuntimeException("Company name already exists");
        }

        Company company = Company.builder()
                .name(companyName)
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .address(request.getAddress())
                .description(request.getDescription())
                .status(request.getIsActive() != null && !request.getIsActive() ? "INACTIVE" : "ACTIVE")
                .build();

        return mapToResponse(companyRepository.save(company));
    }

    @Override
    public CompanyResponse updateCompany(UUID id, UpdateCompanyRequest request) {
        Company company = companyRepository.findByCompanyIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        String companyName = request.getCompanyName() != null
                ? request.getCompanyName()
                : request.getName();

        if (companyName != null && !companyName.isBlank()) {
            company.setName(companyName.trim());
        }

        company.setPhoneNumber(request.getPhoneNumber());
        company.setEmail(request.getEmail());
        company.setAddress(request.getAddress());
        company.setDescription(request.getDescription());

        if (request.getIsActive() != null) {
            company.setStatus(request.getIsActive() ? "ACTIVE" : "INACTIVE");
        }

        return mapToResponse(companyRepository.save(company));
    }

    @Override
    @Transactional
    public void deleteCompany(UUID id) {
        Company company = companyRepository.findByCompanyIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        List<User> users = userRepository.findByCompanyIdAndDeletedAtIsNull(id);
        for (User user : users) {
            user.setCompanyId(null);
            userRepository.save(user);
        }

        List<Site> sites = siteRepository.findByCompanyIdAndDeletedAtIsNullOrderByCreatedAtDesc(id);
        for (Site site : sites) {
            site.setCompanyId(null);
            siteRepository.save(site);
        }

        List<Mission> missions = missionRepository.findByCompanyIdAndDeletedAtIsNull(id);
        for (Mission mission : missions) {
            mission.setCompanyId(null);
            mission.setSiteId(null);
            missionRepository.save(mission);
        }

        List<Device> devices =
        deviceRepository.findByCompany_CompanyIdAndDeletedAtIsNullOrderByCreatedAtDesc(id);

        for (Device device : devices) {
            device.setCompany(null);
            device.setSite(null);
            device.setStatus("INACTIVE");
            deviceRepository.save(device);
        }

        company.setDeletedAt(OffsetDateTime.now());
        company.setStatus("INACTIVE");
        companyRepository.save(company);
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
        boolean active = company.getDeletedAt() == null
                && !"INACTIVE".equalsIgnoreCase(company.getStatus());

        return CompanyResponse.builder()
                .companyId(company.getCompanyId())
                .name(company.getName())
                .companyName(company.getName())
                .phoneNumber(company.getPhoneNumber())
                .email(company.getEmail())
                .address(company.getAddress())
                .description(company.getDescription())
                .status(company.getStatus())
                .isActive(active)
                .createdAt(formatKst(company.getCreatedAt()))
                .updatedAt(formatKst(company.getUpdatedAt()))
                .build();
    }
}