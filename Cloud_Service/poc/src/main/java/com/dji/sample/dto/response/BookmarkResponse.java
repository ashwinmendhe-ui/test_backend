package com.dji.sample.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookmarkResponse {
    private String label;
    private String mdisplay;
    private String duration;
}