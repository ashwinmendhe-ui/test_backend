package com.dji.sample.service;

import com.dji.sample.dto.response.PlaybackListResponse;
import com.dji.sample.dto.response.PlaybackOptionResponse;
import com.dji.sample.dto.response.PlaybackTelemetryResponse;
import com.dji.sample.entity.Device;
import com.dji.sample.entity.LiveStreamSession;
import com.dji.sample.entity.Mission;
import com.dji.sample.entity.ReportHistory;
import com.dji.sample.entity.User;
import com.dji.sample.repository.DeviceRepository;
import com.dji.sample.repository.DeviceTelemetryHistoryRepository;
import com.dji.sample.repository.LiveStreamSessionRepository;
import com.dji.sample.repository.MissionRepository;
import com.dji.sample.repository.ReportHistoryRepository;
import com.dji.sample.repository.UserRepository;
import com.dji.sample.repository.UserRoleRepository;
import com.dji.sample.security.CustomUserDetails;
import com.dji.sample.util.DateTimeUtil;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaybackService {

    private final ReportHistoryRepository reportHistoryRepository;
    private final DeviceTelemetryHistoryRepository deviceTelemetryHistoryRepository;
    private final LiveStreamSessionRepository liveStreamSessionRepository;
    private final DeviceRepository deviceRepository;
    private final MissionRepository missionRepository;

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public List<PlaybackListResponse> getList(
            String companyId,
            String siteId,
            String deviceSn,
            String missionId
    ) {
        User currentUser = getCurrentUser();

        validateCompanyAccess(currentUser, companyId);
        validateSiteAccess(currentUser, siteId);
        validateDeviceAccess(currentUser, deviceSn);
        validateMissionAccess(currentUser, missionId);

        Set<String> allowedDeviceSns = getAllowedDeviceSns(currentUser);
        Set<UUID> allowedMissionIds = getAllowedMissionIds(currentUser);
        Set<UUID> allowedSiteIds = getAllowedSiteIds(currentUser);

        Specification<ReportHistory> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isNotNull(root.get("playbackUrl")));
            predicates.add(cb.notEqual(root.get("playbackUrl"), ""));

            /*
             * SYS_ADMIN can query everything.
             * All other roles remain restricted to their company.
             */
            if (!isSysAdmin(currentUser)) {
                UUID currentCompanyId = currentUser.getCompanyId();

                if (currentCompanyId == null) {
                    return cb.disjunction();
                }

                predicates.add(
                        cb.equal(
                                root.get("companyId"),
                                currentCompanyId
                        )
                );
            }

            /*
             * Company User must only receive assigned resources.
             */
            if (isCompanyUser(currentUser)) {

                if (allowedDeviceSns.isEmpty() ||
                        allowedMissionIds.isEmpty() ||
                        allowedSiteIds.isEmpty()) {
                    return cb.disjunction();
                }

                predicates.add(
                        root.get("deviceSn").in(allowedDeviceSns)
                );

                predicates.add(
                        root.get("missionId").in(allowedMissionIds)
                );

                predicates.add(
                        root.get("siteId").in(allowedSiteIds)
                );
            }

            if (companyId != null && !companyId.isBlank()) {
                predicates.add(
                        cb.equal(
                                root.get("companyId"),
                                UUID.fromString(companyId)
                        )
                );
            }

            if (siteId != null && !siteId.isBlank()) {
                predicates.add(
                        cb.equal(
                                root.get("siteId"),
                                UUID.fromString(siteId)
                        )
                );
            }

            if (deviceSn != null && !deviceSn.isBlank()) {
                predicates.add(
                        cb.equal(
                                root.get("deviceSn"),
                                deviceSn
                        )
                );
            }

            if (missionId != null && !missionId.isBlank()) {
                predicates.add(
                        cb.equal(
                                root.get("missionId"),
                                UUID.fromString(missionId)
                        )
                );
            }

            return cb.and(
                    predicates.toArray(new Predicate[0])
            );
        };

        return reportHistoryRepository
                .findAll(
                        spec,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<PlaybackTelemetryResponse> getTelemetry(UUID sessionId) {

        User currentUser = getCurrentUser();

        LiveStreamSession session = liveStreamSessionRepository
                .findById(sessionId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Stream session not found"
                        )
                );

        /*
         * Prevent direct sessionId access to telemetry belonging
         * to unassigned resources.
         */
        validateDeviceAccess(
                currentUser,
                session.getDeviceSn()
        );

        if (session.getMissionId() != null) {
            validateMissionAccess(
                    currentUser,
                    session.getMissionId().toString()
            );
        }

        OffsetDateTime startedAt = session.getStartedAt();

        return deviceTelemetryHistoryRepository
                .findBySessionIdOrderByRecordedAtAsc(sessionId)
                .stream()
                .map(item ->
                        PlaybackTelemetryResponse.builder()
                                .recordedAt(
                                        item.getRecordedAt()
                                )
                                .offsetMs(
                                        startedAt != null
                                                ? java.time.Duration
                                                    .between(
                                                            startedAt,
                                                            item.getRecordedAt()
                                                    )
                                                    .toMillis()
                                                : 0L
                                )
                                .status(item.getStatus())
                                .battery(item.getBattery())
                                .network(item.getNetwork())
                                .gps(item.getGps())
                                .latitude(item.getLatitude())
                                .longitude(item.getLongitude())
                                .altitude(item.getAltitude())
                                .speed(item.getSpeed())
                                .build()
                )
                .toList();
    }

    public List<PlaybackOptionResponse> getOptions(
            String companyId,
            String siteId
    ) {

        User currentUser = getCurrentUser();

        validateCompanyAccess(currentUser, companyId);
        validateSiteAccess(currentUser, siteId);

        Set<String> allowedDeviceSns =
                getAllowedDeviceSns(currentUser);

        Set<UUID> allowedMissionIds =
                getAllowedMissionIds(currentUser);

        Set<UUID> allowedSiteIds =
                getAllowedSiteIds(currentUser);

        Specification<ReportHistory> spec =
                (root, query, cb) -> {

                    List<Predicate> predicates =
                            new ArrayList<>();

                    predicates.add(
                            cb.isNotNull(
                                    root.get("playbackUrl")
                            )
                    );

                    predicates.add(
                            cb.notEqual(
                                    root.get("playbackUrl"),
                                    ""
                            )
                    );

                    /*
                     * Company Admin / Company User cannot query
                     * another company's playback history.
                     */
                    if (!isSysAdmin(currentUser)) {

                        UUID currentCompanyId =
                                currentUser.getCompanyId();

                        if (currentCompanyId == null) {
                            return cb.disjunction();
                        }

                        predicates.add(
                                cb.equal(
                                        root.get("companyId"),
                                        currentCompanyId
                                )
                        );
                    }

                    /*
                     * Company User options must be the
                     * intersection of playback history and
                     * assigned resources.
                     */
                    if (isCompanyUser(currentUser)) {

                        if (allowedDeviceSns.isEmpty() ||
                                allowedMissionIds.isEmpty() ||
                                allowedSiteIds.isEmpty()) {
                            return cb.disjunction();
                        }

                        predicates.add(
                                root.get("deviceSn")
                                        .in(allowedDeviceSns)
                        );

                        predicates.add(
                                root.get("missionId")
                                        .in(allowedMissionIds)
                        );

                        predicates.add(
                                root.get("siteId")
                                        .in(allowedSiteIds)
                        );
                    }

                    if (companyId != null &&
                            !companyId.isBlank()) {

                        predicates.add(
                                cb.equal(
                                        root.get("companyId"),
                                        UUID.fromString(companyId)
                                )
                        );
                    }

                    if (siteId != null &&
                            !siteId.isBlank()) {

                        predicates.add(
                                cb.equal(
                                        root.get("siteId"),
                                        UUID.fromString(siteId)
                                )
                        );
                    }

                    return cb.and(
                            predicates.toArray(
                                    new Predicate[0]
                            )
                    );
                };

        Map<String, PlaybackOptionResponse> options =
                new LinkedHashMap<>();

        for (
                ReportHistory history :
                        reportHistoryRepository.findAll(spec)
        ) {

            String deviceSn = history.getDeviceSn();
            UUID missionId = history.getMissionId();

            if (deviceSn == null ||
                    deviceSn.isBlank()) {
                continue;
            }

            /*
             * Additional defensive check.
             * The Specification already filters Company User
             * resources, but keep the response builder safe too.
             */
            if (isCompanyUser(currentUser)) {

                if (!allowedDeviceSns.contains(deviceSn)) {
                    continue;
                }

                if (missionId == null ||
                        !allowedMissionIds.contains(missionId)) {
                    continue;
                }
            }

            String key =
                    deviceSn +
                    "|" +
                    (
                            missionId != null
                                    ? missionId.toString()
                                    : ""
                    );

            if (options.containsKey(key)) {
                continue;
            }

            Device device =
                    deviceRepository
                            .findByDeviceSnAndDeletedAtIsNull(
                                    deviceSn
                            )
                            .orElse(null);

            Mission mission =
                    missionId != null
                            ? missionRepository
                                .findByMissionIdAndDeletedAtIsNull(
                                        missionId
                                )
                                .orElse(null)
                            : null;

            options.put(
                    key,
                    PlaybackOptionResponse.builder()
                            .deviceSn(deviceSn)
                            .deviceName(
                                    device != null
                                            ? device.getDeviceName()
                                            : deviceSn
                            )
                            .missionId(missionId)
                            .missionName(
                                    mission != null
                                            ? mission.getMissionName()
                                            : missionId != null
                                                ? missionId.toString()
                                                : ""
                            )
                            .build()
            );
        }

        return new ArrayList<>(options.values());
    }

    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !(authentication.getPrincipal()
                        instanceof CustomUserDetails customUserDetails)) {

            throw new RuntimeException(
                    "Authenticated user not found"
            );
        }

        return userRepository
                .findByUserIdAndDeletedAtIsNull(
                        customUserDetails.getUserId()
                )
                .orElseThrow(
                        () -> new RuntimeException(
                                "User not found"
                        )
                );
    }

    private boolean isSysAdmin(User user) {
        return userRoleRepository
                .existsByUserIdAndRoleId(
                        user.getUserId(),
                        1
                );
    }

    private boolean isCompanyUser(User user) {
        return userRoleRepository
                .existsByUserIdAndRoleId(
                        user.getUserId(),
                        3
                );
    }

    private Set<String> getAllowedDeviceSns(
            User currentUser
    ) {

        if (!isCompanyUser(currentUser)) {
            return Set.of();
        }

        if (currentUser.getDevices() == null) {
            return Set.of();
        }

        return currentUser.getDevices()
                .stream()
                .filter(device ->
                        device.getDeletedAt() == null
                )
                .map(Device::getDeviceSn)
                .filter(deviceSn ->
                        deviceSn != null &&
                        !deviceSn.isBlank()
                )
                .collect(Collectors.toSet());
    }

    private Set<UUID> getAllowedMissionIds(
            User currentUser
    ) {

        if (!isCompanyUser(currentUser)) {
            return Set.of();
        }

        if (currentUser.getMissions() == null) {
            return Set.of();
        }

        return currentUser.getMissions()
                .stream()
                .filter(mission ->
                        mission.getDeletedAt() == null
                )
                .map(Mission::getMissionId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<UUID> getAllowedSiteIds(
            User currentUser
    ) {

        if (!isCompanyUser(currentUser)) {
            return Set.of();
        }

        if (currentUser.getSites() == null) {
            return Set.of();
        }

        return currentUser.getSites()
                .stream()
                .filter(site ->
                        site.getDeletedAt() == null
                )
                .map(site -> site.getSiteId())
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private void validateCompanyAccess(
            User currentUser,
            String companyId
    ) {

        if (isSysAdmin(currentUser)) {
            return;
        }

        UUID currentCompanyId =
                currentUser.getCompanyId();

        if (currentCompanyId == null) {
            throw new AccessDeniedException(
                    "User is not assigned to a company"
            );
        }

        if (companyId != null &&
                !companyId.isBlank() &&
                !currentCompanyId.equals(
                        UUID.fromString(companyId)
                )) {

            throw new AccessDeniedException(
                    "You do not have access to this company"
            );
        }
    }

    private void validateSiteAccess(
            User currentUser,
            String siteId
    ) {

        if (!isCompanyUser(currentUser) ||
                siteId == null ||
                siteId.isBlank()) {
            return;
        }

        UUID requestedSiteId =
                UUID.fromString(siteId);

        boolean assigned =
                currentUser.getSites() != null &&
                currentUser.getSites()
                        .stream()
                        .anyMatch(site ->
                                site.getDeletedAt() == null &&
                                requestedSiteId.equals(
                                        site.getSiteId()
                                )
                        );

        if (!assigned) {
            throw new AccessDeniedException(
                    "You do not have access to this site"
            );
        }
    }

    private void validateDeviceAccess(
            User currentUser,
            String deviceSn
    ) {

        if (!isCompanyUser(currentUser) ||
                deviceSn == null ||
                deviceSn.isBlank()) {
            return;
        }

        boolean assigned =
                currentUser.getDevices() != null &&
                currentUser.getDevices()
                        .stream()
                        .anyMatch(device ->
                                device.getDeletedAt() == null &&
                                deviceSn.equals(
                                        device.getDeviceSn()
                                )
                        );

        if (!assigned) {
            throw new AccessDeniedException(
                    "You do not have access to this device"
            );
        }
    }

    private void validateMissionAccess(
            User currentUser,
            String missionId
    ) {

        if (!isCompanyUser(currentUser) ||
                missionId == null ||
                missionId.isBlank()) {
            return;
        }

        UUID requestedMissionId =
                UUID.fromString(missionId);

        boolean assigned =
                currentUser.getMissions() != null &&
                currentUser.getMissions()
                        .stream()
                        .anyMatch(mission ->
                                mission.getDeletedAt() == null &&
                                requestedMissionId.equals(
                                        mission.getMissionId()
                                )
                        );

        if (!assigned) {
            throw new AccessDeniedException(
                    "You do not have access to this mission"
            );
        }
    }

    private PlaybackListResponse toResponse(
            ReportHistory history
    ) {

        String segment =
                DateTimeUtil.formatKst(
                        history.getCreatedAt()
                );

        return PlaybackListResponse.builder()
                .segment(segment)
                .url(history.getPlaybackUrl())
                .sessionId(history.getSessionId())
                .build();
    }
}