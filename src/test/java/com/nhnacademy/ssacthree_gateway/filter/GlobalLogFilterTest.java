package com.nhnacademy.ssacthree_gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;

class GlobalLogFilterTest {

    @Mock
    private GatewayFilterChain chain;

    @InjectMocks
    private GlobalLogFilter globalLogFilter;

    private ServerWebExchange exchange;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock HTTP 요청을 생성
        exchange = MockServerWebExchange.from(
            MockServerHttpRequest.method(HttpMethod.GET, "/test-path")
                .header("Test-Header", "TestValue")
                .build()
        );

        // Mock Chain 설정
        when(chain.filter(exchange)).thenReturn(Mono.empty());
    }

    @Test
    void testFilter_LogsRequestAndResponse() {
        // 필터 메서드 호출
        globalLogFilter.filter(exchange, chain).block();

        // 요청이 chain.filter로 전달됐는지 확인
        verify(chain, times(1)).filter(exchange);

        // 로그 출력을 검증할 수 있는 log capture 라이브러리나 툴이 있으면 추가적인 검증을 할 수 있음
    }
}

