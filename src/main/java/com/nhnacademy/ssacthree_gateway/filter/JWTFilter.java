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
public class JWTFilter extends AbstractGatewayFilterFactory<JWTFilter.Config> {

    private final JWTUtil jwtUtil;
    private final PathConfig pathConfig;

    public JWTFilter(JWTUtil jwtUtil, PathConfig pathConfig) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
        this.pathConfig = pathConfig;
    }

    public static class Config {

    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            List<String> allowedPaths = Arrays.stream(pathConfig.getAllowedPaths().split(","))
                .toList();
            List<String> memberPaths = Arrays.stream(pathConfig.getMemberPaths().split(","))
                .toList();
            List<String> adminPaths = Arrays.stream(pathConfig.getAdminPaths().split(",")).toList();

            //회원 가입의 경우 필터 태우면 안됨 ㅠ 일단은 더럽지만 이렇게 가자고.
            if (!path.equals("api/shop/members/likes") && path.equals("/api/shop/members") && request.getMethod().toString().equals("POST")) {
                return chain.filter(exchange);
            }

            if (path.equals("/api/shop/search/books") && request.getMethod().toString().equals("GET")) {
                return chain.filter(exchange);
            }

            if (path.equals("/api/shop/carts/cart")
                && request.getMethod().toString().equals("POST")
                && request.getQueryParams().containsKey("customerId")) {

                return chain.filter(exchange); // 필터 통과
            }
            // 경로가 설정된 allowedPaths에 포함되어 있으면 필터를 적용하지 않음
            if (allowedPaths != null && allowedPaths.stream().anyMatch(path::startsWith)) {
                return chain.filter(exchange); // 필터를 건너뜀
            }

            if (!request.getCookies().containsKey("access-token")) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
            }

            String accessToken = request.getCookies().get("access-token").get(0).toString()
                .split("=")[1];

            // 토큰 위조시... 이렇게 처리하면 될 것 같 긴 한 데 이 게 맞 나 .. ?
//            try {
//                if (jwtUtil.isExpired(accessToken)) {
//                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.");
//                }
//            } catch (Exception e) {
//                return exchange.getResponse().setComplete()
//                    .then(Mono.fromRunnable(() -> {
//                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//                        exchange.getResponse().getHeaders().set("Clear-Site-Data", "\"cookies\"");
//                    }));
//            }

            if (jwtUtil.isExpired(accessToken)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.");
            }

            String memberLoginId = jwtUtil.getMemberLoginId(accessToken);
            String role = jwtUtil.getRole(accessToken);

            if (adminPaths.stream().anyMatch(path::startsWith) && !"ROLE_ADMIN".equals(role)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "어드민 권한이 필요합니다.");
            }

            if (memberPaths.stream().anyMatch(path::startsWith) && !"ROLE_USER".equals(role)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "멤버 권한이 필요합니다.");
            }

            exchange.mutate().request(builder -> builder.header("X-USER-ID", memberLoginId))
                .build();
            return chain.filter(exchange);
        };
    }
}
