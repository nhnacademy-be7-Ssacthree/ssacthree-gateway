package com.nhnacademy.ssacthree_gateway.filter;

import com.nhnacademy.ssacthree_gateway.util.JWTUtil;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;


@Component
@Slf4j
public class JWTFilter extends AbstractGatewayFilterFactory<JWTFilter.Config> {

    private final JWTUtil jwtUtil;

    public JWTFilter(JWTUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }
    // 쓸 변수들 넣어주는거임.
    // TODO : 얘네 자꾸 널 떠 서 미치겠음. 이거 만 해결하면 됨 .
    @Getter
    @Setter
    public static class Config {
        private List<String> allowedPaths;
        private List<String> adminPaths;
        private List<String> memberPaths;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            String path = request.getURI().getPath();

            // 경로가 설정된 allowedPaths에 포함되지 않으면 필터를 적용하지 않음
            if (config.getAllowedPaths() != null && !config.getAllowedPaths().contains(path)) {
                return chain.filter(exchange); // 필터를 건너뜀
            }


            if(!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"토큰을 찾을 수 없거나, 유효하지않습니다.");
            }

            String accessToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);



            log.debug("accessToken: {}", accessToken);

            if(jwtUtil.isExpired(accessToken)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"토큰이 만료되었습니다.");
            }

            String memberLoginId = jwtUtil.getMemberLoginId(accessToken);
            String role = jwtUtil.getRole(accessToken);

            if (config.getAdminPaths() != null && config.getAdminPaths().contains(path) && !"ROLE_ADMIN".equals(role)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "어드민 권한이 필요합니다.");
            }

            if (config.getMemberPaths() != null && config.getMemberPaths().contains(path) && !"ROLE_USER".equals(role)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "멤버 권한이 필요합니다.");
            }

            ServerWebExchange modifiedExchange = exchange.mutate().request(builder -> builder.header("X-USER-ID",memberLoginId))
                .build();

            return chain.filter(modifiedExchange);
        };
    }
}
