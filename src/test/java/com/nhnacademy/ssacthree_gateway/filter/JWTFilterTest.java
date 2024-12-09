package com.nhnacademy.ssacthree_gateway.filter;

import com.nhnacademy.ssacthree_gateway.config.PathConfig;
import com.nhnacademy.ssacthree_gateway.util.JWTUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class JWTFilterTest {

    @Mock
    private JWTUtil jwtUtil;

    @Mock
    private PathConfig pathConfig;

    @Mock
    private GatewayFilterChain chain;

    @InjectMocks
    private JWTFilter jwtFilter;

    private ServerWebExchange exchange;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // PathConfig Mock 설정
        when(pathConfig.getAllowedPaths()).thenReturn("/public");
        when(pathConfig.getMemberPaths()).thenReturn("/api/member");
        when(pathConfig.getAdminPaths()).thenReturn("/api/admin");

        // 기본적인 chain 설정 Mock
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    void testFilter_AllowedPath() {
        // /public 경로로 요청 생성
        exchange = createExchange("/public", null);

        // null 대신 new Object() 전달
        jwtFilter.apply(new Object()).filter(exchange, chain);

        // 체인이 실행되었는지 확인
        verify(chain, times(1)).filter(exchange);
    }

    @Test
    void testFilter_NoAccessToken() {
        // 토큰 없는 요청 생성
        exchange = createExchange("/api/member", null);

        // null 대신 new Object() 전달
        assertThrows(ResponseStatusException.class, () ->
            jwtFilter.apply(new Object()).filter(exchange, chain));
    }

    @Test
    void testFilter_ExpiredToken() {
        // 만료된 토큰 요청 생성
        exchange = createExchange("/api/member", "Bearer expiredToken");
        when(jwtUtil.isExpired("expiredToken")).thenReturn(true);

        // null 대신 new Object() 전달
        assertThrows(ResponseStatusException.class, () ->
            jwtFilter.apply(new Object()).filter(exchange, chain));
    }

    @Test
    void testFilter_InvalidRoleForAdminPath() {
        // ROLE_USER로 관리자 경로 요청
        exchange = createExchange("/api/admin", "Bearer validToken");
        when(jwtUtil.isExpired("validToken")).thenReturn(false);
        when(jwtUtil.getRole("validToken")).thenReturn("ROLE_USER");

        // null 대신 new Object() 전달
        assertThrows(ResponseStatusException.class, () ->
            jwtFilter.apply(new Object()).filter(exchange, chain));
    }

    @Test
    void testFilter_ValidTokenWithValidRole() {
        // 요청 생성: 멤버 경로, GET, 유효한 토큰
        exchange = createExchange("/api/member", "GET", null, "validToken", null, null);

        // Mock 설정
        when(jwtUtil.isExpired("validToken")).thenReturn(false);
        when(jwtUtil.getRole("validToken")).thenReturn("ROLE_USER");
        when(jwtUtil.getMemberLoginId("validToken")).thenReturn("user123");

        // 필터 실행
        jwtFilter.apply(new Object()).filter(exchange, chain);

        // 체인이 실행되었는지 확인
        verify(chain, times(1)).filter(exchange);
    }

    @Test
    void testFilter_BypassForMembersPath() {
        // 요청 생성: POST 요청, /api/shop/members
        exchange = createExchange("/api/shop/members", "POST", null, null, null, null);

        // 필터 실행
        jwtFilter.apply(new Object()).filter(exchange, chain);

        // 체인이 실행되었는지 확인
        verify(chain, times(1)).filter(exchange);
    }

    @Test
    void testFilter_BypassForCartsPathWithCustomerId() {
        // 요청 생성: POST 요청, /api/shop/carts/cart, 쿼리 파라미터 포함
        exchange = createExchange("/api/shop/carts/cart", "POST", null, null, "customerId", "12345");

        // 필터 실행
        jwtFilter.apply(new Object()).filter(exchange, chain);

        // 체인이 실행되었는지 확인
        verify(chain, times(1)).filter(exchange);
    }


    private ServerWebExchange createExchange(String path, String authHeader) {
        MockServerHttpRequest.BaseBuilder<?> requestBuilder = MockServerHttpRequest.get(path);
        if (authHeader != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, authHeader); // Authorization 헤더 추가
        }
        return mock(ServerWebExchange.class, invocation -> {
            if (invocation.getMethod().getName().equals("getRequest")) {
                return requestBuilder.build();
            } else if (invocation.getMethod().getName().equals("getResponse")) {
                return new MockServerHttpResponse();
            }
            return invocation.callRealMethod();
        });
    }

    private ServerWebExchange createExchange(String path, String method, String authHeader, String cookie, String queryParam, String queryValue) {
        MockServerHttpRequest.BaseBuilder<?> requestBuilder = MockServerHttpRequest.method(method, path);

        // Authorization 헤더 추가
        if (authHeader != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, authHeader);
        }

        // Cookie 추가
        if (cookie != null) {
            requestBuilder.cookie(new HttpCookie("access-token", cookie));
        }

        // 쿼리 파라미터 추가
        if (queryParam != null && queryValue != null) {
            requestBuilder.queryParam(queryParam, queryValue);
        }

        return mock(ServerWebExchange.class, invocation -> {
            if (invocation.getMethod().getName().equals("getRequest")) {
                return requestBuilder.build();
            } else if (invocation.getMethod().getName().equals("getResponse")) {
                return new MockServerHttpResponse();
            }
            return invocation.callRealMethod();
        });
    }
}