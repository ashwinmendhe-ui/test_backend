package com.dji.sample.config;

import lombok.RequiredArgsConstructor;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;

@Configuration
@RequiredArgsConstructor
public class LocalMqttConfig {

    public static final String DEVICE_STATUS_CHANNEL = "deviceStatusMqttInputChannel";

    @Value("${mqtt.host:localhost}")
    private String host;

    @Value("${mqtt.port:1883}")
    private int port;

    @Value("${mqtt.client-id:dhive-local-backend}")
    private String clientId;

    @Value("${mqtt.inbound-topic:sys/product/+/status}")
    private String inboundTopic;

    @Bean
    public MqttPahoClientFactory localMqttClientFactory() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{"tcp://" + host + ":" + port});
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setKeepAliveInterval(10);

        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel deviceStatusMqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MqttPahoMessageDrivenChannelAdapter deviceStatusMqttInbound(
            MqttPahoClientFactory localMqttClientFactory
    ) {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(
                        clientId,
                        localMqttClientFactory,
                        inboundTopic
                );

        DefaultPahoMessageConverter converter = new DefaultPahoMessageConverter();
        converter.setPayloadAsBytes(false);

        adapter.setConverter(converter);
        adapter.setQos(1);
        adapter.setOutputChannel(deviceStatusMqttInputChannel());

        return adapter;
    }
}