package com.xjbg.oss.application.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kesc
 * @date 2020-04-03 16:15
 */
@Getter
@Setter
@RefreshScope
@ConfigurationProperties(prefix = CorsProperties.CORS_PREFIX)
public class CorsProperties {
    public static final String CORS_PREFIX = "cors";
    private boolean enable = true;
    private String pathPattern = "/**";
    private List<String> allowedOrigins = new ArrayList<>();
    private boolean allowCredentials = true;
    private Long maxAge = 1800L;
    private List<String> allowedHeaders = new ArrayList<>();
    private List<String> allowedMethods = new ArrayList<>();
    private List<String> exposedHeaders = new ArrayList<>();
}
