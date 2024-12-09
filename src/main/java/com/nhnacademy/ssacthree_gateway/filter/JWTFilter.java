package com.nhnacademy.ssacthree_gateway.filter;

import com.nhnacademy.ssacthree_gateway.config.PathConfig;
import com.nhnacademy.ssacthree_gateway.util.JWTUtil;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpMethod;
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
            HttpMethod method = request.getMethod();

            if (isBypassPath(path, method, request)) {
                return chain.filter(exchange);
            }

            if (isAllowedPath(path)) {
                return chain.filter(exchange);
            }

            validateAccessToken(request);

            String accessToken = extractAccessToken(request);
            validateTokenExpiration(accessToken);

            String memberLoginId = jwtUtil.getMemberLoginId(accessToken);
            String role = jwtUtil.getRole(accessToken);

            validateRoleAccess(path, role);

            // Add user ID to the request header
            exchange.mutate().request(builder -> builder.header("X-USER-ID", memberLoginId)).build();
            return chain.filter(exchange);
        };
    }

    private boolean isBypassPath(String path, HttpMethod method, ServerHttpRequest request) {
        return (path.equals("/api/shop/members") && method == HttpMethod.POST) ||
            (path.equals("/api/shop/search/books") && method == HttpMethod.GET) ||
            (path.equals("/api/shop/carts/cart") && method == HttpMethod.POST &&
                request.getQueryParams().containsKey("customerId"));
    }

    private boolean isAllowedPath(String path) {
        List<String> allowedPaths = Arrays.stream(pathConfig.getAllowedPaths().split(",")).toList();
        return allowedPaths.stream().anyMatch(path::startsWith);
    }

    private void validateAccessToken(ServerHttpRequest request) {
        if (!request.getCookies().containsKey("access-token")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }

    private String extractAccessToken(ServerHttpRequest request) {
        return request.getCookies().get("access-token").getFirst().toString().split("=")[1];
    }

    private void validateTokenExpiration(String accessToken) {
        if (Boolean.TRUE.equals(jwtUtil.isExpired(accessToken))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.");
        }
    }

    private void validateRoleAccess(String path, String role) {
        List<String> adminPaths = Arrays.stream(pathConfig.getAdminPaths().split(",")).toList();
        List<String> memberPaths = Arrays.stream(pathConfig.getMemberPaths().split(",")).toList();

        if (adminPaths.stream().anyMatch(path::startsWith) && !"ROLE_ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "어드민 권한이 필요합니다.");
        }

        if (memberPaths.stream().anyMatch(path::startsWith) && !"ROLE_USER".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "멤버 권한이 필요합니다.");
        }
    }
}