package com.nhnacademy.ssacthree_gateway.filter;

import com.nhnacademy.ssacthree_gateway.dto.ErrorResponse; // ErrorResponse DTO 추가
import com.nhnacademy.ssacthree_gateway.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class JwtAuthorizationFilter extends AbstractGatewayFilterFactory<JwtAuthorizationFilter.Config> {
    private final JwtUtils jwtUtils;
    private static final String TOKEN_PREFIX = "Bearer "; // Authorization 헤더에 사용되는 토큰 prefix

    public static class Config {
        // 필터 설정이 필요한 경우 여기에 추가
    }

    public JwtAuthorizationFilter(JwtUtils jwtUtils) {
        super(Config.class);
        this.jwtUtils = jwtUtils;
    }

    /**
     * GateWayFilter, 요청의 Authorization 헤더에서 JWT 토큰 추출하여 유효성 검사.
     * accessToken이 유효하지 않거나, 토큰이 없는 경우 에러 메시지를 반환.
     */
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            // Authorization 헤더가 없거나, 잘못된 형식이면 에러 메시지 반환
            if (authHeader == null || !authHeader.startsWith(TOKEN_PREFIX)) {
                log.error("Authorization 헤더가 없거나 잘못된 형식입니다.");
                return returnErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "Authorization header가 없거나 잘못된 형식입니다.");
            }

            // 실제 토큰값 추출
            String token = authHeader.substring(TOKEN_PREFIX.length());

            // JWT 토큰 유효성 검사, 유효하지 않으면 에러 메시지 반환
            if (!jwtUtils.isTokenValid(token)) {
                log.error("유효하지 않은 JWT 토큰입니다.");
                return returnErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "유효하지 않은 JWT토큰입니다.");
            }

            // 유효한 토큰이면, 요청을 다음 필터로 전달
            return chain.filter(exchange);
        };
    }

    // 에러 응답 반환 메서드
    private Mono<Void> returnErrorResponse(ServerWebExchange exchange, HttpStatus status, String errorMessage) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse errorResponse = new ErrorResponse(status.value(), errorMessage);

        // JSON으로 에러 메시지 반환
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory().wrap(serializeErrorResponse(errorResponse).getBytes())));
    }

    // ErrorResponse를 JSON 문자열로 변환하는 메서드
    private String serializeErrorResponse(ErrorResponse errorResponse) {
        return "{ \"errorCode\": " + errorResponse.getErrorCode() +
                ", \"errorMessage\": \"" + errorResponse.getErrorMessage() + "\" }";
    }
}
