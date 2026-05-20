package com.dji.sample;

import com.dji.sample.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class DhiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(DhiveApplication.class, args);
    }
}