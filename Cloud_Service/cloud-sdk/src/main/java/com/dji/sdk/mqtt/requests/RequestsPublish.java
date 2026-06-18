package com.dji.sdk.mqtt.requests;

import com.dji.sdk.mqtt.CommonTopicResponse;
import com.dji.sdk.mqtt.MqttGatewayPublish;
import com.dji.sdk.mqtt.TopicConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Publisher for thing/product/{gateway_sn}/requests_reply
 */
@Component

public class RequestsPublish {
    @Autowired
    private MqttGatewayPublish gatewayPublish;

    public void publish(String gatewaySn, CommonTopicResponse<?> response) {
        String topic = TopicConst.THING_MODEL_PRE + TopicConst.PRODUCT + gatewaySn + TopicConst.REQUESTS_SUF + TopicConst._REPLY_SUF;
        gatewayPublish.publish(topic, 0, response);
    }
}
