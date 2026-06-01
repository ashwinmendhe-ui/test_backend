package com.dji.sample.controller;

import com.dji.sample.dto.request.MissionRequest;
import com.dji.sample.dto.response.ApiResponse;
import com.dji.sample.dto.response.MissionResponse;
import com.dji.sample.service.MissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;

    @GetMapping("/search")
    public List<MissionResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        return missionService.search(keyword, from, to);
    }

    @GetMapping
    public List<MissionResponse> list(
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String siteId
    ) {
        return missionService.list(companyId, siteId);
    }

    @GetMapping("/{id}")
    public MissionResponse getById(@PathVariable UUID id) {
        return missionService.getById(id);
    }
        @PostMapping
        public ApiResponse<MissionResponse> create(@RequestBody MissionRequest request) {
            return ApiResponse.<MissionResponse>builder()
                    .success(true)
                    .message("Mission created successfully")
                    .data(missionService.create(request))
                    .build();
        }

    @PostMapping("/update/{id}")
    public ApiResponse<MissionResponse> update(
            @PathVariable UUID id,
            @RequestBody MissionRequest request
    ) {
        return ApiResponse.<MissionResponse>builder()
                .success(true)
                .message("Mission updated successfully")
                .data(missionService.update(id, request))
                .build();
    }

    @PostMapping("/delete/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        missionService.delete(id);

        return ApiResponse.<Void>builder()
                .success(true)
                .message("Mission deleted successfully")
                .data(null)
                .build();
    }
}