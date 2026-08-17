package com.dji.sample.service.impl;
import com.dji.sample.service.IDeviceRedisService;
import com.dji.sample.dto.request.DeviceRequest;
import com.dji.sample.dto.response.DeviceResponse;
import com.dji.sample.entity.Company;
import com.dji.sample.entity.Device;
import com.dji.sample.entity.Mission;
import com.dji.sample.entity.Site;
import com.dji.sample.repository.CompanyRepository;
import com.dji.sample.repository.DeviceRepository;
import com.dji.sample.repository.LiveStreamSessionRepository;
import com.dji.sample.repository.MissionRepository;
import com.dji.sample.repository.SiteRepository;
import com.dji.sample.service.DeviceService;
import com.dji.sample.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import com.dji.sample.robot.dto.response.RobotTelemetryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dji.sample.entity.SubDevice;
import com.dji.sample.repository.SubDeviceRepository;
import com.dji.sample.entity.User;
import com.dji.sample.repository.UserRepository;
import com.dji.sample.repository.UserRoleRepository;
import com.dji.sample.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


@Service
@RequiredArgsConstructor
@Transactional
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final CompanyRepository companyRepository;
    private final SiteRepository siteRepository;
    private final LiveStreamSessionRepository liveStreamSessionRepository;
    private final MissionRepository missionRepository;
    private final IDeviceRedisService deviceRedisService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final SubDeviceRepository subDeviceRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DeviceResponse> getDevices(
            String keyword,
            String from,
            String to,
            UUID siteId,
            String scope
    ) {
        User currentUser = getCurrentUser();
        UUID userId = currentUser.getUserId();

        boolean isSysAdmin = userRoleRepository.existsByUserIdAndRoleId(userId, 1);
        boolean isCompanyUser = userRoleRepository.existsByUserIdAndRoleId(userId, 3);
        
        List<Device> devices;

        if (isSysAdmin) {

                devices = siteId != null
                        ? deviceRepository
                                .findBySite_SiteIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                                        siteId
                                )
                        : deviceRepository
                                .findByDeletedAtIsNullOrderByCreatedAtDesc();

            } else if (isCompanyUser) {

                UUID currentCompanyId = currentUser.getCompanyId();

                devices = currentUser.getDevices() == null
                        ? List.of()
                        : currentUser.getDevices().stream()
                                .filter(device -> device.getDeletedAt() == null)
                                .filter(device ->
                                        device.getCompany() != null &&
                                        currentCompanyId != null &&
                                        currentCompanyId.equals(
                                                device.getCompany().getCompanyId()
                                        )
                                )
                                .filter(device ->
                                        siteId == null ||
                                        (
                                            device.getSite() != null &&
                                            siteId.equals(
                                                device.getSite().getSiteId()
                                            )
                                        )
                                )
                                .toList();

            } else {

                UUID companyId = currentUser.getCompanyId();

                if (companyId == null) {
                    devices = List.of();
                } else if (siteId != null) {
                    devices =
                            deviceRepository
                                    .findBySite_SiteIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                                            siteId
                                    )
                                    .stream()
                                    .filter(device ->
                                            device.getCompany() != null &&
                                            companyId.equals(
                                                    device.getCompany().getCompanyId()
                                            )
                                    )
                                    .toList();
                } else {
                    devices =
                            deviceRepository
                                    .findByCompany_CompanyIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                                            companyId
                                    );
                }
            }

        return devices.stream()
                .filter(device -> matchesKeyword(device, keyword))
                .map(this::toResponse)
                .toList();
    }
    @Override
    @Transactional(readOnly = true)
    public DeviceResponse getDevice(UUID deviceId) {
        Device device = deviceRepository.findByDeviceIdAndDeletedAtIsNull(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        return toResponse(device);
    }

    @Override
    public DeviceResponse createDevice(DeviceRequest request) {
        Company company = getCompany(request.getCompanyId());
        Site site = getSite(request.getSiteId());

        String deviceSn = request.getDeviceSn() != null
                ? request.getDeviceSn().trim()
                : "";

        if (deviceSn.isBlank()) {
            throw new RuntimeException("Serial number is required");
        }

        if (deviceRepository.existsByDeviceSn(deviceSn)) {
            throw new RuntimeException("Serial number already exists");
        }

        String deviceType = normalizeDeviceType(request.getDeviceType());

        Device device = Device.builder()
                .deviceId(request.getDeviceId() != null ? request.getDeviceId() : UUID.randomUUID())
                .deviceName(request.getDeviceName())
                .deviceType(deviceType)
                .brandName(request.getBrandName())
                .model(request.getModel())
                .deviceSn(deviceSn)
                .description(request.getDescription())
                .status("inactive")
                .company(company)
                .site(site)
                .domain(null)
                .type(deviceType)
                .subType(null)
                .thingVersion(null)
                .build();

        return toResponse(deviceRepository.save(device));
    }

    @Override
    public DeviceResponse updateDevice(UUID deviceId, DeviceRequest request) {
        Device device = deviceRepository.findByDeviceIdAndDeletedAtIsNull(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        String deviceSn = request.getDeviceSn() != null
                ? request.getDeviceSn().trim()
                : "";

        if (deviceSn.isBlank()) {
            throw new RuntimeException("Serial number is required");
        }

        deviceRepository.findByDeviceSnAndDeletedAtIsNull(deviceSn)
                .filter(existing -> !existing.getDeviceId().equals(deviceId))
                .ifPresent(existing -> {
                    throw new RuntimeException("Serial number already exists");
                });

        String deviceType = normalizeDeviceType(request.getDeviceType());

        device.setDeviceName(request.getDeviceName());
        device.setDeviceType(deviceType);
        device.setBrandName(request.getBrandName());
        device.setModel(request.getModel());
        device.setDeviceSn(deviceSn);
        device.setDescription(request.getDescription());
        device.setCompany(getCompany(request.getCompanyId()));
        device.setSite(getSite(request.getSiteId()));
        device.setType(deviceType);

        return toResponse(deviceRepository.save(device));
    }
    @Override
    public void deleteDevice(UUID deviceId) {
        Device device = deviceRepository.findByDeviceIdAndDeletedAtIsNull(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        device.setDeletedAt(OffsetDateTime.now());
        device.setStatus("inactive");

        deviceRepository.save(device);
    }

    private Company getCompany(UUID companyId) {
        if (companyId == null) {
            return null;
        }

        return companyRepository.findByCompanyIdAndDeletedAtIsNull(companyId)
                .orElse(null);
    }

    private Site getSite(UUID siteId) {
        if (siteId == null) {
            return null;
        }

        return siteRepository.findBySiteIdAndDeletedAtIsNull(siteId)
                .orElse(null);
    }

    private boolean matchesKeyword(Device device, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }

        String lowerKeyword = keyword.toLowerCase();

        return contains(device.getDeviceName(), lowerKeyword)
                || contains(device.getDeviceSn(), lowerKeyword)
                || contains(device.getDeviceType(), lowerKeyword)
                || contains(device.getBrandName(), lowerKeyword)
                || contains(device.getModel(), lowerKeyword)
                || contains(device.getStatus(), lowerKeyword)
                || (device.getCompany() != null && contains(device.getCompany().getName(), lowerKeyword))
                || (device.getSite() != null && contains(device.getSite().getName(), lowerKeyword));
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private String normalizeDeviceType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();

        if ("DRONE".equalsIgnoreCase(trimmed)) {
            return "Drone";
        }

        if ("ROBOT".equalsIgnoreCase(trimmed)
                || "Quadruped Robot".equalsIgnoreCase(trimmed)) {
            return "Robot";
        }

        return trimmed;
    }


    private RobotTelemetryResponse getTelemetryResponse(String deviceSn) {
        if (deviceSn == null || deviceSn.isBlank()) {
            return null;
        }

        String telemetryJson = deviceRedisService.getRobotTelemetry(deviceSn);

        if (telemetryJson == null || telemetryJson.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(telemetryJson, RobotTelemetryResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RobotTelemetryResponse getTelemetryByDeviceSn(String deviceSn) {
        return getTelemetryResponse(deviceSn);
    }

    private String resolveStreamDeviceSn(String deviceSn) {
            if (deviceSn == null || deviceSn.isBlank()) {
                return deviceSn;
            }

            return subDeviceRepository
                    .findFirstByDeviceSnAndDeletedAtIsNullOrderByIdAsc(deviceSn)
                    .map(SubDevice::getSn)
                    .filter(sn -> sn != null && !sn.isBlank())
                    .orElse(deviceSn);
        }

    private User getCurrentUser() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails customUserDetails)) {
                throw new RuntimeException("Authenticated user not found");
            }

            return userRepository.findByUserIdAndDeletedAtIsNull(customUserDetails.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }

    private DeviceResponse toResponse(Device device) {
    Company company = device.getCompany();
    Site site = device.getSite();

    UUID missionId = null;
    String missionName = null;

    String responseStatus = "offline";

    String deviceSn = device.getDeviceSn();

    boolean isOnline =
            deviceSn != null
                    && !deviceSn.isBlank()
                    && deviceRedisService.getDeviceOnline(deviceSn) != null;

    /*
     * State rule:
     *
     * OFFLINE
     *   -> status = offline
     *   -> mission = null
     *
     * ONLINE + no ACTIVE stream
     *   -> status = online
     *   -> mission = null
     *
     * ONLINE + ACTIVE stream
     *   -> status = working
     *   -> mission = active session mission
     */
    if (isOnline) {

        responseStatus = "online";

        String streamDeviceSn = resolveStreamDeviceSn(deviceSn);

        var activeSession =
                liveStreamSessionRepository
                        .findFirstByDeviceSnAndSessionStatusOrderByStartedAtDesc(
                                streamDeviceSn,
                                "ACTIVE"
                        );

        if (activeSession.isPresent()) {

            responseStatus = "working";

            if (activeSession.get().getMissionId() != null) {
                missionId = activeSession.get().getMissionId();

                Mission mission =
                        missionRepository
                                .findByMissionIdAndDeletedAtIsNull(missionId)
                                .orElse(null);

                missionName =
                        mission != null
                                ? mission.getMissionName()
                                : null;
            }
        }
    }

    return DeviceResponse.builder()
            .deviceId(device.getDeviceId())
            .deviceName(device.getDeviceName())
            .companyId(company != null ? company.getCompanyId() : null)
            .companyName(company != null ? company.getName() : null)
            .siteId(site != null ? site.getSiteId() : null)
            .siteName(site != null ? site.getName() : null)

            .missionId(missionId)
            .missionName(missionName)

            .deviceType(device.getDeviceType())
            .brandName(device.getBrandName())
            .model(device.getModel())
            .deviceSn(deviceSn)
            .description(device.getDescription())

            .status(responseStatus)

            .createdAt(DateTimeUtil.formatKst(device.getCreatedAt()))
            .updatedAt(DateTimeUtil.formatKst(device.getUpdatedAt()))
            .createdDate(DateTimeUtil.formatKst(device.getCreatedAt()))

            .telemetry(getTelemetryResponse(deviceSn))

            .build();
}
}