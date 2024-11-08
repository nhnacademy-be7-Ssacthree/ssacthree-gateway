package com.nhnacademy.ssacthree_gateway.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
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
