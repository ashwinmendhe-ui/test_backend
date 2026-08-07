package com.dji.sample.service.impl;

import com.dji.sample.dto.response.HistoryDetailResponse;
import com.dji.sample.dto.response.HistoryListResponse;
import com.dji.sample.entity.*;
import com.dji.sample.repository.*;
import com.dji.sample.service.HistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import com.dji.sample.util.DateTimeUtil;
import com.dji.sample.dto.request.CreateHistoryRequest;
import java.time.Duration;
import com.dji.sample.dto.response.BookmarkResponse;
import com.dji.sample.service.S3PresignService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.springframework.data.domain.PageRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryServiceImpl implements HistoryService {

    private final ReportHistoryRepository reportHistoryRepository;
    private final CompanyRepository companyRepository;
    private final SiteRepository siteRepository;
    private final MissionRepository missionRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final LiveStreamSessionRepository liveStreamSessionRepository;
    private final S3PresignService s3PresignService;
    private final ObjectMapper objectMapper;
    private static final long BOOKMARK_FPS = 30L;
    
    @Override
    public List<HistoryListResponse> getList() {
        return reportHistoryRepository.findByOrderByCreatedAtDesc(PageRequest.of(0, 20))
                .stream()
                .map(this::toListResponse)
                .toList();
    }

    @Override
    public HistoryDetailResponse getDetail(UUID historyId) {
        ReportHistory history = reportHistoryRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("History not found"));

        Site site = findSite(history.getSiteId());
        Mission mission = findMission(history.getMissionId());
        Device device = findDevice(history.getDeviceSn());
        User user = findUser(history.getUserId());
        Company company = findCompany(history.getCompanyId());
        String deviceName = device != null ? device.getDeviceName() : history.getDeviceSn();
        String userName = resolveUserName(user);
        String metadataUrl = resolveMetadataUrl(history.getPlaybackUrl());

        Map<String, Integer> labelCounts = loadLabelCounts(metadataUrl);
        List<BookmarkResponse> bookmarks =
        loadBookmarks(metadataUrl, history.getStartTime());
        int calculatedTotalRecognition = !labelCounts.isEmpty()
                ? labelCounts.values().stream().mapToInt(Integer::intValue).sum()
                : bookmarks.size();

        if ((history.getTotalRecognition() == null || history.getTotalRecognition() == 0)
                && calculatedTotalRecognition > 0) {
            history.setTotalRecognition(calculatedTotalRecognition);
            reportHistoryRepository.save(history);
        }
            return HistoryDetailResponse.builder()
            .deviceSn(history.getDeviceSn())
            .siteName(site != null ? site.getName() : "")
            .deviceName(deviceName)
            .companyId(history.getCompanyId())
            .companyName(company != null ? company.getName() : "")
            .siteId(history.getSiteId())
            .missionId(history.getMissionId())
            .robotName(deviceName)
            .missionName(mission != null ? mission.getMissionName() : "")
            .userName(userName)
            .workerName(userName)
            .startTime(format(history.getStartTime()))
            .endTime(format(history.getEndTime()))
            .totalTime(history.getTotalTime())
            .totalRecognition(calculatedTotalRecognition)
            .duration(history.getTotalTime())
            .distance("")
            .playbackUrl(history.getPlaybackUrl())
            .reportCreatedAt(
                    format(
                            history.getEndTime() != null
                                    ? history.getEndTime()
                                    : history.getStartTime()
                    )
            )
            .labelCounts(labelCounts)
            .bookmarks(bookmarks)
            .build();
    }

    private HistoryListResponse toListResponse(ReportHistory history) {
        Company company = findCompany(history.getCompanyId());
        Site site = findSite(history.getSiteId());
        Mission mission = findMission(history.getMissionId());
        Device device = findDevice(history.getDeviceSn());
        User user = findUser(history.getUserId());

        return HistoryListResponse.builder()
                .historyId(history.getHistoryId())
                .companyId(history.getCompanyId())
                .companyName(company != null ? company.getName() : "")
                .siteId(history.getSiteId())
                .siteName(site != null ? site.getName() : "")
                .missionId(history.getMissionId())
                .missionName(mission != null ? mission.getMissionName() : "")
                .deviceSn(history.getDeviceSn())
                .deviceName(device != null ? device.getDeviceName() : history.getDeviceSn())
                .playbackUrl(history.getPlaybackUrl())
                .userName(resolveUserName(user))
                .totalRecognition(history.getTotalRecognition())
                .createdAt(format(history.getCreatedAt()))
                .videoStatus(history.getVideoStatus())
                .build();
    }

    private Company findCompany(UUID companyId) {
        if (companyId == null) return null;
        return companyRepository.findByCompanyIdAndDeletedAtIsNull(companyId).orElse(null);
    }

    private Site findSite(UUID siteId) {
        if (siteId == null) return null;
        return siteRepository.findBySiteIdAndDeletedAtIsNull(siteId).orElse(null);
    }

    private Mission findMission(UUID missionId) {
        if (missionId == null) return null;
        return missionRepository.findByMissionIdAndDeletedAtIsNull(missionId).orElse(null);
    }

    private Device findDevice(String deviceSn) {
        if (deviceSn == null || deviceSn.isBlank()) return null;
        return deviceRepository.findByDeviceSnAndDeletedAtIsNull(deviceSn).orElse(null);
    }

    private User findUser(UUID userId) {
        if (userId == null) return null;
        return userRepository.findByUserIdAndDeletedAtIsNull(userId).orElse(null);
    }

    private String resolveUserName(User user) {
        if (user == null) return "";
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getEmail() != null ? user.getEmail() : "";
    }


    private String calculateTotalTime(OffsetDateTime startTime, OffsetDateTime endTime) {
        if (startTime == null || endTime == null) {
            return "00:00:00";
        }

        Duration duration = Duration.between(startTime, endTime);

        long seconds = Math.max(duration.getSeconds(), 0);
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    @Override
    public HistoryDetailResponse createHistory(CreateHistoryRequest request) {

        if (request.getDeviceSn() == null || request.getDeviceSn().isBlank()) {
            throw new RuntimeException("deviceSn is required");
        }

        if (request.getPlaybackUrl() == null || request.getPlaybackUrl().isBlank()) {
            throw new RuntimeException("playbackUrl is required");
        }

        Device device = findDevice(request.getDeviceSn());

        LiveStreamSession session = null;

        if (request.getSessionId() != null) {
            session = liveStreamSessionRepository
                    .findById(request.getSessionId())
                    .orElse(null);
        }

        if (session == null && request.getMissionId() != null) {
            session = liveStreamSessionRepository
                    .findFirstByDeviceSnAndMissionIdOrderByStartedAtDesc(
                            request.getDeviceSn(),
                            request.getMissionId()
                    )
                    .orElse(null);
        }

        UUID companyId = null;
        UUID siteId = null;

        if (device != null) {
            companyId = device.getCompany() != null ? device.getCompany().getCompanyId() : null;
            siteId = device.getSite() != null ? device.getSite().getSiteId() : null;
        }

        OffsetDateTime startTime = session != null ? session.getStartedAt() : OffsetDateTime.now();
        OffsetDateTime endTime = session != null && session.getStoppedAt() != null
                ? session.getStoppedAt()
                : OffsetDateTime.now();

        Map<String, Integer> labelCounts = loadLabelCounts(resolveMetadataUrl(request.getPlaybackUrl()));
        int totalRecognition = labelCounts.values().stream().mapToInt(Integer::intValue).sum();

        ReportHistory history = ReportHistory.builder()
                .historyId(UUID.randomUUID())
                .deviceSn(request.getDeviceSn())
                .playbackUrl(request.getPlaybackUrl())
                .companyId(companyId)
                .siteId(siteId)
                .missionId(request.getMissionId())
                .userId(session != null ? session.getUserId() : null)
                .startTime(startTime)
                .endTime(endTime)
                .totalTime(calculateTotalTime(startTime, endTime))
                .totalRecognition(totalRecognition)
                .createdAt(OffsetDateTime.now())
                .videoStatus("AVAILABLE")
                .sessionId(request.getSessionId())
                .build();

        ReportHistory saved = reportHistoryRepository.save(history);

        return getDetail(saved.getHistoryId());
    }


    private String extractFolderPathFromUrl(String playbackUrl) {
        int idx = playbackUrl.indexOf("/streams/");
        if (idx < 0) {
            return null;
        }

        String after = playbackUrl.substring(idx + "/streams/".length());
        String[] parts = after.split("/");

        if (parts.length < 2) {
            return null;
        }

        return "streams/" + parts[0] + "/" + parts[1];    }

    private Map<String, Integer> loadLabels(String playbackUrl) {
        try {
            String folderPath = extractFolderPathFromUrl(playbackUrl);
            if (folderPath == null) return Collections.emptyMap();

            String objectKey = folderPath + "/info.json";
            

            try (InputStream is = s3PresignService.getStreamObject(objectKey)) {
                JsonNode root = objectMapper.readTree(is);
                JsonNode labelsNode = root.get("labels");

                if (labelsNode == null || !labelsNode.isObject()) {
                    return Collections.emptyMap();
                }

                Map<String, Integer> labels = new HashMap<>();
                labelsNode.fields().forEachRemaining(entry ->
                        labels.put(entry.getKey(), entry.getValue().asInt())
                );

                return labels;
            }
        } catch (Exception e) {

    return Collections.emptyMap();
}
    }

   private List<BookmarkRaw> loadBookmarkRaw(String playbackUrl) {
    try {
        String folderPath = extractFolderPathFromUrl(playbackUrl);

        if (folderPath == null) {
            return Collections.emptyList();
        }

        String objectKey = folderPath + "/bookmark.ndjson";
        List<BookmarkRaw> result = new ArrayList<>();

        try (
                InputStream is = s3PresignService.getStreamObject(objectKey);
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(is))
        ) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                JsonNode node = objectMapper.readTree(line);

                BookmarkRaw raw = new BookmarkRaw();
                raw.labelIds = new ArrayList<>();

                JsonNode cAr = node.get("c_ar");

                if (cAr != null && cAr.isArray()) {
                    for (JsonNode item : cAr) {
                        raw.labelIds.add(item.asInt());
                    }
                }

                raw.offset = node.has("o")
                        ? node.get("o").asLong()
                        : 0L;

                result.add(raw);
            }
        }

        return result;

    } catch (Exception e) {
        log.warn(
                "[History] Failed to load bookmark.ndjson for playbackUrl={}",
                playbackUrl,
                e
        );

        return Collections.emptyList();
    }
}

