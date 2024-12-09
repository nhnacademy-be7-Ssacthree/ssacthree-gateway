package com.nhnacademy.ssacthree_gateway.config;

import com.nhnacademy.ssacthree_gateway.filter.JWTFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RouteLocatorConfig {

    private final JWTFilter jwtFilter;

    @Bean
    public RouteLocator myRoute(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("shop-service",
                p -> p.path("/api/shop/**")
                    .filters(f -> f.filter(jwtFilter.apply(new Object()))) // null 대신 빈 객체 전달
                    .uri("lb://SHOP-SERVICE")
            )
            .route("auth-service",
                p -> p.path("/api/auth/**")
                    .filters(f -> f.filter(jwtFilter.apply(new Object()))) // null 대신 빈 객체 전달
                    .uri("lb://AUTH-SERVICE")
            )
            .build();
    }
}
