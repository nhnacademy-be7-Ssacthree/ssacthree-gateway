package com.nhnacademy.ssacthree_gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "path")
public class PathConfig {

    private String adminPaths;
    private String allowedPaths;
    private String memberPaths;


}
