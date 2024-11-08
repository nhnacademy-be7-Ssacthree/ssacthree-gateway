package com.nhnacademy.ssacthree_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@ConfigurationPropertiesScan
public class SsacthreeGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(SsacthreeGatewayApplication.class, args);
    }

}
