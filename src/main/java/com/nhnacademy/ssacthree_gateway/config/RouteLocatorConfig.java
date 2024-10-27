package com.nhnacademy.ssacthree_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteLocatorConfig {
    @Bean
    public RouteLocator myRoute(RouteLocatorBuilder builder) {
        RouteLocator routeLocator = builder.routes().build();

        return builder.routes()
                //해당 경로로 요청이 오면, 각 서비스로 로드밸런싱
                .route("shop-service",
                        //API 명세 수정 필요
                        p->p.path("api/shop/**").and()
                                .uri("lb://SHOP-SERVICE")
                )
                .route("auth-service",
                        //auth로 통일할지?
                        p -> p.path("api/members/**").uri("lb://AUTH-SERVICE")
                )
                .build();
    }
}
