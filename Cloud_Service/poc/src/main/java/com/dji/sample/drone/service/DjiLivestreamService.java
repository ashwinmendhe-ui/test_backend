package com.dji.sample.drone.service;

import com.dji.sdk.cloudapi.device.PayloadIndex;
import com.dji.sdk.cloudapi.device.VideoId;
import com.dji.sdk.cloudapi.livestream.LiveStartPushRequest;
import com.dji.sdk.cloudapi.livestream.LiveStopPushRequest;
import com.dji.sdk.cloudapi.livestream.LivestreamRtmpUrl;
import com.dji.sdk.cloudapi.livestream.LiveStreamMethodEnum;
import com.dji.sdk.cloudapi.livestream.UrlTypeEnum;
import com.dji.sdk.cloudapi.livestream.VideoQualityEnum;
import com.dji.sdk.cloudapi.livestream.VideoTypeEnum;
import com.dji.sdk.common.SDKManager;
import com.dji.sdk.config.version.GatewayManager;
import com.dji.sdk.config.version.GatewayTypeEnum;
import com.dji.sdk.mqtt.services.ServicesPublish;
import com.dji.sdk.mqtt.services.ServicesReplyData;
import com.dji.sdk.mqtt.services.TopicServicesResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DjiLivestreamService {

    private static final long DEFAULT_TIMEOUT = 20_000;

    private final ServicesPublish servicesPublish;

    public void registerGateway(String gatewaySn, String droneSn) {
        SDKManager.registerDevice(new GatewayManager(
                gatewaySn,
                droneSn,
                GatewayTypeEnum.RC2,
                "1.2.0",
                "1.2.0"
        ));

        log.info("[DJI][SDK_REGISTER] gatewaySn={}, droneSn={}", gatewaySn, droneSn);
    }

    public void startPush(
            String gatewaySn,
            String droneSn,
            String payloadIndex,
            String videoType,
            Integer videoQuality,
            String rtmpUrl
    ) {
        registerGateway(gatewaySn, droneSn);

        VideoId videoId = new VideoId()
                .setDroneSn(droneSn)
                .setPayloadIndex(new PayloadIndex(payloadIndex))
                .setVideoType(VideoTypeEnum.find(videoType));

        LiveStartPushRequest request = new LiveStartPushRequest()
                .setUrl(new LivestreamRtmpUrl().setUrl(rtmpUrl))
                .setUrlType(UrlTypeEnum.RTMP)
                .setVideoId(videoId)
                .setVideoQuality(VideoQualityEnum.find(videoQuality));

        log.info("[DJI][LIVE_START_PUSH] gatewaySn={}, droneSn={}, videoId={}, rtmpUrl={}",
                gatewaySn, droneSn, videoId, rtmpUrl);

        TopicServicesResponse<ServicesReplyData<String>> response =
                servicesPublish.publish(
                        new TypeReference<String>() {},
                        gatewaySn,
                        LiveStreamMethodEnum.LIVE_START_PUSH.getMethod(),
                        request,
                        DEFAULT_TIMEOUT
                );

        log.info("[DJI][LIVE_START_PUSH] response={}", response);

        if (response == null
                || response.getData() == null
                || response.getData().getResult() == null
                || !response.getData().getResult().isSuccess()) {
            throw new RuntimeException("DJI live_start_push failed: " + response);
        }
    }

    public void stopPush(
                String gatewaySn,
                String droneSn,
                String payloadIndex,
                String videoType
        ) {
        VideoId videoId = new VideoId()
                .setDroneSn(droneSn)
                .setPayloadIndex(new PayloadIndex(payloadIndex))
                .setVideoType(VideoTypeEnum.find(videoType));

        LiveStopPushRequest request = new LiveStopPushRequest()
                .setVideoId(videoId);

        log.info(
                "[DJI][LIVE_STOP_PUSH] gatewaySn={}, droneSn={}, videoId={}",
                gatewaySn,
                droneSn,
                videoId
        );

        TopicServicesResponse<ServicesReplyData<String>> response =
                servicesPublish.publish(
                        new TypeReference<String>() {},
                        gatewaySn,
                        LiveStreamMethodEnum.LIVE_STOP_PUSH.getMethod(),
                        request,
                        DEFAULT_TIMEOUT
                );

        log.info(
                "[DJI][LIVE_STOP_PUSH] response={}",
                response
        );

        if (response == null
                || response.getData() == null
                || response.getData().getResult() == null
                || !response.getData().getResult().isSuccess()) {

                throw new RuntimeException(
                        "DJI live_stop_push failed: " + response
                );
        }
        }
}