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
        // JWTFilter 및 PathConfig Mock 설정
        when(pathConfig.getAllowedPaths()).thenReturn("/api/allowed");
        when(pathConfig.getMemberPaths()).thenReturn("/api/member");
        when(pathConfig.getAdminPaths()).thenReturn("/api/admin");
    }

    @Test
    void testRouteLocatorConfig() {
        // JWTFilter를 Mock하여, 필터의 동작을 확인
        when(jwtFilter.apply(any(JWTFilter.Config.class))).thenReturn((exchange, chain) -> chain.filter(exchange));

        // RouteLocator 설정
        RouteLocator routeLocator = routeLocatorConfig.myRoute(builder, jwtFilter);

        // assertNotNull으로 routeLocator가 잘 생성되었는지 확인
        assertThat(routeLocator).isNotNull();
    }
}



