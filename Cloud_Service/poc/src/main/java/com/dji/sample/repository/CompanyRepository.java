package com.dji.sample.repository;

import com.dji.sample.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, Integer> {

    Optional<Company> findByCompanyIdAndDeletedAtIsNull(UUID companyId);

    Optional<Company> findByNameAndDeletedAtIsNull(String name);

    boolean existsByNameAndDeletedAtIsNull(String name);

    List<Company> findByDeletedAtIsNull();

    List<Company> findByNameContainingIgnoreCaseAndDeletedAtIsNull(String keyword);
}