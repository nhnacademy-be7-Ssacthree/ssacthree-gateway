package com.nhnacademy.ssacthree_gateway.filter;

import com.nhnacademy.ssacthree_gateway.config.PathConfig;
import com.nhnacademy.ssacthree_gateway.util.JWTUtil;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;



@Component
@Slf4j
public class JWTFilter extends AbstractGatewayFilterFactory<JWTFilter.Config> {

    private final JWTUtil jwtUtil;
    private final PathConfig pathConfig;
    public JWTFilter(JWTUtil jwtUtil, PathConfig pathConfig) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
        this.pathConfig = pathConfig;
    }

    // 쓸 변수들 넣어주는거임.
    // TODO : 경로 확실히 구분 해서 검증 확실히 하기
    public static class Config {

    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            List<String> allowedPaths = Arrays.stream(pathConfig.getAllowedPaths().split(",")).toList();
            List<String> memberPaths = Arrays.stream(pathConfig.getMemberPaths().split(",")).toList();
            List<String> adminPaths = Arrays.stream(pathConfig.getAdminPaths().split(",")).toList();

            log.debug("admin path:{}",pathConfig.getAdminPaths());
            // 경로가 설정된 allowedPaths에 포함되어 있으면 필터를 적용하지 않음
            if (allowedPaths != null && allowedPaths.stream().anyMatch(path::startsWith)) {
                return chain.filter(exchange); // 필터를 건너뜀
            }


            if(!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"토큰을 찾을 수 없거나, 유효하지않습니다.");
            }

            String accessToken = Objects.requireNonNull(
                request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).substring("Bearer ".length());





            if(jwtUtil.isExpired(accessToken)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"토큰이 만료되었습니다.");
            }

            String memberLoginId = jwtUtil.getMemberLoginId(accessToken);
            String role = jwtUtil.getRole(accessToken);

            if (adminPaths.stream().anyMatch(path::startsWith) && !"ROLE_ADMIN".equals(role)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "어드민 권한이 필요합니다.");
            }

            if (memberPaths.stream().anyMatch(path::startsWith) && !"ROLE_USER".equals(role)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "멤버 권한이 필요합니다.");
            }

//            ServerWebExchange modifiedExchange = exchange.mutate().request(builder -> builder.header("X-USER-ID",memberLoginId))
//                .build();
            exchange.mutate().request(builder -> builder.header("X-USER-ID", memberLoginId))
                .build();
            return chain.filter(exchange);
        };
    }
}
