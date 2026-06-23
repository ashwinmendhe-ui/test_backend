package com.dji.sample.service;

import com.dji.sample.dto.response.HistoryDetailResponse;
import com.dji.sample.entity.Device;
import com.dji.sample.entity.LiveStreamSession;

public interface SlackNotificationService {

    void notifyStreamStarted(Device device, LiveStreamSession session, String hlsUrl);

    void notifyAiDetectionReport(HistoryDetailResponse report);
}