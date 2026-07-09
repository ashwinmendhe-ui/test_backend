package com.dji.sample.repository;

import com.dji.sample.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUserIdAndDeletedAtIsNull(UUID userId);

    Optional<User> findByUsernameAndDeletedAtIsNull(String username);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByUsernameAndDeletedAtIsNull(String username);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    List<User> findByCompanyIdAndDeletedAtIsNull(UUID companyId);

    List<User> findByDeletedAtIsNull();

    List<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseAndDeletedAtIsNull(
            String username,
            String email
    );
    List<User> findByCompanyIdAndIsActiveTrueAndDeletedAtIsNull(UUID companyId);
    long countByDeletedAtIsNull();

    long countByCompanyIdAndDeletedAtIsNull(UUID companyId);
}