package com.dji.sample.repository;

import com.dji.sample.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUserId(UUID userId);

    Optional<UserRole> findByUserIdAndRoleId(UUID userId, Integer roleId);

    void deleteByUserId(UUID userId);

    void deleteByUserIdAndRoleId(UUID userId, Integer roleId);

    boolean existsByUserIdAndRoleId(UUID userId, Integer roleId);
}