private Map<String, Integer> loadLabelCounts(String playbackUrl) {
    Map<String, Integer> labels = loadLabels(playbackUrl);

    if (labels.isEmpty()) {
        return Collections.emptyMap();
    }

    Map<Integer, String> idToName = new HashMap<>();
    labels.forEach((name, id) -> idToName.put(id, name));

    Map<String, Integer> counts = new HashMap<>();

    for (BookmarkRaw bookmark : loadBookmarkRaw(playbackUrl)) {
        for (Integer labelId : bookmark.labelIds) {
            String labelName = idToName.get(labelId);

            if (labelName != null) {
                counts.put(
                        labelName,
                        counts.getOrDefault(labelName, 0) + 1
                );
            }
        }
    }

    return counts;
}

private List<BookmarkResponse> loadBookmarks(
        String playbackUrl,
        OffsetDateTime startTime
) {
    Map<String, Integer> labels = loadLabels(playbackUrl);

    if (labels.isEmpty()) {
        return Collections.emptyList();
    }

    Map<Integer, String> idToName = new HashMap<>();
    labels.forEach((name, id) -> idToName.put(id, name));

    List<BookmarkRaw> rawBookmarks =
            new ArrayList<>(loadBookmarkRaw(playbackUrl));

    rawBookmarks.sort(
            Comparator.comparingLong(
                    bookmark -> bookmark.offset == null
                            ? 0L
                            : bookmark.offset
            )
    );

    List<BookmarkResponse> responses = new ArrayList<>();

    for (BookmarkRaw bookmark : rawBookmarks) {
        for (Integer labelId : bookmark.labelIds) {
            String labelName = idToName.get(labelId);

            if (labelName == null) {
                continue;
            }

            String duration =
                    formatBookmarkOffset(bookmark.offset);

            String displayTime =
                    formatBookmarkClockTime(
                            startTime,
                            bookmark.offset
                    );

            responses.add(
                    BookmarkResponse.builder()
                            .label(labelName)
                            .mdisplay(displayTime)
                            .duration(duration)
                            .build()
            );
        }
    }

    return responses;
}

