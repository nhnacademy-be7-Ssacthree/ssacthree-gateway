package com.nhnacademy.ssacthree_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Order(1) // 우선 순위를 1로 설정
@Slf4j
@Component
public class GlobalLogFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logRequest(exchange);
        return chain.filter(exchange).then(Mono.fromRunnable(() -> logResponse(exchange)));
    }

    private void logRequest(ServerWebExchange exchange) {
        log.info("Request Path: {}", exchange.getRequest().getURI());
        log.info("Request Method: {}", exchange.getRequest().getMethod());
        log.info("Request Headers: {}", exchange.getRequest().getHeaders());
    }

    private void logResponse(ServerWebExchange exchange) {
        log.info("Response Status Code: {}", exchange.getResponse().getStatusCode());
    }
}
