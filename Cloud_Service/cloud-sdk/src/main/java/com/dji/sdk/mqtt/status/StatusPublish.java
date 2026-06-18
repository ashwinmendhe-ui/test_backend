package com.dji.sdk.mqtt.status;

import com.dji.sdk.mqtt.CommonTopicResponse;
import com.dji.sdk.mqtt.MqttGatewayPublish;
import com.dji.sdk.mqtt.TopicConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Publisher for sys/product/{gateway_sn}/status_reply
 */
@Component

public class StatusPublish {
    @Autowired
    private MqttGatewayPublish gatewayPublish;

    public void publish(String gatewaySn, CommonTopicResponse<?> response) {
        String topic = TopicConst.BASIC_PRE + TopicConst.PRODUCT + gatewaySn + TopicConst.STATUS_SUF + TopicConst._REPLY_SUF;
        gatewayPublish.publish(topic, 0, response);
    }
}
