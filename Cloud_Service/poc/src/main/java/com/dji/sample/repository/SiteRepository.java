package com.dji.sample.repository;

import com.dji.sample.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, UUID> {

    List<Site> findByIsActiveTrueOrderByCreatedAtDesc();

    List<Site> findByCompanyIdAndIsActiveTrueOrderByCreatedAtDesc(UUID companyId);
    List<Site> findByCompanyId(UUID companyId);
    Optional<Site> findBySiteId(UUID siteId);
    boolean existsByNameIgnoreCaseAndCompanyIdAndIsActiveTrue(
            String name,
            UUID companyId
    );
}