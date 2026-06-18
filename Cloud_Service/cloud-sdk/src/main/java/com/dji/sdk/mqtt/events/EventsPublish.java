package com.dji.sdk.mqtt.events;

import com.dji.sdk.mqtt.CommonTopicResponse;
import com.dji.sdk.mqtt.MqttGatewayPublish;
import com.dji.sdk.mqtt.TopicConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Publisher for thing/product/{gateway_sn}/events_reply
 */
@Component

public class EventsPublish {
    @Autowired
    private MqttGatewayPublish gatewayPublish;

    public void publish(String gatewaySn, CommonTopicResponse<?> response) {
        String topic = TopicConst.THING_MODEL_PRE + TopicConst.PRODUCT + gatewaySn + TopicConst.EVENTS_SUF + TopicConst._REPLY_SUF;
        gatewayPublish.publish(topic, 0, response);
    }
}
