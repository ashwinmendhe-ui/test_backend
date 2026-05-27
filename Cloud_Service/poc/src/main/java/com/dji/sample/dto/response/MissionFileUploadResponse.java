package com.dji.sample.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MissionFileUploadResponse {

    private String file;
    private String fileName;
    private String downloadUrl;
}