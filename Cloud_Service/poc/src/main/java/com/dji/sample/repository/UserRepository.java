package com.dji.sample.repository;

import com.dji.sample.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);
    List<User> findByCompanyId(UUID companyId);

    boolean existsByEmail(String email); 
    List<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String username,
            String email
    );
}