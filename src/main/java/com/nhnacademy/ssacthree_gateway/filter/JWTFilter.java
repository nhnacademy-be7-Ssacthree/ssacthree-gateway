package com.nhnacademy.ssacthree_gateway.filter;

import com.nhnacademy.ssacthree_gateway.config.PathConfig;
import com.nhnacademy.ssacthree_gateway.util.JWTUtil;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@Slf4j
public class JWTFilter extends AbstractGatewayFilterFactory<Object> {

    private final JWTUtil jwtUtil;
    private final PathConfig pathConfig;

    public JWTFilter(JWTUtil jwtUtil, PathConfig pathConfig) {
        super(Object.class);
        this.jwtUtil = jwtUtil;
        this.pathConfig = pathConfig;
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            List<String> allowedPaths = Arrays.stream(pathConfig.getAllowedPaths().split(",")).toList();
            List<String> memberPaths = Arrays.stream(pathConfig.getMemberPaths().split(",")).toList();
            List<String> adminPaths = Arrays.stream(pathConfig.getAdminPaths().split(",")).toList();

            // 특정 경로에 대한 필터 우회 처리
            if (path.equals("/api/shop/members") && request.getMethod().toString().equals("POST")) {
                return chain.filter(exchange);
            }

            if (path.equals("/api/shop/search/books") && request.getMethod().toString().equals("GET")) {
                return chain.filter(exchange);
            }

            if (path.equals("/api/shop/carts/cart")
                && request.getMethod().toString().equals("POST")
                && request.getQueryParams().containsKey("customerId")) {
                return chain.filter(exchange);
            }

            // 허용된 경로라면 필터를 건너뜀
            if (allowedPaths.stream().anyMatch(path::startsWith)) {
                return chain.filter(exchange);
            }

            // 토큰 유효성 검사
            if (!request.getCookies().containsKey("access-token")) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
            }

            String accessToken = request.getCookies().get("access-token").getFirst().toString().split("=")[1];

            if (jwtUtil.isExpired(accessToken)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.");
            }

            String memberLoginId = jwtUtil.getMemberLoginId(accessToken);
            String role = jwtUtil.getRole(accessToken);

            // 역할 권한 확인
            if (adminPaths.stream().anyMatch(path::startsWith) && !"ROLE_ADMIN".equals(role)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "어드민 권한이 필요합니다.");
            }

            if (memberPaths.stream().anyMatch(path::startsWith) && !"ROLE_USER".equals(role)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "멤버 권한이 필요합니다.");
            }

            // 헤더 추가
            exchange.mutate().request(builder -> builder.header("X-USER-ID", memberLoginId)).build();
            return chain.filter(exchange);
        };
    }
}