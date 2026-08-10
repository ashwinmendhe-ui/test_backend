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
import com.dji.sample.repository.UserRoleRepository;
import com.dji.sample.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final SiteRepository siteRepository;
    private final MissionRepository missionRepository;
    private final DeviceRepository deviceRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
public List<CompanyResponse> searchCompanies(String keyword) {
    User currentUser = getCurrentUser();
    List<Company> companies;

    if (isSysAdmin(currentUser)) {
        companies = (keyword == null || keyword.isBlank())
                ? companyRepository.findByDeletedAtIsNull()
                : companyRepository.findByNameContainingIgnoreCaseAndDeletedAtIsNull(keyword.trim());
    } else {
        UUID companyId = currentUser.getCompanyId();

        companies = companyId == null
                ? List.of()
                : companyRepository.findByCompanyIdAndDeletedAtIsNull(companyId)
                    .map(List::of)
                    .orElse(List.of());

        if (keyword != null && !keyword.isBlank()) {
            String lower = keyword.trim().toLowerCase();
            companies = companies.stream()
                    .filter(company -> company.getName() != null
                            && company.getName().toLowerCase().contains(lower))
                    .toList();
        }
    }

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

    OffsetDateTime now = OffsetDateTime.now();

    // 1. Soft-delete users under the company
    List<User> users = userRepository.findByCompanyIdAndDeletedAtIsNull(id);

    for (User user : users) {
        // Same behavior as UserServiceImpl.deleteUser()
        userRoleRepository.deleteByUserId(user.getUserId());

        user.setDeletedAt(now);
        user.setIsActive(false);

        // Keep companyId.
        // Do NOT set companyId to null because DB column is NOT NULL.
        userRepository.save(user);
    }

    // 2. Soft-delete sites under the company
    List<Site> sites =
            siteRepository.findByCompanyIdAndDeletedAtIsNullOrderByCreatedAtDesc(id);

    for (Site site : sites) {
        site.setDeletedAt(now);

        // Keep companyId for historical/reference integrity.
        siteRepository.save(site);
    }

    // 3. Soft-delete missions under the company
    List<Mission> missions =
            missionRepository.findByCompanyIdAndDeletedAtIsNull(id);

    for (Mission mission : missions) {
        mission.setDeletedAt(now);

        // Keep companyId and siteId.
        missionRepository.save(mission);
    }

    // 4. Soft-delete devices under the company
    List<Device> devices =
            deviceRepository.findByCompany_CompanyIdAndDeletedAtIsNullOrderByCreatedAtDesc(id);

    for (Device device : devices) {
        device.setDeletedAt(now);
        device.setStatus("INACTIVE");

        // Keep company and site relationship.
        deviceRepository.save(device);
    }

    // 5. Soft-delete the company itself
    company.setDeletedAt(now);
    company.setStatus("INACTIVE");

    companyRepository.save(company);
}
    private User getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null ||
            !(authentication.getPrincipal() instanceof CustomUserDetails customUserDetails)) {
        throw new RuntimeException("Authenticated user not found");
    }

    return userRepository.findByUserIdAndDeletedAtIsNull(customUserDetails.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));
}

private boolean isSysAdmin(User user) {
    return userRoleRepository.existsByUserIdAndRoleId(user.getUserId(), 1);
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