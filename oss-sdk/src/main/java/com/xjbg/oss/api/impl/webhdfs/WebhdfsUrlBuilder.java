package com.xjbg.oss.api.impl.webhdfs;

import com.google.common.base.Joiner;
import com.xjbg.oss.api.ApiConstant;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author kesc
 * @date 2021-09-09 16:17
 */
@ToString
public class WebhdfsUrlBuilder {
    private static final Logger log = LoggerFactory.getLogger(WebhdfsUrlBuilder.class);
    private String endpoint;
    private String defaultBucket;
    private String bucket;
    private String object;
    private Map<String, String> params = new HashMap<>();

    private WebhdfsUrlBuilder() {
    }

    public static WebhdfsUrlBuilder newBuilder() {
        return new WebhdfsUrlBuilder();
    }

    public WebhdfsUrlBuilder endpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public WebhdfsUrlBuilder defaultBucket(String defaultBucket) {
        this.defaultBucket = defaultBucket;
        return this;
    }

    public WebhdfsUrlBuilder bucket(String bucket) {
        this.bucket = bucket;
        return this;
    }

    public WebhdfsUrlBuilder object(String object) {
        this.object = object;
        return this;
    }

    public WebhdfsUrlBuilder params(Map<String, String> params) {
        this.params.putAll(params);
        return this;
    }

    private String buildPath() {
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotBlank(defaultBucket)) {
            parts.add(defaultBucket);
        }
        if (StringUtils.isNotBlank(bucket)) {
            parts.add(bucket);
        }
        if (StringUtils.isNotBlank(object)) {
            parts.add(object);
        }
        return parts.stream().map(x -> Arrays.asList(x.replace(ApiConstant.BACK_SLASH, ApiConstant.SLASH)
                .split(ApiConstant.SLASH))).flatMap(Collection::stream)
                .filter(StringUtils::isNotBlank).map(x -> {
                    try {
                        return URLEncoder.encode(x, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        return x;
                    }
                }).collect(Collectors.joining(ApiConstant.SLASH));
    }

    public String build() {
        //http://<host>:<port>/webhdfs/v1[/<path>][?<params>]
        log.info(toString());
        StringBuilder sb = new StringBuilder(endpoint);
        if (!endpoint.endsWith(ApiConstant.SLASH)) {
            sb.append(ApiConstant.SLASH);
        }
        sb.append(buildPath());
        if (params != null && params.size() > 0) {
            String queryString = Joiner.on("&").join(params.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.toList()));
            sb.append("?").append(queryString);
        }
        return sb.toString();
    }
}
