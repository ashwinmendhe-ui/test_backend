package com.dji.sample.service.impl;

import com.dji.sample.dto.request.CreateUserRequest;
import com.dji.sample.dto.request.UpdateUserRequest;
import com.dji.sample.dto.response.UserResponse;
import com.dji.sample.dto.response.UserSiteResponse;
import com.dji.sample.entity.Site;
import com.dji.sample.entity.Company;
import com.dji.sample.entity.Role;
import com.dji.sample.entity.User;
import com.dji.sample.entity.UserRole;
import com.dji.sample.repository.CompanyRepository;
import com.dji.sample.repository.DeviceRepository;
import com.dji.sample.repository.MissionRepository;
import com.dji.sample.repository.RoleRepository;
import com.dji.sample.repository.SiteRepository;
import com.dji.sample.repository.UserRepository;
import com.dji.sample.repository.UserRoleRepository;
import com.dji.sample.service.UserService;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final SiteRepository siteRepository;
    private final MissionRepository missionRepository;
    private final DeviceRepository deviceRepository;

    @Override
    public List<UserResponse> searchUsers(String keyword) {
        List<User> users;

        if (keyword == null || keyword.isBlank()) {
            users = userRepository.findAll();
        } else {
            users = userRepository
                    .findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                            keyword,
                            keyword
                    );
        }

        List<UserResponse> responses = new ArrayList<>();

        for (User user : users) {
            responses.add(mapToResponse(user));
        }

        return responses;
    }

    @Override
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return mapToResponse(user);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        user.setFullName(
                request.getFullName() != null && !request.getFullName().isBlank()
                        ? request.getFullName()
                        : request.getUsername()
        );

        user.setPhone(request.getPhone());
        user.setDescription(request.getDescription());

        applyCompanyToUser(user, request.getCompanyId(), request.getCompanyName());

        user.setIsActive(
                request.getIsActive() != null ? request.getIsActive() : true
        );

        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );

        User savedUser = userRepository.save(user);

        saveUserRoles(savedUser, resolveRoleIds(request.getRoleIds(), request.getRole()));
        updateUserAssignments(
                savedUser,
                request.getSiteIds(),
                request.getMissionIds(),
                request.getDeviceIds()
        );
        return mapToResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        user.setFullName(
                request.getFullName() != null && !request.getFullName().isBlank()
                        ? request.getFullName()
                        : request.getUsername()
        );

        user.setPhone(request.getPhone());
        user.setDescription(request.getDescription());

        applyCompanyToUser(user, request.getCompanyId(), request.getCompanyName());

        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(
                    passwordEncoder.encode(request.getPassword())
            );
        }

        User updatedUser = userRepository.save(user);

        userRoleRepository.deleteByUserId(updatedUser.getUserId());
        userRoleRepository.flush();

        saveUserRoles(updatedUser, resolveRoleIds(request.getRoleIds(), request.getRole()));
        updateUserAssignments(
                updatedUser,
                request.getSiteIds(),
                request.getMissionIds(),
                request.getDeviceIds()
        );

        userRepository.save(updatedUser);
                
        return mapToResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        userRoleRepository.deleteByUserId(user.getUserId());

        userRepository.delete(user);
    }

    private void applyCompanyToUser(User user, UUID companyId, String companyName) {
        user.setCompanyId(companyId);

        if (companyId != null) {
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new RuntimeException("Company not found"));

            user.setCompanyName(company.getCompanyName());
        } else {
            user.setCompanyName(companyName);
        }
    }

    private List<Long> resolveRoleIds(List<Long> roleIds, Long role) {
        if (roleIds != null && !roleIds.isEmpty()) {
            return roleIds;
        }

        if (role != null) {
            return List.of(role);
        }

        return List.of();
    }

    private void saveUserRoles(User user, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }

        for (Long roleId : roleIds) {
            Role role = roleRepository.findById(roleId.intValue())
                    .orElseThrow(() -> new RuntimeException("Role not found"));

            UserRole userRole = new UserRole();

            userRole.setUserId(user.getUserId());
            userRole.setRoleId(role.getId());

            userRoleRepository.save(userRole);
        }
    }

    private String formatKst(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        return dateTime
                .atZoneSameInstant(java.time.ZoneId.of("Asia/Seoul"))
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private UserResponse mapToResponse(User user) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getUserId());
        List<Long> roleIds = new ArrayList<>();
        List<String> roleNames = new ArrayList<>();

        for (UserRole userRole : userRoles) {
                Integer roleIdValue = userRole.getRoleId();

                if (roleIdValue == null) {
                    continue;
                }

                int roleId = roleIdValue;

                Role role = roleRepository.findById(roleId)
                        .orElse(null);

                if (role != null) {
                    roleIds.add(role.getId().longValue());
                    roleNames.add(role.getRoleKey());
                }
            }

        List<UUID> siteIds = user.getSites().stream()
                .map(Site::getSiteId)
                .toList();

        List<UUID> missionIds = user.getMissions().stream()
                .map(mission -> mission.getMissionId())
                .toList();

        List<UUID> deviceIds = user.getDevices().stream()
                .map(device -> device.getDeviceId())
                .toList();

        List<UserSiteResponse> sites = user.getSites().stream()
                .map(site -> UserSiteResponse.builder()
                        .siteId(site.getSiteId())
                        .siteName(site.getName())
                        .createdAt(formatKst(site.getCreatedAt()))
                        .missionList(
                                user.getMissions().stream()
                                        .filter(mission -> mission.getSiteId() != null
                                            && mission.getSiteId().equals(site.getSiteId()))
                                        .map(mission -> mission.getMissionId())
                                        .toList()
                        )
                        .deviceList(
                                user.getDevices().stream()
                                        .filter(device -> device.getSite() != null
                                                && device.getSite().getSiteId().equals(site.getSiteId()))
                                        .map(device -> device.getDeviceId())
                                        .toList()
                        )
                        .build())
                .toList();

        



        return UserResponse.builder()
                .userId(user.getUserId())
                .id(user.getUserId())
                .username(user.getUsername())
                .name(
                        user.getFullName() != null
                                ? user.getFullName()
                                : user.getUsername()
                )
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .description(user.getDescription())
                .roleIds(roleIds)
                .roleNames(roleNames)
                .role(roleIds != null && !roleIds.isEmpty() ? roleIds.get(0) : null)
                .roles(roleIds)

                .companyId(user.getCompanyId())
                .companyName(user.getCompanyName())

                .company(
                        user.getCompanyId() != null
                                ? user.getCompanyId().toString()
                                : null
                )

                .companyIds(
                        user.getCompanyId() != null
                                ? List.of(user.getCompanyId())
                                : List.of()
                )
                .companies(
                        user.getCompanyId() != null
                                ? List.of(user.getCompanyId().toString())
                                : List.of()
                )
                .isActive(user.getIsActive())
                .createdAt(formatKst(user.getCreatedAt()))
                .updatedAt(formatKst(user.getUpdatedAt()))
                .siteIds(siteIds)
                .missionIds(missionIds)
                .deviceIds(deviceIds)
                .sites(sites)
                .build();
    }

    private void updateUserAssignments(User user, List<UUID> siteIds, List<UUID> missionIds, List<UUID> deviceIds) {
        user.getSites().clear();
        user.getMissions().clear();
        user.getDevices().clear();

        if (siteIds != null && !siteIds.isEmpty()) {
            user.getSites().addAll(siteRepository.findAllById(siteIds));
        }

        if (missionIds != null && !missionIds.isEmpty()) {
            user.getMissions().addAll(missionRepository.findAllById(missionIds));
        }

        if (deviceIds != null && !deviceIds.isEmpty()) {
            user.getDevices().addAll(deviceRepository.findAllById(deviceIds));
        }
    }


}