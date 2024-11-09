package com.nhnacademy.ssacthree_gateway.filter;

import com.nhnacademy.ssacthree_gateway.config.PathConfig;
import com.nhnacademy.ssacthree_gateway.util.JWTUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
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
        // 설정된 경로 리스트 모킹
        when(pathConfig.getAllowedPaths()).thenReturn("/public");
        when(pathConfig.getMemberPaths()).thenReturn("/api/member");
        when(pathConfig.getAdminPaths()).thenReturn("/api/admin");

        // 기본적인 chain 설정 모킹
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    void testFilter_AllowedPath() {
        // /public 경로로 요청을 보내도록 설정
        exchange = createExchange("/public");

        jwtFilter.apply(new JWTFilter.Config()).filter(exchange, chain);

        // 체인이 정상적으로 실행됐는지 확인
        verify(chain, times(1)).filter(exchange);
    }

    @Test
    void testFilter_NoAuthorizationHeader() {
        // Authorization 헤더가 없는 요청
        exchange = createExchange("/api/member");

        // 예외 발생 여부 확인
        assertThrows(ResponseStatusException.class, () ->
                jwtFilter.apply(new JWTFilter.Config()).filter(exchange, chain),
            "토큰을 찾을 수 없거나, 유효하지 않습니다.");
    }

    @Test
    void testFilter_TokenExpired() {
        // Authorization 헤더가 있는 요청 및 만료된 토큰 설정
        exchange = createExchangeWithAuth("/api/member", "Bearer expiredToken");
        when(jwtUtil.isExpired("expiredToken")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () ->
                jwtFilter.apply(new JWTFilter.Config()).filter(exchange, chain),
            "토큰이 만료되었습니다.");
    }

    @Test
    void testFilter_AdminAccessWithoutAdminRole() {
        // 관리자 권한이 필요한 경로로 요청을 보내지만, ROLE_USER 토큰 사용
        exchange = createExchangeWithAuth("/api/admin", "Bearer validToken");
        when(jwtUtil.isExpired("validToken")).thenReturn(false);
        when(jwtUtil.getRole("validToken")).thenReturn("ROLE_USER");

        assertThrows(ResponseStatusException.class, () ->
                jwtFilter.apply(new JWTFilter.Config()).filter(exchange, chain),
            "어드민 권한이 필요합니다.");
    }

    @Test
    void testFilter_MemberAccessWithoutMemberRole() {
        // 멤버 권한이 필요한 경로로 요청을 보내지만, ROLE_ADMIN 토큰 사용
        exchange = createExchangeWithAuth("/api/member", "Bearer validToken");
        when(jwtUtil.isExpired("validToken")).thenReturn(false);
        when(jwtUtil.getRole("validToken")).thenReturn("ROLE_ADMIN");

        assertThrows(ResponseStatusException.class, () ->
                jwtFilter.apply(new JWTFilter.Config()).filter(exchange, chain),
            "멤버 권한이 필요합니다.");
    }

    private ServerWebExchange createExchange(String path) {
        ServerHttpRequest request = MockServerHttpRequest.get(path).build();
        return mock(ServerWebExchange.class, invocation -> {
            if (invocation.getMethod().getName().equals("getRequest")) {
                return request;
            } else if (invocation.getMethod().getName().equals("getResponse")) {
                return new MockServerHttpResponse();
            }
            return invocation.callRealMethod();
        });
    }

    private ServerWebExchange createExchangeWithAuth(String path, String authHeader) {
        ServerHttpRequest request = MockServerHttpRequest.get(path)
            .header(HttpHeaders.AUTHORIZATION, authHeader)
            .build();
        return mock(ServerWebExchange.class, invocation -> {
            if (invocation.getMethod().getName().equals("getRequest")) {
                return request;
            } else if (invocation.getMethod().getName().equals("getResponse")) {
                return new MockServerHttpResponse();
            }
            return invocation.callRealMethod();
        });
    }
}
