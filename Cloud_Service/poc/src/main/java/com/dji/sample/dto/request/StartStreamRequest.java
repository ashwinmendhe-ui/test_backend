package com.dji.sample.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class StartStreamRequest {

    private String deviceSn;

    private Integer urlType;

    private VideoId videoId;

    private Integer videoQuality;

    private String videoType;

    private UUID missionId;

    @Data
    public static class VideoId {
        private String droneSn;
        private PayloadIndex payloadIndex;
        private String videoType;
    }

    @Data
    public static class PayloadIndex {
        private Integer type;
        private Integer subType;
        private Integer position;
    }
}