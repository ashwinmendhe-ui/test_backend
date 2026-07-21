package com.dji.sample.drone.service;

import com.dji.sdk.cloudapi.device.UpdateTopo;
import com.dji.sdk.cloudapi.device.UpdateTopoSubDevice;
import com.dji.sdk.common.SDKManager;
import com.dji.sdk.mqtt.ChannelName;
import com.dji.sdk.mqtt.MqttReply;
import com.dji.sdk.mqtt.status.TopicStatusRequest;
import com.dji.sdk.mqtt.status.TopicStatusResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
public class DjiDeviceStatusHandler {

    /**
     * Handles gateway and sub-device ONLINE topology messages.
     *
     * Flow:
     * INBOUND_STATUS
     * -> StatusRouter
     * -> INBOUND_STATUS_ONLINE
     * -> this method
     * -> OUTBOUND_STATUS
     * -> DJI status_reply
     */
    @ServiceActivator(
            inputChannel = ChannelName.INBOUND_STATUS_ONLINE,
            outputChannel = ChannelName.OUTBOUND_STATUS
    )
    public TopicStatusResponse<MqttReply> updateTopoOnline(
            TopicStatusRequest<UpdateTopo> request,
            MessageHeaders headers
    ) {
        String gatewaySn = request == null ? null : request.getFrom();

        try {
            validateOnlineRequest(request);

            UpdateTopo topology = request.getData();
            List<UpdateTopoSubDevice> subDevices = topology.getSubDevices();

            /*
             * A DJI gateway normally reports its connected aircraft as the
             * first sub-device. GatewayManager supports one drone SN for each
             * gateway registration, so register the first valid sub-device.
             */
            UpdateTopoSubDevice drone = subDevices.stream()
                    .filter(item -> item != null && StringUtils.hasText(item.getSn()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "DJI online topology does not contain a valid sub-device SN"
                    ));

            SDKManager.registerDevice(
                    gatewaySn,
                    drone.getSn(),
                    topology.getDomain(),
                    topology.getType(),
                    topology.getSubType(),
                    topology.getThingVersion(),
                    drone.getThingVersion()
            );

            log.info(
                    "[DJI][STATUS_ONLINE][SDK_REGISTER] gatewaySn={}, droneSn={}, "
                            + "gatewayDomain={}, gatewayType={}, gatewaySubType={}, "
                            + "gatewayThingVersion={}, droneThingVersion={}",
                    gatewaySn,
                    drone.getSn(),
                    topology.getDomain(),
                    topology.getType(),
                    topology.getSubType(),
                    topology.getThingVersion(),
                    drone.getThingVersion()
            );

            return successResponse(request);

        } catch (Exception exception) {
            log.error(
                    "[DJI][STATUS_ONLINE][FAILED] gatewaySn={}, request={}",
                    gatewaySn,
                    request,
                    exception
            );

            return errorResponse(
                    request,
                    "Failed to register DJI device: " + safeMessage(exception)
            );
        }
    }

    /**
     * Handles gateway/sub-device OFFLINE topology messages.
     *
     * StatusRouter sends a request to this channel when sub_devices is absent
     * or empty.
     */
    @ServiceActivator(
            inputChannel = ChannelName.INBOUND_STATUS_OFFLINE,
            outputChannel = ChannelName.OUTBOUND_STATUS
    )
    public TopicStatusResponse<MqttReply> updateTopoOffline(
            TopicStatusRequest<UpdateTopo> request,
            MessageHeaders headers
    ) {
        String gatewaySn = request == null ? null : request.getFrom();

        try {
            if (!StringUtils.hasText(gatewaySn)) {
                throw new IllegalArgumentException(
                        "Gateway SN is missing from DJI status topic"
                );
            }

            SDKManager.logoutDevice(gatewaySn);

            log.info(
                    "[DJI][STATUS_OFFLINE][SDK_LOGOUT] gatewaySn={}",
                    gatewaySn
            );

            return successResponse(request);

        } catch (Exception exception) {
            log.error(
                    "[DJI][STATUS_OFFLINE][FAILED] gatewaySn={}, request={}",
                    gatewaySn,
                    request,
                    exception
            );

            return errorResponse(
                    request,
                    "Failed to unregister DJI device: " + safeMessage(exception)
            );
        }
    }

    private void validateOnlineRequest(
            TopicStatusRequest<UpdateTopo> request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "DJI online status request is null"
            );
        }

        if (!StringUtils.hasText(request.getFrom())) {
            throw new IllegalArgumentException(
                    "Gateway SN is missing from DJI status topic"
            );
        }

        UpdateTopo topology = request.getData();

        if (topology == null) {
            throw new IllegalArgumentException(
                    "DJI online topology data is missing"
            );
        }

        if (topology.getDomain() == null
                || topology.getType() == null
                || topology.getSubType() == null) {
            throw new IllegalArgumentException(
                    "DJI gateway domain/type/subType is incomplete"
            );
        }

        if (CollectionUtils.isEmpty(topology.getSubDevices())) {
            throw new IllegalArgumentException(
                    "DJI online topology does not contain sub-devices"
            );
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private TopicStatusResponse<MqttReply> successResponse(
            TopicStatusRequest<?> request
    ) {
        return new TopicStatusResponse<MqttReply>()
                .setTid(request == null ? null : request.getTid())
                .setBid(request == null ? null : request.getBid())
                .setMethod(request == null ? null : request.getMethod())
                .setTimestamp(System.currentTimeMillis())
                .setData(MqttReply.success());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private TopicStatusResponse<MqttReply> errorResponse(
            TopicStatusRequest<?> request,
            String message
    ) {
        return new TopicStatusResponse<MqttReply>()
                .setTid(request == null ? null : request.getTid())
                .setBid(request == null ? null : request.getBid())
                .setMethod(request == null ? null : request.getMethod())
                .setTimestamp(System.currentTimeMillis())
                .setData(MqttReply.error(message));
    }

    private String safeMessage(Exception exception) {
        if (exception == null) {
            return "Unknown error";
        }

        return StringUtils.hasText(exception.getMessage())
                ? exception.getMessage()
                : exception.getClass().getSimpleName();
    }
}