package com.dji.sample.service;

import com.dji.sample.dto.request.MissionRequest;
import com.dji.sample.dto.response.MissionResponse;

import java.util.List;
import java.util.UUID;

public interface MissionService {

    List<MissionResponse> search(String keyword, String from, String to);

    List<MissionResponse> list(String companyId, String siteId);

    MissionResponse getById(UUID id);

    MissionResponse create(MissionRequest request);

    MissionResponse update(UUID id, MissionRequest request);

    void delete(UUID id);
}