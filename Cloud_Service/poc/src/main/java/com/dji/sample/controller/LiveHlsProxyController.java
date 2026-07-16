package com.dji.sample.controller;

import com.dji.sample.entity.LiveStreamSession;
import com.dji.sample.repository.LiveStreamSessionRepository;
import com.dji.sample.service.S3PresignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/live/hls")
@RequiredArgsConstructor
public class LiveHlsProxyController {

    private static final MediaType HLS_PLAYLIST_TYPE =
            MediaType.parseMediaType("application/vnd.apple.mpegurl");

    private static final MediaType MPEG_TS_TYPE =
            MediaType.parseMediaType("video/mp2t");

    private final LiveStreamSessionRepository liveStreamSessionRepository;
    private final S3PresignService s3PresignService;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping(
            value = "/{sessionId}/index.m3u8",
            produces = "application/vnd.apple.mpegurl"
    )
    public ResponseEntity<?> getPlaylist(
            @PathVariable UUID sessionId
    ) {
        LiveStreamSession session =
                liveStreamSessionRepository.findById(sessionId)
                        .orElse(null);

        if (session == null) {
            log.warn(
                    "[HLS] Session not found for playlist. sessionId={}",
                    sessionId
            );

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Stream session not found");
        }

        if (session.getPlaybackUrl() == null ||
                session.getPlaybackUrl().isBlank()) {

            log.warn(
                    "[HLS] Playback URL missing. sessionId={}",
                    sessionId
            );

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Playback URL not available");
        }

        String indexKey;

        try {
            indexKey = extractObjectKey(session.getPlaybackUrl());
        } catch (RuntimeException e) {
            log.error(
                    "[HLS] Invalid playback URL. sessionId={}, playbackUrl={}",
                    sessionId,
                    session.getPlaybackUrl(),
                    e
            );

            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Invalid playback URL");
        }

        String signedIndexUrl =
                s3PresignService.createStreamDownloadUrl(indexKey);

        try {
            ResponseEntity<byte[]> upstreamResponse =
                    restTemplate.exchange(
                            URI.create(signedIndexUrl),
                            HttpMethod.GET,
                            null,
                            byte[].class
                    );

            byte[] responseBody = upstreamResponse.getBody();

            if (responseBody == null || responseBody.length == 0) {
                log.warn(
                        "[HLS] Playlist is empty. sessionId={}, indexKey={}",
                        sessionId,
                        indexKey
                );

                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("Playlist is empty");
            }

            String playlist =
                    new String(
                            responseBody,
                            StandardCharsets.UTF_8
                    );

            String rewritten = playlist.lines()
                    .map(line -> {
                        if (line.isBlank() ||
                                line.startsWith("#")) {
                            return line;
                        }

                        /*
                         * Convert every segment or related file into an
                         * absolute backend proxy URL.
                         */
                        return "/api/v1/live/hls/"
                                + sessionId
                                + "/"
                                + line.trim();
                    })
                    .reduce(
                            (first, second) ->
                                    first + "\n" + second
                    )
                    .orElse("");

            return ResponseEntity.ok()
                    .contentType(HLS_PLAYLIST_TYPE)
                    .header(
                            HttpHeaders.CACHE_CONTROL,
                            "no-store, no-cache, must-revalidate"
                    )
                    .body(rewritten);

        } catch (HttpStatusCodeException e) {
            log.warn(
                    "[HLS] Upstream playlist request failed. sessionId={}, indexKey={}, status={}",
                    sessionId,
                    indexKey,
                    e.getStatusCode()
            );

            return ResponseEntity
                    .status(e.getStatusCode())
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Playlist is not available");

        } catch (Exception e) {
            log.error(
                    "[HLS] Unexpected playlist proxy error. sessionId={}, indexKey={}",
                    sessionId,
                    indexKey,
                    e
            );

            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Unable to fetch playlist");
        }
    }

    /*
     * Compatibility route for a client that resolves a relative file URL
     * below index.m3u8.
     */
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
        LiveStreamSession session =
                liveStreamSessionRepository.findById(sessionId)
                        .orElse(null);

        MediaType mediaType =
                resolveMediaType(fileName);

        if (session == null) {
            log.warn(
                    "[HLS] Session not found for file. sessionId={}, fileName={}",
                    sessionId,
                    fileName
            );

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .contentType(mediaType)
                    .body(new byte[0]);
        }

        if (session.getPlaybackUrl() == null ||
                session.getPlaybackUrl().isBlank()) {

            log.warn(
                    "[HLS] Playback URL missing for file. sessionId={}, fileName={}",
                    sessionId,
                    fileName
            );

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .contentType(mediaType)
                    .body(new byte[0]);
        }

