package com.olehprukhnytskyi.macrotrackercloudgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class MacroTrackerCloudGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(MacroTrackerCloudGatewayApplication.class, args);
    }

}
