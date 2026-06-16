package com.dji.sample.config;

import com.dji.sdk.mqtt.ChannelName;
import com.dji.sdk.mqtt.IMqttMessageGateway;
import com.dji.sdk.mqtt.MqttGatewayPublish;
import com.dji.sdk.mqtt.services.ServicesPublish;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.ReflectionUtils;
import com.dji.sdk.mqtt.services.ServicesReplyHandler;

import java.lang.reflect.Field;
import java.util.UUID;

@Configuration
@IntegrationComponentScan(basePackageClasses = IMqttMessageGateway.class)
public class DjiMqttSdkPublishConfig {

    @Bean
    public ServicesReplyHandler servicesReplyHandler() {
        return new ServicesReplyHandler();
    }

    @Bean
    public MqttGatewayPublish mqttGatewayPublish(
            IMqttMessageGateway messageGateway,
            MqttPahoClientFactory mqttClientFactory
    ) {
        MqttGatewayPublish bean = new MqttGatewayPublish();

        setField(bean, "messageGateway", messageGateway);
        setField(bean, "mqttClientFactory", mqttClientFactory);

        return bean;
    }

    @Bean
    public ServicesPublish servicesPublish(MqttGatewayPublish mqttGatewayPublish) {
        ServicesPublish bean = new ServicesPublish();

        setField(bean, "gatewayPublish", mqttGatewayPublish);

        return bean;
    }

    @Bean
    @ServiceActivator(inputChannel = ChannelName.OUTBOUND)
    public MessageHandler djiMqttOutbound(MqttPahoClientFactory mqttClientFactory) {
        MqttPahoMessageHandler handler =
                new MqttPahoMessageHandler(UUID.randomUUID().toString(), mqttClientFactory);

        DefaultPahoMessageConverter converter = new DefaultPahoMessageConverter();
        converter.setPayloadAsBytes(true);

        handler.setAsync(true);
        handler.setDefaultQos(0);
        handler.setConverter(converter);

        return handler;
    }

    private static void setField(Object target, String fieldName, Object value) {
        Field field = ReflectionUtils.findField(target.getClass(), fieldName);
        if (field == null) {
            throw new IllegalStateException("Field not found: " + fieldName);
        }
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, target, value);
    }
}