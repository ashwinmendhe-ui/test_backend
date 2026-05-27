package com.dji.sample.controller;

import com.dji.sample.dto.request.CreateSiteRequest;
import com.dji.sample.dto.request.UpdateSiteRequest;
import com.dji.sample.dto.response.SiteResponse;
import com.dji.sample.service.SiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sites")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;

    @GetMapping("/search")
    public List<SiteResponse> searchSites(
            @RequestParam(required = false) UUID companyId
    ) {
        return siteService.searchSites(companyId);
    }

    @GetMapping("/{id}")
    public SiteResponse getSiteById(@PathVariable UUID id) {
        return siteService.getSiteById(id);
    }

    @GetMapping
    public List<SiteResponse> listSites(
            @RequestParam(required = false) UUID companyId
    ) {
        return siteService.searchSites(companyId);
    }

    @PostMapping
    public SiteResponse createSite(@RequestBody CreateSiteRequest request) {
        return siteService.createSite(request);
    }

    @PostMapping("/update/{id}")
    public SiteResponse updateSite(
            @PathVariable UUID id,
            @RequestBody UpdateSiteRequest request
    ) {
        return siteService.updateSite(id, request);
    }

    @PostMapping("/delete/{id}")
    public void deleteSite(@PathVariable UUID id) {
        siteService.deleteSite(id);
    }
}