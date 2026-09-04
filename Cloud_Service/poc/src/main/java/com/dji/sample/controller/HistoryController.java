package com.dji.sample.controller;

import com.dji.sample.dto.request.CreateHistoryRequest;
import com.dji.sample.dto.response.HistoryDetailResponse;
import com.dji.sample.dto.response.HistoryListResponse;
import com.dji.sample.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.dji.sample.dto.request.UpdateWorkIssueRequest;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping
    public List<HistoryListResponse> getList() {
        return historyService.getList();
    }

    @GetMapping("/session/{sessionId}")
    public HistoryDetailResponse getDetailBySessionId(
            @PathVariable UUID sessionId
    ) {
        return historyService.getDetailBySessionId(
                sessionId
        );
    }
    
    @GetMapping("/{id}")
    public HistoryDetailResponse getDetail(@PathVariable UUID id) {
        return historyService.getDetail(id);
    }

    @PatchMapping("/{id}/work-issue")
    public HistoryDetailResponse updateWorkIssue(
            @PathVariable UUID id,
            @RequestBody UpdateWorkIssueRequest request
    ) {
        return historyService.updateWorkIssue(
                id,
                request.getWorkIssue()
        );
    }

    

    @PostMapping
    public HistoryDetailResponse createHistory(@RequestBody CreateHistoryRequest request) {
        return historyService.createHistory(request);
    }
}