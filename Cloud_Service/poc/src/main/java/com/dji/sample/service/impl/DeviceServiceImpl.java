package com.dji.sample.service.impl;

import com.dji.sample.dto.request.DeviceRequest;
import com.dji.sample.dto.response.DeviceResponse;
import com.dji.sample.entity.Company;
import com.dji.sample.entity.Device;
import com.dji.sample.entity.Site;
import com.dji.sample.repository.CompanyRepository;
import com.dji.sample.repository.DeviceRepository;
import com.dji.sample.repository.SiteRepository;
import com.dji.sample.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dji.sample.util.DateTimeUtil;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final CompanyRepository companyRepository;
    private final SiteRepository siteRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DeviceResponse> getDevices(String keyword, String from, String to, UUID siteId) {
        List<Device> devices;

        if (siteId != null) {
            devices = deviceRepository.findBySite_SiteId(siteId);
        } else {
            devices = deviceRepository.findAll();
        }

        return devices.stream()
                .filter(device -> matchesKeyword(device, keyword))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceResponse getDevice(UUID deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        return toResponse(device);
    }

    @Override
    public DeviceResponse createDevice(DeviceRequest request) {
        Company company = getCompany(request.getCompanyId());
        Site site = getSite(request.getSiteId());

        Device device = Device.builder()
                .deviceId(request.getDeviceId() != null ? request.getDeviceId() : UUID.randomUUID())
                .deviceName(request.getDeviceName())
                .deviceType(request.getDeviceType())
                .brandName(request.getBrandName())
                .model(request.getModel())
                .deviceSn(request.getDeviceSn())
                .description(request.getDescription())
                .status("INACTIVE")
                .company(company)
                .site(site)
                .build();

        return toResponse(deviceRepository.save(device));
    }

    @Override
    public DeviceResponse updateDevice(UUID deviceId, DeviceRequest request) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        device.setDeviceName(request.getDeviceName());
        device.setDeviceType(request.getDeviceType());
        device.setBrandName(request.getBrandName());
        device.setModel(request.getModel());
        device.setDeviceSn(request.getDeviceSn());
        device.setDescription(request.getDescription());
        device.setCompany(getCompany(request.getCompanyId()));
        device.setSite(getSite(request.getSiteId()));

        return toResponse(deviceRepository.save(device));
    }

    @Override
    public void deleteDevice(UUID deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        deviceRepository.delete(device);
    }

    private Company getCompany(UUID companyId) {
        if (companyId == null) {
            return null;
        }

        return companyRepository.findById(companyId)
                .orElse(null);
    }

    private Site getSite(UUID siteId) {
        if (siteId == null) {
            return null;
        }

        return siteRepository.findById(siteId)
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
                || (device.getCompany() != null && contains(device.getCompany().getCompanyName(), lowerKeyword))
                || (device.getSite() != null && contains(device.getSite().getName(), lowerKeyword));
             }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private DeviceResponse toResponse(Device device) {
        Company company = device.getCompany();
        Site site = device.getSite();

        return DeviceResponse.builder()
                .deviceId(device.getDeviceId())
                .deviceName(device.getDeviceName())
                .companyId(company != null ? company.getCompanyId() : null)
                .companyName(company != null ? company.getCompanyName() : null)
                .siteId(site != null ? site.getSiteId() : null)
                .siteName(site != null ? site.getName() : null)
                .deviceType(device.getDeviceType())
                .brandName(device.getBrandName())
                .model(device.getModel())
                .deviceSn(device.getDeviceSn())
                .description(device.getDescription())
                .status(device.getStatus())
                .createdAt(DateTimeUtil.formatKst(device.getCreatedAt()))
                .updatedAt(DateTimeUtil.formatKst(device.getUpdatedAt()))
                .createdDate(DateTimeUtil.formatKst(device.getCreatedAt()))
                .build();
    }
}