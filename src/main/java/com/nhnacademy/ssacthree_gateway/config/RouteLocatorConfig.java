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

    private final JWTFilter jwtUtil;

    @Bean
    public RouteLocator myRoute(RouteLocatorBuilder builder, JWTFilter jWTFilter) {
        return builder.routes()
            //해당 경로로 요청이 오면, 각 서비스로 로드밸런싱(lb)
            .route("shop-service",
                p -> p.path("/api/shop/**")
                    .filters(f -> f.filter(jwtUtil.apply(new JWTFilter.Config())))
                    .uri("lb://SHOP-SERVICE")
            )
            .route("auth-service",
                p -> p.path("/api/auth/**")
                    .filters(f -> f.filter(jwtUtil.apply(new JWTFilter.Config())))
                    .uri("lb://AUTH-SERVICE")
            )
            .build();
    }
}
