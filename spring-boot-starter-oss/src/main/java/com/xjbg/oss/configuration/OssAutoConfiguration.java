package com.xjbg.oss.configuration;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.http.SystemPropertyTlsKeyManagersProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.util.AwsHostNameUtils;
import com.xjbg.oss.api.OssTemplate;
import com.xjbg.oss.api.impl.*;
import com.xjbg.oss.enums.ApiType;
import com.xjbg.oss.properties.OssProperties;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * @author kesc
 * @date 2020-08-06 11:21
 */
@Configuration
@AutoConfigureOrder(value = Integer.MAX_VALUE)
@EnableConfigurationProperties(value = {OssProperties.class})
public class OssAutoConfiguration {

    private AmazonS3 amazonS3Client(String ak, String sk, String endpoint, OssProperties.ClientConfig ossClientConfig) {
        BasicAWSCredentials credentials = new BasicAWSCredentials(ak, sk);
        ClientConfiguration clientConfig = new ClientConfiguration();
        if (ossClientConfig.isUseHttps()) {
            clientConfig.setProtocol(Protocol.HTTPS);
            clientConfig.setTlsKeyManagersProvider(new SystemPropertyTlsKeyManagersProvider());
        } else {
            clientConfig.setProtocol(Protocol.HTTP);
        }
        if (ossClientConfig.getConnectionTimeout() != null) {
            clientConfig.setConnectionTimeout(ossClientConfig.getConnectionTimeout());
        }
        if (ossClientConfig.getConnectionMaxIdleMillis() != null) {
            clientConfig.setConnectionMaxIdleMillis(ossClientConfig.getConnectionMaxIdleMillis());
        }
        if (ossClientConfig.getConnectionTTL() != null) {
            clientConfig.setConnectionTTL(ossClientConfig.getConnectionTTL());
        }
        if (ossClientConfig.getMaxConnections() != null) {
            clientConfig.setMaxConnections(ossClientConfig.getMaxConnections());
        }
        if (ossClientConfig.getRequestTimeout() != null) {
            clientConfig.setRequestTimeout(ossClientConfig.getRequestTimeout());
        }
        if (ossClientConfig.getSocketTimeout() != null) {
            clientConfig.setSocketTimeout(ossClientConfig.getSocketTimeout());
        }
        clientConfig.setUseGzip(ossClientConfig.isUseGzip());
        clientConfig.setUseTcpKeepAlive(ossClientConfig.isTcpKeepAlive());
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(endpoint, AwsHostNameUtils.parseRegion(endpoint, AmazonS3Client.S3_SERVICE_NAME));
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withClientConfiguration(clientConfig)
                .withPathStyleAccessEnabled(true)
                .withEndpointConfiguration(endpointConfiguration)
                .build();
    }

    @Bean
    @RefreshScope
    @ConditionalOnMissingBean(OssTemplate.class)
    @ConditionalOnProperty(name = "oss.enable", matchIfMissing = true)
    public OssTemplate ossTemplate(OssProperties properties) {
        OssTemplate ossTemplate = new OssTemplate();
        OssProperties.ClientConfig clientConfig = properties.getClientConfig();
        //register s3
        OssProperties.S3Properties s3Properties = properties.getS3();
        if (s3Properties.isEnable() && StringUtils.hasText(s3Properties.getUrl())) {
            ossTemplate.register(ApiType.S3, new S3ApiImpl(amazonS3Client(s3Properties.getAccessKey(), s3Properties.getSecretKey(), s3Properties.getUrl(), clientConfig), s3Properties.getDefaultBucket(), s3Properties.isAutoCreateBucket()));
        }
        //register ceph
        OssProperties.CephProperties cephProperties = properties.getCeph();
        if (cephProperties.isEnable() && StringUtils.hasText(cephProperties.getUrl())) {
            ossTemplate.register(ApiType.CEPH, new CephApiImpl(amazonS3Client(cephProperties.getAccessKey(), cephProperties.getSecretKey(), cephProperties.getUrl(), clientConfig), cephProperties.getDefaultBucket(), cephProperties.isAutoCreateBucket()));
        }
        //register minio
        OssProperties.MinioProperties minioProperties = properties.getMinio();
        if (minioProperties.isEnable() && StringUtils.hasText(minioProperties.getUrl())) {
            ossTemplate.register(ApiType.MINIO, new MinioApiImpl(amazonS3Client(minioProperties.getAccessKey(), minioProperties.getSecretKey(), minioProperties.getUrl(), clientConfig), minioProperties.getDefaultBucket(), minioProperties.isAutoCreateBucket()));
        }
        //register fusion storage
        OssProperties.FusionStorageProperties fusionStorageProperties = properties.getFusion();
        if (fusionStorageProperties.isEnable() && StringUtils.hasText(fusionStorageProperties.getUrl())) {
            ossTemplate.register(ApiType.FUSION, new FusionStorageApiImpl(amazonS3Client(fusionStorageProperties.getAccessKey(), fusionStorageProperties.getSecretKey(), fusionStorageProperties.getUrl(), clientConfig), fusionStorageProperties.getDefaultBucket(), fusionStorageProperties.isAutoCreateBucket()));
        }
        if (properties.getDefaultApiType() != null) {
            ossTemplate.setDefaultApiType(properties.getDefaultApiType());
        }
        //register filesystem
        ossTemplate.register(ApiType.FILESYSTEM, new FileSystemApiImpl(properties.getBaseDir()));
        return ossTemplate;
    }

}
