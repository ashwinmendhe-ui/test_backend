package com.dji.sample.service.impl;
import com.dji.sample.service.IDeviceRedisService;
import com.dji.sample.dto.request.DeviceRequest;
import com.dji.sample.dto.response.DeviceResponse;
import com.dji.sample.entity.Company;
import com.dji.sample.entity.Device;
import com.dji.sample.entity.LiveStreamSession;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

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
    @Override
    @Transactional(readOnly = true)
    public List<DeviceResponse> getDevices(String keyword, String from, String to, UUID siteId) {
        List<Device> devices;

        if (siteId != null) {
            devices = deviceRepository.findBySite_SiteIdAndDeletedAtIsNullOrderByCreatedAtDesc(siteId);
        } else {
            devices = deviceRepository.findByDeletedAtIsNullOrderByCreatedAtDesc();
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
                .status("INACTIVE")
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
        device.setStatus("INACTIVE");

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

    @Override
    public void markDeviceOnlineForTest(String deviceSn) {
        Device device = deviceRepository.findByDeviceSnAndDeletedAtIsNull(deviceSn)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        deviceRedisService.setDeviceOnline(device);
    }

    @Override
    public void markDeviceOfflineForTest(String deviceSn) {
        deviceRedisService.delDeviceOnline(deviceSn);
    }

    private DeviceResponse toResponse(Device device) {
        Company company = device.getCompany();
        Site site = device.getSite();

        UUID missionId = null;
        String missionName = null;
        boolean hasActiveStream = false;

        if (device.getDeviceSn() != null) {
            var activeSession = liveStreamSessionRepository
                    .findFirstByDeviceSnAndSessionStatusOrderByStartedAtDesc(
                            device.getDeviceSn(),
                            "ACTIVE"
                    );

            hasActiveStream = activeSession.isPresent();

            if (activeSession.isPresent() && activeSession.get().getMissionId() != null) {
                missionId = activeSession.get().getMissionId();

                        Mission mission = missionRepository
                                .findByMissionIdAndDeletedAtIsNull(missionId)
                                .orElse(null);

                        missionName = mission != null ? mission.getMissionName() : null;
                    }
                }

        if (missionId == null && device.getMissionId() != null) {
            missionId = device.getMissionId();

            Mission mission = missionRepository
                    .findByMissionIdAndDeletedAtIsNull(missionId)
                    .orElse(null);

            missionName = mission != null ? mission.getMissionName() : null;
        }

        String responseStatus = "offline";

        if (device.getDeviceSn() != null
                && deviceRedisService.getDeviceOnline(device.getDeviceSn()) != null) {
            if (missionId != null) {
                responseStatus = "working";
            } else {
                responseStatus = "online";
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
                .deviceSn(device.getDeviceSn())
                .description(device.getDescription())
                .status(responseStatus)
                .createdAt(DateTimeUtil.formatKst(device.getCreatedAt()))
                .updatedAt(DateTimeUtil.formatKst(device.getUpdatedAt()))
                .createdDate(DateTimeUtil.formatKst(device.getCreatedAt()))
                .build();
    }
}