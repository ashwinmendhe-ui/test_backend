package com.dji.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = {
        "com.dji.sample",
        "com.dji.sdk.common",
        "com.dji.sdk.config"
})
@ConfigurationPropertiesScan(basePackages = "com.dji.sample")
public class DhiveApplication {
    public static void main(String[] args) {
        SpringApplication.run(DhiveApplication.class, args);
    }
}