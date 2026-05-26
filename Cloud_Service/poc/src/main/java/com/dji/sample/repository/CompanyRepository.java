package com.dji.sample.repository;

import com.dji.sample.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByCompanyName(String companyName);

    boolean existsByCompanyName(String companyName);

    List<Company> findByCompanyNameContainingIgnoreCase(String keyword);
}