package com.nhnacademy.ssacthree_gateway.config;

import com.nhnacademy.ssacthree_gateway.filter.JwtAuthorizationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RouteLocatorConfig {
    @Bean
    public RouteLocator myRoute(RouteLocatorBuilder builder, JwtAuthorizationFilter jwtAuthorizationFilter) {
        return builder.routes()
                //해당 경로로 요청이 오면, 각 서비스로 로드밸런싱(lb)
                .route("shop-service",
                        p->p.path("api/shop/**")
                                .filters(f -> f.filter(jwtAuthorizationFilter.apply(new JwtAuthorizationFilter.Config()))) //jwt 토큰 검증
                                .uri("lb://SHOP-SERVICE")
                )
                .route("auth-service",
                        p -> p.path("api/auth/**")
                                .uri("lb://AUTH-SERVICE")
                )
                .build();
    }
}
