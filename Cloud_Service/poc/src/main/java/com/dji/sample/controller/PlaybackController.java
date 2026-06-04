package com.dji.sample.controller;

import com.dji.sample.dto.response.PlaybackListResponse;
import com.dji.sample.service.PlaybackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/playback")
@RequiredArgsConstructor
public class PlaybackController {

    private final PlaybackService playbackService;

    @GetMapping("/list")
    public List<PlaybackListResponse> getList(
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String siteId,
            @RequestParam(required = false) String deviceSn,
            @RequestParam(required = false) String missionId
    ) {
        return playbackService.getList(companyId, siteId, deviceSn, missionId);
    }
}