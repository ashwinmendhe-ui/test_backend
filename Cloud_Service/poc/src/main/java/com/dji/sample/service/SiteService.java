package com.dji.sample.service;

import com.dji.sample.dto.request.CreateSiteRequest;
import com.dji.sample.dto.request.UpdateSiteRequest;
import com.dji.sample.dto.response.SiteResponse;

import java.util.List;
import java.util.UUID;

public interface SiteService {

    List<SiteResponse> searchSites(UUID companyId);

    SiteResponse getSiteById(UUID id);

    SiteResponse createSite(CreateSiteRequest request);

    SiteResponse updateSite(UUID id, UpdateSiteRequest request);

    void deleteSite(UUID id);
}