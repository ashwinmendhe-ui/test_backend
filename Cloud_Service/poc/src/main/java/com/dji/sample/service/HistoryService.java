package com.dji.sample.service;

import com.dji.sample.dto.request.CreateHistoryRequest;
import com.dji.sample.dto.response.HistoryDetailResponse;
import com.dji.sample.dto.response.HistoryListResponse;

import java.util.List;
import java.util.UUID;

public interface HistoryService {
    List<HistoryListResponse> getList();
    HistoryDetailResponse getDetail(UUID historyId);
    HistoryDetailResponse createHistory(CreateHistoryRequest request);
}