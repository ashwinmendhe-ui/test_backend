package com.dji.sample.repository;

import com.dji.sample.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SiteRepository extends JpaRepository<Site, Integer> {

    List<Site> findByDeletedAtIsNullOrderByCreatedAtDesc();

    List<Site> findByCompanyIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID companyId);

    List<Site> findByCompanyId(UUID companyId);

    Optional<Site> findBySiteIdAndDeletedAtIsNull(UUID siteId);

    boolean existsByNameIgnoreCaseAndCompanyIdAndDeletedAtIsNull(
            String name,
            UUID companyId
    );
}