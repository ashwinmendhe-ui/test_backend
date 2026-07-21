package com.dji.sample.drone.service;

import com.dji.sample.drone.model.StreamSource;
import com.dji.sample.dto.request.StartStreamRequest;
import com.dji.sample.entity.Device;
import com.dji.sample.entity.SubDevice;
import com.dji.sample.repository.SubDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamSourceResolver {

    private final SubDeviceRepository subDeviceRepository;

    /**
     * Resolves the physical stream source when starting a stream.
     */
    public StreamSource resolve(
            StartStreamRequest request,
            Device device
    ) {
        String requestDeviceSn = request.getDeviceSn();

        if (!isDrone(device)) {
            return defaultSource(requestDeviceSn);
        }

        SubDevice subDevice =
                subDeviceRepository
                        .findFirstByDeviceSnAndDeletedAtIsNullOrderByIdAsc(
                                requestDeviceSn
                        )
                        .orElse(null);

        log.info(
                "[LIVE][SUB_DEVICE] requestDeviceSn={}, found={}, subSn={}",
                requestDeviceSn,
                subDevice != null,
                subDevice != null ? subDevice.getSn() : null
        );

        String streamDeviceSn = requestDeviceSn;
        String gatewaySn = requestDeviceSn;
        String payloadIndex = resolvePayloadIndexFromRequest(request);
        String videoType = resolveVideoTypeFromRequest(request);

        if (subDevice != null
                && subDevice.getSn() != null
                && !subDevice.getSn().isBlank()) {

            streamDeviceSn = subDevice.getSn();

            Integer type =
                    subDevice.getType() != null
                            ? subDevice.getType()
                            : 99;

            Integer subType =
                    subDevice.getSubType() != null
                            ? subDevice.getSubType()
                            : 0;

            payloadIndex = type + "-" + subType + "-0";

        } else if (request.getVideoId() != null
                && request.getVideoId().getDroneSn() != null
                && !request.getVideoId().getDroneSn().isBlank()) {

            streamDeviceSn =
                    request.getVideoId().getDroneSn();
        }

        String videoId =
                buildVideoId(
                        streamDeviceSn,
                        payloadIndex,
                        videoType
                );

        StreamSource source =
                new StreamSource(
                        requestDeviceSn,
                        streamDeviceSn,
                        gatewaySn,
                        streamDeviceSn,
                        payloadIndex,
                        videoType,
                        videoId
                );

        logResolvedSource(source, "START");

        return source;
    }

    /**
     * Resolves the same physical stream source when stopping or cleaning up.
     */
    public StreamSource resolveForDeviceSn(
            String requestDeviceSn
    ) {
        SubDevice subDevice =
                subDeviceRepository
                        .findFirstByDeviceSnAndDeletedAtIsNullOrderByIdAsc(
                                requestDeviceSn
                        )
                        .orElse(null);

        if (subDevice == null
                || subDevice.getSn() == null
                || subDevice.getSn().isBlank()) {

            StreamSource source =
                    defaultSource(requestDeviceSn);

            logResolvedSource(source, "STOP");

            return source;
        }

        Integer type =
                subDevice.getType() != null
                        ? subDevice.getType()
                        : 99;

        Integer subType =
                subDevice.getSubType() != null
                        ? subDevice.getSubType()
                        : 0;

        String payloadIndex =
                type + "-" + subType + "-0";

        String videoType = "normal";

        String videoId =
                buildVideoId(
                        subDevice.getSn(),
                        payloadIndex,
                        videoType
                );

        StreamSource source =
                new StreamSource(
                        requestDeviceSn,
                        subDevice.getSn(),
                        requestDeviceSn,
                        subDevice.getSn(),
                        payloadIndex,
                        videoType,
                        videoId
                );

        logResolvedSource(source, "STOP");

        return source;
    }

    private StreamSource defaultSource(
            String deviceSn
    ) {
        return new StreamSource(
                deviceSn,
                deviceSn,
                deviceSn,
                deviceSn,
                "99-0-0",
                "normal",
                deviceSn
        );
    }

    private String resolvePayloadIndexFromRequest(
            StartStreamRequest request
    ) {
        if (request.getVideoId() != null
                && request.getVideoId().getPayloadIndex() != null) {

            return request.getVideoId()
                    .getPayloadIndex()
                    .getType()
                    + "-"
                    + request.getVideoId()
                    .getPayloadIndex()
                    .getSubType()
                    + "-"
                    + request.getVideoId()
                    .getPayloadIndex()
                    .getPosition();
        }

        return "99-0-0";
    }

    private String resolveVideoTypeFromRequest(
            StartStreamRequest request
    ) {
        if (request.getVideoId() != null
                && request.getVideoId().getVideoType() != null
                && !request.getVideoId().getVideoType().isBlank()) {

            return request.getVideoId().getVideoType();
        }

        return "normal";
    }

    private String buildVideoId(
            String droneSn,
            String payloadIndex,
            String videoType
    ) {
        return droneSn
                + "/"
                + payloadIndex
                + "/"
                + videoType
                + "-0";
    }

    private void logResolvedSource(
            StreamSource source,
            String operation
    ) {
        log.info(
                "[LIVE][STREAM_SOURCE][{}] requestDeviceSn={}, streamDeviceSn={}, gatewaySn={}, droneSn={}, payloadIndex={}, videoType={}, videoId={}",
                operation,
                source.requestDeviceSn(),
                source.streamDeviceSn(),
                source.gatewaySn(),
                source.droneSn(),
                source.payloadIndex(),
                source.videoType(),
                source.videoId()
        );
    }

    private boolean isDrone(
            Device device
    ) {
        String type = device.getDeviceType();

        return "Drone".equalsIgnoreCase(type)
                || "드론".equals(type);
    }
}