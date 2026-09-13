package com.dji.sample.service.impl;

import com.dji.sample.dto.request.CreateUserRequest;
import com.dji.sample.dto.request.UpdateUserRequest;
import com.dji.sample.dto.response.UserResponse;
import com.dji.sample.dto.response.UserSiteResponse;
import com.dji.sample.entity.Company;
import com.dji.sample.entity.Role;
import com.dji.sample.entity.Site;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.dji.sample.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.dji.sample.dto.request.ChangePasswordRequest;

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

        User currentUser = getCurrentUser();
        List<User> users;

        if (isSysAdmin(currentUser)) {

            if (keyword == null || keyword.isBlank()) {
                users = userRepository.findByDeletedAtIsNull();
            } else {
                users = userRepository
                        .findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseAndDeletedAtIsNull(
                                keyword,
                                keyword
                        );
            }

        } else {

            UUID companyId = currentUser.getCompanyId();

            if (companyId == null) {
                return List.of();
            }

            users = userRepository.findByCompanyIdAndDeletedAtIsNull(companyId);

            // Company-level users must never see SYS_ADMIN accounts.
            users = users.stream()
                    .filter(user -> !isSysAdmin(user))
                    .toList();

            if (keyword != null && !keyword.isBlank()) {
                String lower = keyword.toLowerCase();

                users = users.stream()
                        .filter(user ->
                                (user.getUsername() != null &&
                                        user.getUsername().toLowerCase().contains(lower))
                                        ||
                                (user.getEmail() != null &&
                                        user.getEmail().toLowerCase().contains(lower)))
                        .toList();
            }
        }

        return users.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public UserResponse getUserById(UUID userId) {
        User currentUser = getCurrentUser();

        User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        validateUserAccess(currentUser, user);

        return mapToResponse(user);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        User currentUser = getCurrentUser();

        List<Long> requestedRoleIds =
                resolveRoleIds(request.getRoleIds(), request.getRole());

        if (!isSysAdmin(currentUser)) {
            if (containsSysAdminRole(requestedRoleIds)) {
                throw new AccessDeniedException(
                        "Only System Administrator can assign SYS_ADMIN role"
                );
            }

            UUID currentCompanyId = currentUser.getCompanyId();

            if (request.getCompanyId() == null ||
                    !request.getCompanyId().equals(currentCompanyId)) {
                throw new AccessDeniedException(
                        "You cannot create a user for another company"
                );
            }
        }

        User user = new User();

        String username = request.getUsername() != null ? request.getUsername().trim() : "";
            String email = request.getEmail() != null ? request.getEmail().trim() : "";

            if (username.isBlank()) {
                throw new RuntimeException("Username is required");
            }

            if (email.isBlank()) {
                throw new RuntimeException("Email is required");
            }

            if (userRepository.existsByUsernameAndDeletedAtIsNull(username)) {
                throw new RuntimeException("Username already exists");
            }

            if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
                throw new RuntimeException("Email already exists");
            }

            user.setUsername(username);
            user.setEmail(email);
        user.setFullName(
                request.getFullName() != null && !request.getFullName().isBlank()
                        ? request.getFullName()
                        : request.getUsername()
        );

        user.setPhone(request.getPhone());
        user.setDescription(request.getDescription());

        applyCompanyToUser(user, request.getCompanyId(), request.getCompanyName());

        user.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);

        saveUserRoles(savedUser, requestedRoleIds);

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
        User currentUser = getCurrentUser();

        User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        validateUserAccess(currentUser, user);

        List<Long> requestedRoleIds =
                resolveRoleIds(request.getRoleIds(), request.getRole());

        if (!isSysAdmin(currentUser)) {
            if (containsSysAdminRole(requestedRoleIds)) {
                throw new AccessDeniedException(
                        "Only System Administrator can assign SYS_ADMIN role"
                );
            }

            UUID currentCompanyId = currentUser.getCompanyId();

            if (request.getCompanyId() != null &&
                    !request.getCompanyId().equals(currentCompanyId)) {
                throw new AccessDeniedException(
                        "You cannot assign a user to another company"
                );
            }
        }

        String username = request.getUsername() != null ? request.getUsername().trim() : "";
        String email = request.getEmail() != null ? request.getEmail().trim() : "";

        if (username.isBlank()) {
            throw new RuntimeException("Username is required");
        }

        if (email.isBlank()) {
            throw new RuntimeException("Email is required");
        }

        userRepository.findByUsernameAndDeletedAtIsNull(username)
                .filter(existing -> !existing.getUserId().equals(userId))
                .ifPresent(existing -> {
                    throw new RuntimeException("Username already exists");
                });

        userRepository.findByEmailAndDeletedAtIsNull(email)
                .filter(existing -> !existing.getUserId().equals(userId))
                .ifPresent(existing -> {
                    throw new RuntimeException("Email already exists");
                });

        user.setUsername(username);
        user.setEmail(email);

        user.setFullName(
                request.getFullName() != null && !request.getFullName().isBlank()
                        ? request.getFullName()
                        : username
        );

        user.setPhone(request.getPhone());
        user.setDescription(request.getDescription());

        applyCompanyToUser(user, request.getCompanyId(), request.getCompanyName());

        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        User updatedUser = userRepository.save(user);

        userRoleRepository.deleteByUserId(updatedUser.getUserId());
        userRoleRepository.flush();

        saveUserRoles(updatedUser, requestedRoleIds);

        updateUserAssignments(
                updatedUser,
                request.getSiteIds(),
                request.getMissionIds(),
                request.getDeviceIds()
        );

        User saved = userRepository.save(updatedUser);

        return mapToResponse(saved);
    }
    
    @Override