        String indexKey;

        try {
            indexKey =
                    extractObjectKey(session.getPlaybackUrl());
        } catch (RuntimeException e) {
            log.error(
                    "[HLS] Invalid playback URL for file. sessionId={}, fileName={}, playbackUrl={}",
                    sessionId,
                    fileName,
                    session.getPlaybackUrl(),
                    e
            );

            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .contentType(mediaType)
                    .body(new byte[0]);
        }

        int lastSlashIndex =
                indexKey.lastIndexOf("/");

        if (lastSlashIndex < 0) {
            log.error(
                    "[HLS] Invalid index object key. sessionId={}, indexKey={}",
                    sessionId,
                    indexKey
            );

            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .contentType(mediaType)
                    .body(new byte[0]);
        }

        String baseKey =
                indexKey.substring(
                        0,
                        lastSlashIndex + 1
                );

        String fileKey =
                baseKey + fileName;

        String signedFileUrl =
                s3PresignService.createStreamDownloadUrl(
                        fileKey
                );

        try {
            ResponseEntity<byte[]> upstreamResponse =
                    restTemplate.exchange(
                            URI.create(signedFileUrl),
                            HttpMethod.GET,
                            null,
                            byte[].class
                    );

            byte[] data =
                    upstreamResponse.getBody();

            if (data == null || data.length == 0) {
                log.warn(
                        "[HLS] Upstream file is empty. sessionId={}, fileName={}, fileKey={}",
                        sessionId,
                        fileName,
                        fileKey
                );

                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .contentType(mediaType)
                        .body(new byte[0]);
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(
                            HttpHeaders.CACHE_CONTROL,
                            "no-store, no-cache, must-revalidate"
                    )
                    .body(data);

        } catch (HttpStatusCodeException e) {
            /*
             * Important:
             * Return the HLS-compatible response directly.
             * Do not throw into GlobalExceptionHandler.
             */
            log.warn(
                    "[HLS] Upstream file request failed. sessionId={}, fileName={}, fileKey={}, status={}",
                    sessionId,
                    fileName,
                    fileKey,
                    e.getStatusCode()
            );

            return ResponseEntity
                    .status(e.getStatusCode())
                    .contentType(mediaType)
                    .body(new byte[0]);

        } catch (Exception e) {
            log.error(
                    "[HLS] Unexpected file proxy error. sessionId={}, fileName={}, fileKey={}",
                    sessionId,
                    fileName,
                    fileKey,
                    e
            );

            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .contentType(mediaType)
                    .body(new byte[0]);
        }
    }

    private MediaType resolveMediaType(
            String fileName
    ) {
        String normalized =
                fileName.toLowerCase();

        if (normalized.endsWith(".ts")) {
            return MPEG_TS_TYPE;
        }

        if (normalized.endsWith(".m3u8")) {
            return HLS_PLAYLIST_TYPE;
        }

        if (normalized.endsWith(".json")) {
            return MediaType.APPLICATION_JSON;
        }

        if (normalized.endsWith(".ndjson")) {
            return MediaType.parseMediaType(
                    "application/x-ndjson"
            );
        }

        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private String extractObjectKey(
            String playbackUrl
    ) {
        if (playbackUrl == null ||
                playbackUrl.isBlank()) {
            throw new RuntimeException(
                    "Playback URL is empty"
            );
        }

        if (playbackUrl.contains(".amazonaws.com/")) {

            String marker = ".amazonaws.com/";
            int start =
                    playbackUrl.indexOf(marker);

            String keyWithQuery =
                    playbackUrl.substring(
                            start + marker.length()
                    );

            int queryIndex =
                    keyWithQuery.indexOf("?");

            return queryIndex >= 0
                    ? keyWithQuery.substring(
                            0,
                            queryIndex
                    )
                    : keyWithQuery;
        }

        if (playbackUrl.contains(".cloudfront.net/")) {

            String marker = ".cloudfront.net/";
            int start =
                    playbackUrl.indexOf(marker);

            String keyWithQuery =
                    playbackUrl.substring(
                            start + marker.length()
                    );

            int queryIndex =
                    keyWithQuery.indexOf("?");

            return queryIndex >= 0
                    ? keyWithQuery.substring(
                            0,
                            queryIndex
                    )
                    : keyWithQuery;
        }

        throw new RuntimeException(
                "Unsupported playback URL: " +
                        playbackUrl
        );
    }
}