private String resolveMetadataUrl(String playbackUrl) {
    if (playbackUrl == null || playbackUrl.isBlank()) {
        return playbackUrl;
    }

    if (playbackUrl.contains("/streams/")) {
        return playbackUrl;
    }

    String marker = "/live/hls/";
    int idx = playbackUrl.indexOf(marker);

    if (idx < 0) {
        return playbackUrl;
    }

    String after = playbackUrl.substring(idx + marker.length());
    String sessionIdText = after.split("/")[0];

    try {
        UUID sessionId = UUID.fromString(sessionIdText);

        return liveStreamSessionRepository.findById(sessionId)
                .map(LiveStreamSession::getPlaybackUrl)
                .filter(url -> url != null && !url.isBlank())
                .orElse(playbackUrl);

    } catch (Exception e) {
        log.warn(
                "[History] Failed to resolve metadata url from playbackUrl={}",
                playbackUrl,
                e
        );

        return playbackUrl;
    }
}

private String formatBookmarkOffset(Long frameOffset) {
    if (frameOffset == null || frameOffset < 0) {
        return "00:00:00";
    }

    long totalSeconds = frameOffset / BOOKMARK_FPS;

    long hours = totalSeconds / 3600;
    long minutes = (totalSeconds % 3600) / 60;
    long seconds = totalSeconds % 60;

    return String.format(
            "%02d:%02d:%02d",
            hours,
            minutes,
            seconds
    );
}

private String formatBookmarkClockTime(
        OffsetDateTime startTime,
        Long frameOffset
) {
    if (startTime == null) {
        return formatBookmarkOffset(frameOffset);
    }

    long safeFrameOffset =
            frameOffset == null || frameOffset < 0
                    ? 0L
                    : frameOffset;

    long offsetMillis = Math.round(
            safeFrameOffset * 1000.0 / BOOKMARK_FPS
    );

    return startTime
            .plusNanos(offsetMillis * 1_000_000L)
            .atZoneSameInstant(
                    java.time.ZoneId.of("Asia/Seoul")
            )
            .format(
                    java.time.format.DateTimeFormatter
                            .ofPattern("HH:mm:ss")
            );
}

private static class BookmarkRaw {
    List<Integer> labelIds;
    Long offset;
}

    private String format(OffsetDateTime value) {
        String formatted = DateTimeUtil.formatKst(value);
        return formatted != null ? formatted : "";
    }
}