@Transactional
public void changePassword(UUID userId, ChangePasswordRequest request) {
    User currentUser = getCurrentUser();

    User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

    validateUserAccess(currentUser, user);

    String currentPassword =
            request.getCurrentPassword() != null
                    ? request.getCurrentPassword()
                    : "";

    String newPassword =
            request.getNewPassword() != null
                    ? request.getNewPassword()
                    : "";

    String confirmPassword =
            request.getConfirmPassword() != null
                    ? request.getConfirmPassword()
                    : "";

    if (currentPassword.isBlank()) {
        throw new RuntimeException("Current password is required");
    }

    if (newPassword.isBlank()) {
        throw new RuntimeException("New password is required");
    }

    if (confirmPassword.isBlank()) {
        throw new RuntimeException("Confirm password is required");
    }

    if (!newPassword.equals(confirmPassword)) {
        throw new RuntimeException(
                "New password and confirm password do not match"
        );
    }

    if (!passwordEncoder.matches(
            currentPassword,
            user.getPasswordHash()
    )) {
        throw new RuntimeException("Current password is incorrect");
    }

    if (passwordEncoder.matches(
            newPassword,
            user.getPasswordHash()
    )) {
        throw new RuntimeException(
                "New password must be different from the current password"
        );
    }

    user.setPasswordHash(passwordEncoder.encode(newPassword));

    userRepository.save(user);
}
    
    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        User currentUser = getCurrentUser();

        User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        validateUserAccess(currentUser, user);

        userRoleRepository.deleteByUserId(user.getUserId());

        user.setDeletedAt(OffsetDateTime.now());
        user.setIsActive(false);

        userRepository.save(user);
    }

    private void applyCompanyToUser(User user, UUID companyId, String companyName) {
        user.setCompanyId(companyId);

        if (companyId != null) {
            companyRepository.findByCompanyIdAndDeletedAtIsNull(companyId)
                    .orElseThrow(() -> new RuntimeException("Company not found"));
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

            Role role = roleRepository.findById(roleIdValue).orElse(null);

            if (role != null) {
                roleIds.add(role.getId().longValue());
                roleNames.add(role.getRoleKey());
            }
        }

        List<UUID> siteIds = user.getSites().stream()
                .filter(site -> site.getDeletedAt() == null)
                .map(Site::getSiteId)
                .toList();

        List<UUID> missionIds = user.getMissions().stream()
                .filter(mission -> mission.getDeletedAt() == null)
                .map(mission -> mission.getMissionId())
                .toList();

        List<UUID> deviceIds = user.getDevices().stream()
                .filter(device -> device.getDeletedAt() == null)
                .map(device -> device.getDeviceId())
                .toList();

        List<UserSiteResponse> sites = user.getSites().stream()
                .filter(site -> site.getDeletedAt() == null)
                .map(site -> UserSiteResponse.builder()
                        .siteId(site.getSiteId())
                        .siteName(site.getName())
                        .createdAt(formatKst(site.getCreatedAt()))
                        .missionList(
                                user.getMissions().stream()
                                        .filter(mission -> mission.getDeletedAt() == null)
                                        .filter(mission -> mission.getSiteId() != null
                                                && mission.getSiteId().equals(site.getSiteId()))
                                        .map(mission -> mission.getMissionId())
                                        .toList()
                        )
                        .deviceList(
                                user.getDevices().stream()
                                        .filter(device -> device.getDeletedAt() == null)
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
                .name(user.getFullName() != null ? user.getFullName() : user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .description(user.getDescription())
                .roleIds(roleIds)
                .roleNames(roleNames)
                .role(!roleIds.isEmpty() ? roleIds.get(0) : null)
                .roles(roleIds)
                .companyId(user.getCompanyId())
                .companyName(resolveCompanyName(user))
                .company(user.getCompanyId() != null ? user.getCompanyId().toString() : null)
                .companyIds(user.getCompanyId() != null ? List.of(user.getCompanyId()) : List.of())
                .companies(user.getCompanyId() != null ? List.of(user.getCompanyId().toString()) : List.of())
                .isActive(user.getDeletedAt() == null && Boolean.TRUE.equals(user.getIsActive()))
                .createdAt(formatKst(user.getCreatedAt()))
                .updatedAt(formatKst(user.getUpdatedAt()))
                .siteIds(siteIds)
                .missionIds(missionIds)
                .deviceIds(deviceIds)
                .sites(sites)
                .build();
    }

    private String resolveCompanyName(User user) {
        if (user.getCompanyId() == null) {
            return null;
        }

        return companyRepository.findByCompanyIdAndDeletedAtIsNull(user.getCompanyId())
                .map(Company::getName)
                .orElse(null);
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

private boolean containsSysAdminRole(List<Long> roleIds) {
    return roleIds != null &&
            roleIds.stream()
                    .anyMatch(roleId -> roleId != null && roleId == 1L);
}

private void validateUserAccess(User currentUser, User targetUser) {

    // SYS_ADMIN has full access.
    if (isSysAdmin(currentUser)) {
        return;
    }

    // Company-level users must never access a SYS_ADMIN account.
    if (isSysAdmin(targetUser)) {
        throw new AccessDeniedException(
                "You do not have permission to access this user"
        );
    }

    UUID currentCompanyId = currentUser.getCompanyId();
    UUID targetCompanyId = targetUser.getCompanyId();

    // Company-level users can only access users in their own company.
    if (currentCompanyId == null ||
            targetCompanyId == null ||
            !currentCompanyId.equals(targetCompanyId)) {
        throw new AccessDeniedException(
                "You do not have permission to access this user"
        );
    }
}

    private void updateUserAssignments(User user, List<UUID> siteIds, List<UUID> missionIds, List<UUID> deviceIds) {
        user.getSites().clear();
        user.getMissions().clear();
        user.getDevices().clear();

        if (siteIds != null && !siteIds.isEmpty()) {
            user.getSites().addAll(
                    siteIds.stream()
                            .map(id -> siteRepository.findBySiteIdAndDeletedAtIsNull(id)
                                    .orElseThrow(() -> new RuntimeException("Site not found: " + id)))
                            .toList()
            );
        }

        if (missionIds != null && !missionIds.isEmpty()) {
            user.getMissions().addAll(
                    missionIds.stream()
                            .map(id -> missionRepository.findByMissionIdAndDeletedAtIsNull(id)
                                    .orElseThrow(() -> new RuntimeException("Mission not found: " + id)))
                            .toList()
            );
        }

        if (deviceIds != null && !deviceIds.isEmpty()) {
            user.getDevices().addAll(
                    deviceIds.stream()
                            .map(id -> deviceRepository.findByDeviceIdAndDeletedAtIsNull(id)
                                    .orElseThrow(() -> new RuntimeException("Device not found: " + id)))
                            .toList()
            );
        }
    }
}