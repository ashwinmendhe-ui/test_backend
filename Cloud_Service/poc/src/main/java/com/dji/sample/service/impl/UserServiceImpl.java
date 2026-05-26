package com.dji.sample.service.impl;

import com.dji.sample.dto.request.CreateUserRequest;
import com.dji.sample.dto.request.UpdateUserRequest;
import com.dji.sample.dto.response.UserResponse;
import com.dji.sample.entity.Role;
import com.dji.sample.entity.User;
import com.dji.sample.entity.UserRole;
import com.dji.sample.repository.RoleRepository;
import com.dji.sample.repository.UserRepository;
import com.dji.sample.repository.UserRoleRepository;
import com.dji.sample.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.dji.sample.entity.Company;
import com.dji.sample.repository.CompanyRepository;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompanyRepository companyRepository;

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
        user.setCompanyId(request.getCompanyId());

        if (request.getCompanyId() != null) {
            Company company = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found"));

            user.setCompanyName(company.getCompanyName());
        } else {
            user.setCompanyName(request.getCompanyName());
        }
        user.setCompanyName(request.getCompanyName());
        user.setIsActive(
                request.getIsActive() != null ? request.getIsActive() : true
        );

        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );

        User savedUser = userRepository.save(user);

        saveUserRoles(savedUser, request.getRoleIds());

        return mapToResponse(savedUser);
    }

    @Override
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
        user.setCompanyId(request.getCompanyId());
        user.setCompanyId(request.getCompanyId());

        if (request.getCompanyId() != null) {
            Company company = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found"));

            user.setCompanyName(company.getCompanyName());
        } else {
            user.setCompanyName(request.getCompanyName());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        if (request.getPassword() != null &&
                !request.getPassword().isBlank()) {

            user.setPasswordHash(
                    passwordEncoder.encode(request.getPassword())
            );
        }

        User updatedUser = userRepository.save(user);

        userRoleRepository.deleteByUserId(updatedUser.getUserId());

        saveUserRoles(updatedUser, request.getRoleIds());

        return mapToResponse(updatedUser);
    }

    @Override
    public void deleteUser(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        userRoleRepository.deleteByUserId(user.getUserId());

        userRepository.delete(user);
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
    private UserResponse mapToResponse(User user) {

        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getUserId());
        List<Long> roleIds = new ArrayList<>();
        List<String> roleNames = new ArrayList<>();

        for (UserRole userRole : userRoles) {

            Role role = roleRepository.findById(userRole.getRoleId())
                    .orElse(null);

            if (role != null) {
                roleIds.add(role.getId().longValue());
                roleNames.add(role.getRoleKey());
            }
        }

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
            .companyId(user.getCompanyId())
            .companyName(user.getCompanyName())
            .isActive(user.getIsActive())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
        }
}