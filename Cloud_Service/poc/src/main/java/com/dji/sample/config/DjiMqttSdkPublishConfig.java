package com.dji.sample.config;

import com.dji.sdk.mqtt.ChannelName;
import com.dji.sdk.mqtt.IMqttMessageGateway;
import com.dji.sdk.mqtt.MqttGatewayPublish;
import com.dji.sdk.mqtt.services.ServicesPublish;
import com.dji.sdk.mqtt.services.ServicesReplyHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.UUID;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import com.dji.sdk.mqtt.status.StatusRouter;

@Configuration
@IntegrationComponentScan(basePackageClasses = IMqttMessageGateway.class)
public class DjiMqttSdkPublishConfig {

    /*
     * ServicesReplyHandler belongs to com.dji.sdk, which is outside the
     * com.dji.sample component-scan package. Register it explicitly.
     */
    @Bean
    public ServicesReplyHandler servicesReplyHandler() {
        return new ServicesReplyHandler();
    }

    /*
     * Connect DJI services_reply MQTT messages to the SDK reply handler.
     *
     * Required for synchronous ServicesPublish calls such as
     * live_start_push to receive their matching response through Chan.
     */
    @Bean
    public IntegrationFlow djiServicesReplyFlow(
            ServicesReplyHandler servicesReplyHandler
    ) {
        return IntegrationFlow
                .from(ChannelName.INBOUND_SERVICES_REPLY)
                .handle(servicesReplyHandler, "servicesReply")
                .get();
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
    public ServicesPublish servicesPublish(
            MqttGatewayPublish mqttGatewayPublish
    ) {
        ServicesPublish bean = new ServicesPublish();

        setField(bean, "gatewayPublish", mqttGatewayPublish);

        return bean;
    }


    @Bean
public StatusRouter statusRouter(
        MqttGatewayPublish mqttGatewayPublish
) {
    StatusRouter router = new StatusRouter();

    setField(router, "gatewayPublish", mqttGatewayPublish);

    return router;
}

@Bean
public IntegrationFlow djiStatusRouterFlow(
        StatusRouter statusRouter
) {
    return statusRouter.statusRouterFlow();
}

@Bean
public IntegrationFlow djiStatusReplyFlow(
        StatusRouter statusRouter
) {
    return statusRouter.replySuccessStatus();
}

    @Bean
    @ServiceActivator(inputChannel = ChannelName.OUTBOUND)
    public MessageHandler djiMqttOutbound(
            MqttPahoClientFactory mqttClientFactory
    ) {
        MqttPahoMessageHandler handler =
                new MqttPahoMessageHandler(
                        UUID.randomUUID().toString(),
                        mqttClientFactory
                );

        DefaultPahoMessageConverter converter =
                new DefaultPahoMessageConverter();

        converter.setPayloadAsBytes(true);

        handler.setAsync(true);
        handler.setDefaultQos(0);
        handler.setConverter(converter);

        return handler;
    }


    @Bean(name = ChannelName.INBOUND_STATUS)
        public MessageChannel djiInboundStatusChannel() {
        return new DirectChannel();
        }

        @Bean(name = ChannelName.INBOUND_STATUS_ONLINE)
        public MessageChannel djiInboundStatusOnlineChannel() {
        return new DirectChannel();
        }

        @Bean(name = ChannelName.INBOUND_STATUS_OFFLINE)
        public MessageChannel djiInboundStatusOfflineChannel() {
        return new DirectChannel();
        }

        @Bean(name = ChannelName.OUTBOUND_STATUS)
        public MessageChannel djiOutboundStatusChannel() {
        return new DirectChannel();
        }


    private static void setField(
            Object target,
            String fieldName,
            Object value
    ) {
        Field field = ReflectionUtils.findField(
                target.getClass(),
                fieldName
        );

        if (field == null) {
            throw new IllegalStateException(
                    "Field not found: " + fieldName
            );
        }

        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, target, value);
    }
}