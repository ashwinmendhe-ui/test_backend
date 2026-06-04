package com.dji.sample.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaybackListResponse {
    private String segment;
    private String url;
}