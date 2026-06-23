package com.dji.sample.service.impl;

import com.dji.sample.dto.response.HistoryDetailResponse;
import com.dji.sample.entity.Device;
import com.dji.sample.entity.LiveStreamSession;
import com.dji.sample.entity.User;
import com.dji.sample.repository.UserRepository;
import com.dji.sample.security.CustomUserDetails;
import com.dji.sample.service.SlackNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackNotificationServiceImpl implements SlackNotificationService {

    private final RestTemplateBuilder restTemplateBuilder;
    private final UserRepository userRepository;

    @Value("${slack.enabled:false}")
    private boolean enabled;

    @Value("${slack.bot-token:}")
    private String botToken;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void notifyAiDetectionReport(HistoryDetailResponse report) {
        if (!enabled) {
            log.info("[SLACK] Disabled. Skip AI detection report.");
            return;
        }

        if (botToken == null || botToken.isBlank()) {
            log.warn("[SLACK] Bot token is empty. Skip AI detection report.");
            return;
        }

        if (report == null) {
            log.warn("[SLACK] Report is null. Skip AI detection report.");
            return;
        }

        if (report.getCompanyId() == null) {
            log.warn("[SLACK] companyId is empty. Skip AI detection report. deviceSn={}", report.getDeviceSn());
            return;
        }

        List<String> emails = userRepository
                .findByCompanyIdAndIsActiveTrueAndDeletedAtIsNull(report.getCompanyId())
                .stream()
                .map(User::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .distinct()
                .toList();

        if (emails.isEmpty()) {
            log.warn("[SLACK] No active company users found. companyId={}, deviceSn={}",
                    report.getCompanyId(), report.getDeviceSn());
            return;
        }

        RestTemplate restTemplate = restTemplateBuilder.build();
        List<Map<String, Object>> blocks = buildAiReportBlocks(report);

        for (String email : emails) {
            try {
                String userId = lookupUserByEmail(restTemplate, email);

                if (userId == null || userId.isBlank()) {
                    log.warn("[SLACK] Slack user not found for email={}", email);
                    continue;
                }

                sendBlockMessage(
                        restTemplate,
                        userId,
                        blocks,
                        "AI Detection Session Report"
                );

                log.info("[SLACK] AI detection report sent. email={}, userId={}, deviceSn={}",
                        email, userId, report.getDeviceSn());

            } catch (Exception e) {
                log.warn("[SLACK] Failed to send AI detection report to email={}. deviceSn={}, error={}",
                        email, report.getDeviceSn(), e.getMessage(), e);
            }
        }
    }

    @Override
    public void notifyStreamStarted(Device device, LiveStreamSession session, String hlsUrl) {
        if (!enabled) {
            log.info("[SLACK] Disabled. Skip stream start notification.");
            return;
        }

        if (botToken == null || botToken.isBlank()) {
            log.warn("[SLACK] Bot token is empty. Skip stream start notification.");
            return;
        }

        String email = getCurrentUserEmail();

        if (email == null || email.isBlank()) {
            log.warn("[SLACK] Current user email is empty. Skip stream start notification.");
            return;
        }

        try {
            RestTemplate restTemplate = restTemplateBuilder.build();

            String userId = lookupUserByEmail(restTemplate, email);

            if (userId == null || userId.isBlank()) {
                log.warn("[SLACK] Slack user not found for email={}", email);
                return;
            }

            String startedAtKst = session.getStartedAt()
                    .atZoneSameInstant(KST)
                    .format(FORMATTER);

            String companyName = device.getCompany() != null && device.getCompany().getName() != null
                    ? device.getCompany().getName()
                    : "-";

            String siteName = device.getSite() != null && device.getSite().getName() != null
                    ? device.getSite().getName()
                    : "-";

            String missionId = session.getMissionId() != null
                    ? session.getMissionId().toString()
                    : "-";

            String deviceType = safe(device.getDeviceType());

            String infoText = """
                    *%s ID:* %s
                    *%s Name:* %s
                    *Company:* %s
                    *Site:* %s
                    *Mission ID:* %s

                    *Session:* %s
                    *Start Time:* %s KST
                    *Playback URL:* %s
                    """.formatted(
                    deviceType,
                    safe(device.getDeviceSn()),
                    deviceType,
                    safe(device.getDeviceName()),
                    companyName,
                    siteName,
                    missionId,
                    savedSessionName(session),
                    startedAtKst,
                    hlsUrl
            );

            List<Map<String, Object>> blocks = List.of(
                    Map.of(
                            "type", "header",
                            "text", Map.of(
                                    "type", "plain_text",
                                    "text", "ROBOPILOT Stream Started",
                                    "emoji", true
                            )
                    ),
                    Map.of(
                            "type", "section",
                            "text", Map.of(
                                    "type", "mrkdwn",
                                    "text", infoText
                            )
                    ),
                    Map.of("type", "divider")
            );

            sendBlockMessage(
                    restTemplate,
                    userId,
                    blocks,
                    "ROBOPILOT Stream Started"
            );

            log.info("[SLACK] Stream start notification sent. email={}, userId={}, deviceSn={}, sessionId={}",
                    email, userId, device.getDeviceSn(), session.getId());

        } catch (Exception e) {
            log.warn("[SLACK] Failed to send stream start notification. deviceSn={}, error={}",
                    device.getDeviceSn(), e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> buildAiReportBlocks(HistoryDetailResponse report) {
        List<Map<String, Object>> blocks = new ArrayList<>();

        blocks.add(Map.of(
                "type", "header",
                "text", Map.of(
                        "type", "plain_text",
                        "text", "AI Detection Session Report",
                        "emoji", true
                )
        ));

        String deviceType = resolveDeviceType(report);

        StringBuilder info = new StringBuilder();

        appendLine(info, "*" + deviceType + " ID:* ", report.getDeviceSn());
        appendLine(info, "*" + deviceType + " Name:* ", firstNonBlank(report.getDeviceName(), report.getRobotName()));
        appendLine(info, "*Company:* ", report.getCompanyName());
        appendLine(info, "*Site:* ", report.getSiteName());
        appendLine(info, "*User Name:* ", firstNonBlank(report.getUserName(), report.getWorkerName()));
        appendLine(info, "*Mission:* ", report.getMissionName());

        if (report.getMissionId() != null) {
            info.append("*Mission ID:* ").append(report.getMissionId()).append("\n\n");
        }

        appendLine(info, "*Session:* ", safeSession(report));
        appendLine(info, "*Start Time:* ", report.getStartTime());
        appendLine(info, "*End Time:* ", report.getEndTime());
        appendLine(info, "*Duration:* ", formatDuration(report));

        int totalBookmarks = report.getBookmarks() != null
                ? report.getBookmarks().size()
                : report.getTotalRecognition() != null ? report.getTotalRecognition() : 0;

        info.append("*Total Bookmarks:* ").append(totalBookmarks);

        blocks.add(Map.of(
                "type", "section",
                "text", Map.of(
                        "type", "mrkdwn",
                        "text", info.toString()
                )
        ));

        blocks.add(Map.of("type", "divider"));

        if (report.getLabelCounts() != null && !report.getLabelCounts().isEmpty()) {
            StringBuilder counts = new StringBuilder();
            counts.append("*Detection Summary:*\n");

            report.getLabelCounts().forEach((label, count) ->
                    counts.append("• ")
                            .append(label)
                            .append(": ")
                            .append(count)
                            .append("\n")
            );

            blocks.add(Map.of(
                    "type", "section",
                    "text", Map.of(
                            "type", "mrkdwn",
                            "text", counts.toString()
                    )
            ));
        }

        return blocks;
    }

    private String lookupUserByEmail(RestTemplate restTemplate, String email) {
        String url = UriComponentsBuilder
                .fromHttpUrl("https://slack.com/api/users.lookupByEmail")
                .queryParam("email", email)
                .toUriString();

        HttpHeaders headers = authHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        Map body = response.getBody();

        if (body == null || !Boolean.TRUE.equals(body.get("ok"))) {
            log.warn("[SLACK] users.lookupByEmail failed. email={}, response={}", email, body);
            return null;
        }

        Object userObj = body.get("user");
        if (!(userObj instanceof Map<?, ?> userMap)) {
            return null;
        }

        Object id = userMap.get("id");
        return id != null ? id.toString() : null;
    }

    private void sendBlockMessage(
            RestTemplate restTemplate,
            String channelOrUserId,
            List<Map<String, Object>> blocks,
            String fallbackText
    ) {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = Map.of(
                "channel", channelOrUserId,
                "blocks", blocks,
                "text", fallbackText
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(
                        "https://slack.com/api/chat.postMessage",
                        HttpMethod.POST,
                        entity,
                        Map.class
                );

        Map body = response.getBody();

        if (body == null || !Boolean.TRUE.equals(body.get("ok"))) {
            throw new RuntimeException("chat.postMessage failed: " + body);
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(botToken);
        return headers;
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getEmail();
        }

        return null;
    }

    private String savedSessionName(LiveStreamSession session) {
        return session.getStartedAt()
                .atZoneSameInstant(KST)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
    }

    private String safeSession(HistoryDetailResponse report) {
        if (report.getStartTime() == null || report.getStartTime().isBlank()) {
            return "-";
        }

        return report.getStartTime()
                .replace(" ", "_")
                .replace(":", "-");
    }

    private String resolveDeviceType(HistoryDetailResponse report) {
        String name = firstNonBlank(report.getRobotName(), report.getDeviceName());

        if (name.toLowerCase().contains("drone")) {
            return "Drone";
        }

        if (name.toLowerCase().contains("robot") || name.toLowerCase().contains("go2")) {
            return "Robot";
        }

        return "Device";
    }

    private String formatDuration(HistoryDetailResponse report) {
        String duration = firstNonBlank(report.getDuration(), report.getTotalTime());

        if (duration == null || duration.isBlank()) {
            return "0m 0s";
        }

        String[] parts = duration.split(":");
        if (parts.length != 3) {
            return duration;
        }

        try {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = Integer.parseInt(parts[2]);

            if (hours > 0) {
                return hours + "h " + minutes + "m " + seconds + "s";
            }

            return minutes + "m " + seconds + "s";
        } catch (Exception e) {
            return duration;
        }
    }

    private void appendLine(StringBuilder builder, String label, String value) {
        if (value != null && !value.isBlank()) {
            builder.append(label).append(value).append("\n");
        }
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }

        if (second != null && !second.isBlank()) {
            return second;
        }

        return "";
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}