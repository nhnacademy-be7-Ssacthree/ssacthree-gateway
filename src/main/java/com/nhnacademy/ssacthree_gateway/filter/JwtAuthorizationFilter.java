package com.nhnacademy.ssacthree_gateway.filter;

import com.nhnacademy.ssacthree_gateway.client.AuthServiceClient;
import com.nhnacademy.ssacthree_gateway.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class JwtAuthorizationFilter extends AbstractGatewayFilterFactory<JwtAuthorizationFilter.Config> {
    private final JwtUtils jwtUtils;
    private final AuthServiceClient authServiceClient; // AuthServiceClient 주입
    private static final String TOKEN_PREFIX = "Bearer "; // Authorization 헤더에 사용되는 토큰 prefix

    public static class Config {
        // 필터 설정이 필요한 경우 여기에 추가
    }

    public JwtAuthorizationFilter(JwtUtils jwtUtils, AuthServiceClient authServiceClient) {
        super(Config.class);
        this.jwtUtils = jwtUtils;
        this.authServiceClient = authServiceClient;
    }

    /**
     * GateWayFilter, 요청의 Authorization 헤더에서 JWT 토큰 추출하여 유효성 검사.
     * accessToken이 유효하지 않거나, 토큰이 없는 경우, auth-service로 요청을 위임하여 재발급.
     */
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            // Authorization 헤더가 없거나, 잘못된 형식이면 auth-service로 토큰 재발급 요청
            if (authHeader == null || !authHeader.startsWith(TOKEN_PREFIX)) {
                log.error("Authorization 헤더가 없거나 잘못된 형식입니다.");
                return Mono.empty();
            }

            // 실제 토큰값 추출
            String token = authHeader.substring(TOKEN_PREFIX.length());

            // JWT 토큰 유효성 검사, 유효하지 않으면 auth-service로 토큰 재발급 요청
            if (!jwtUtils.isTokenValid(token)) {
                log.error("유효하지 않은 JWT 토큰입니다.");
                return requestNewAccessToken(exchange, token);
            }

            // 유효한 토큰이면, 요청을 다음 필터로 전달
            return chain.filter(exchange);
        };
    }

    // auth-service로 재발급 요청을 위한 메서드
    private Mono<Void> requestNewAccessToken(ServerWebExchange exchange, String refreshToken) {
        String refreshHeader = "Bearer " + refreshToken; // Refresh 토큰 형식에 맞게 설정
        return Mono.fromCallable(() -> authServiceClient.refreshAccessToken(refreshHeader))
                .flatMap(newAccessToken -> {
                    // 새로운 accessToken을 클라이언트의 응답 헤더에 추가
                    exchange.getResponse().getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken);
                    return exchange.getResponse().setComplete(); // 응답 완료
                })
                .onErrorResume(throwable -> {
                    log.error("Refresh token 요청 중 오류 발생: {}", throwable.getMessage());
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete(); // 에러 발생 시 응답 완료
                });
    }
}
