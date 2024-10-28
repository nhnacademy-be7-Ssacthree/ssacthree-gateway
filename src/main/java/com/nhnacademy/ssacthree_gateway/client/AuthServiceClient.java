package com.nhnacademy.ssacthree_gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @PostMapping("/api/auth/refresh")
    @ResponseBody
    String refreshAccessToken(@RequestHeader(HttpHeaders.AUTHORIZATION) String refreshToken);
}
