package com.nhnacademy.ssacthree_gateway.config;

import com.nhnacademy.ssacthree_gateway.filter.JWTFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import static org.mockito.Mockito.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RouteLocatorConfigTest {

    @Mock
    private JWTFilter jwtFilter;

    @Mock
    private PathConfig pathConfig;

    @Autowired
    private RouteLocatorConfig routeLocatorConfig;

    @Mock
    private RouteLocatorBuilder builder;

    @BeforeEach
    void setUp() {
        // PathConfig Mock 설정
        when(pathConfig.getAllowedPaths()).thenReturn("/api/allowed");
        when(pathConfig.getMemberPaths()).thenReturn("/api/member");
        when(pathConfig.getAdminPaths()).thenReturn("/api/admin");
    }

    @Test
    void testRouteLocatorConfig() {
        // JWTFilter Mock 설정
        when(jwtFilter.apply(null)).thenReturn((exchange, chain) -> chain.filter(exchange));

        // RouteLocator 설정
        RouteLocator routeLocator = routeLocatorConfig.myRoute(builder);

        // RouteLocator 검증
        assertThat(routeLocator).isNotNull();
    }
}
