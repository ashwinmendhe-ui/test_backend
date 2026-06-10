package com.dji.sample.controller;

import com.dji.sample.entity.LiveStreamSession;
import com.dji.sample.repository.LiveStreamSessionRepository;
import com.dji.sample.service.S3PresignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/live/hls")
@RequiredArgsConstructor
public class LiveHlsProxyController {

    private final LiveStreamSessionRepository liveStreamSessionRepository;
    private final S3PresignService s3PresignService;

    @GetMapping(value = "/{sessionId}/index.m3u8", produces = "application/vnd.apple.mpegurl")
    public ResponseEntity<String> getPlaylist(@PathVariable UUID sessionId) {
        LiveStreamSession session = liveStreamSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Stream session not found"));

        String indexKey = extractObjectKey(session.getPlaybackUrl());
        String signedIndexUrl = s3PresignService.createStreamDownloadUrl(indexKey);

        RestTemplate restTemplate = new RestTemplate();
        String playlist = restTemplate.getForObject(
                URI.create(signedIndexUrl),
                String.class
        );
        if (playlist == null || playlist.isBlank()) {
            throw new RuntimeException("Playlist is empty");
        }

        String rewritten = playlist.lines()
                .map(line -> {
                    if (line.isBlank() || line.startsWith("#")) {
                        return line;
                    }

                    return "/api/v1/live/hls/" + sessionId + "/" + line.trim();
                })
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                .body(rewritten);
    }

    @GetMapping("/{sessionId}/index.m3u8/{fileName:.+}")
    public ResponseEntity<byte[]> getHlsFileAfterIndex(
            @PathVariable UUID sessionId,
            @PathVariable String fileName
    ) {
        return getHlsFile(sessionId, fileName);
    }

    @GetMapping("/{sessionId}/{fileName:.+}")
    public ResponseEntity<byte[]> getHlsFile(
            @PathVariable UUID sessionId,
            @PathVariable String fileName
    ) {
        LiveStreamSession session = liveStreamSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Stream session not found"));

        String indexKey = extractObjectKey(session.getPlaybackUrl());
        String baseKey = indexKey.substring(0, indexKey.lastIndexOf("/") + 1);
        String fileKey = baseKey + fileName;

        String signedFileUrl = s3PresignService.createStreamDownloadUrl(fileKey);

        RestTemplate restTemplate = new RestTemplate();
        byte[] data = restTemplate.getForObject(
                URI.create(signedFileUrl),
                byte[].class
        );
        MediaType mediaType;

        if (fileName.endsWith(".ts")) {
            mediaType = MediaType.parseMediaType("video/mp2t");
        } else if (fileName.endsWith(".m3u8")) {
            mediaType = MediaType.parseMediaType("application/vnd.apple.mpegurl");
        } else if (fileName.endsWith(".json") || fileName.endsWith(".ndjson")) {
            mediaType = MediaType.APPLICATION_JSON;
        } else {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(data);
    }

    private String extractObjectKey(String playbackUrl) {
        String marker = ".amazonaws.com/";
        int start = playbackUrl.indexOf(marker);

        if (start < 0) {
            throw new RuntimeException("Invalid S3 playback URL");
        }

        String keyWithQuery = playbackUrl.substring(start + marker.length());
        int queryIndex = keyWithQuery.indexOf("?");

        return queryIndex >= 0
                ? keyWithQuery.substring(0, queryIndex)
                : keyWithQuery;
    }
}