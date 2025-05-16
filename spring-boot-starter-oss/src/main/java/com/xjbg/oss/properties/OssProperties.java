package com.xjbg.oss.properties;

import com.xjbg.oss.api.ApiConstant;
import com.xjbg.oss.enums.ApiType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.annotation.Order;

import java.util.HashMap;
import java.util.Map;

/**
 * @author kesc
 * @date 2020-08-06 11:19
 */
@Getter
@Setter
@Order
@RefreshScope
@ConfigurationProperties(prefix = OssProperties.PREFIX)
public class OssProperties {
    public static final String PREFIX = "oss";
    /**
     * Default api type {@link ApiType}
     */
    private String defaultApiType = ApiType.FILESYSTEM.name();

    /**
     * enable oss template
     */
    private boolean enable = true;

    /**
     * base directory for filesystem
     */
    private String baseDir = "";

    /**
     * http connection config
     */
    private ClientConfig clientConfig = new ClientConfig();

    /**
     * s3 properties
     */
    private S3Properties s3 = new S3Properties();

    /**
     * ceph properties
     */
    private CephProperties ceph = new CephProperties();

    /**
     * minio properties
     */
    private MinioProperties minio = new MinioProperties();

    /**
     * fusion storage properties
     */
    private FusionStorageProperties fusion = new FusionStorageProperties();

    /**
     * webhdfs properties
     */
    private WebHdfsProperties webhdfs = new WebHdfsProperties();

    @Data
    public static class S3Properties {
        /**
         * default bucket to use when args don't specify bucket
         */
        private String defaultBucket;
        /**
         * Endpoint is an URL, domain name, IPv4 or IPv6 address of S3 service.
         */
        private String url;
        /**
         * Access key (aka user ID) of your account
         */
        private String accessKey;
        /**
         * Secret key (aka password) of your account
         */
        private String secretKey;

        /**
         * enable minio
         */
        private boolean enable = false;

        /**
         * auto create bucket
         */
        private boolean autoCreateBucket = false;
    }

    @EqualsAndHashCode(callSuper = true)
    public static class MinioProperties extends S3Properties {
    }

    @EqualsAndHashCode(callSuper = true)
    public static class FusionStorageProperties extends S3Properties {
    }

    @EqualsAndHashCode(callSuper = true)
    public static class CephProperties extends S3Properties {
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class WebHdfsProperties extends S3Properties {
        /**
         * auth type: kerberos,pseudo
         */
        private String authType = ApiConstant.KERBEROS;
        private String krb5;
        private String servicePrincipal;
        private boolean debug = false;
        /**
         * key is ip:port, value is spn, for namenode and secondary namenode
         */
        private Map<String, String> standby = new HashMap<>();
    }

    @Getter
    @Setter
    public static class ClientConfig {
        /**
         * {@link com.amazonaws.http.SystemPropertyTlsKeyManagersProvider}
         */
        private boolean useHttps = false;
        private Integer connectionTimeout = 10_000;
        private Long connectionMaxIdleMillis = 60_000L;
        private Long connectionTTL = 120_000L;
        private Integer maxConnections = 30;
        private Integer requestTimeout = 10_000;
        private Integer socketTimeout = 10_000;
        private boolean useGzip = false;
        private boolean tcpKeepAlive = false;
    }

    public ApiType getDefaultApiType() {
        return ApiType.valueOf(defaultApiType.toUpperCase());
    }
}
