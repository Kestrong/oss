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
import com.xjbg.oss.api.impl.filesystem.FileSystemApiImpl;
import com.xjbg.oss.api.impl.s3.CephApiImpl;
import com.xjbg.oss.api.impl.s3.FusionStorageApiImpl;
import com.xjbg.oss.api.impl.s3.MinioApiImpl;
import com.xjbg.oss.api.impl.s3.S3ApiImpl;
import com.xjbg.oss.api.impl.webhdfs.KerberosAuthenticator;
import com.xjbg.oss.api.impl.webhdfs.WebHdfsApiImpl;
import com.xjbg.oss.enums.ApiType;
import com.xjbg.oss.properties.OssProperties;
import lombok.SneakyThrows;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

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
        //register webhdfs
        OssProperties.WebHdfsProperties webHdfsProperties = properties.getWebhdfs();
        if (webHdfsProperties.isEnable() && StringUtils.hasText(webHdfsProperties.getUrl())) {
            OkHttpClient okHttpClient = createOkHttp(clientConfig);
            KerberosAuthenticator kerberosAuthenticator = new KerberosAuthenticator(okHttpClient, webHdfsProperties.getAccessKey(), webHdfsProperties.getSecretKey(), webHdfsProperties.getKrb5(), webHdfsProperties.getServicePrincipal(), webHdfsProperties.isDebug());
            ossTemplate.register(ApiType.WEBHDFS, new WebHdfsApiImpl(okHttpClient, kerberosAuthenticator, webHdfsProperties.getAccessKey(), webHdfsProperties.getSecretKey(), webHdfsProperties.getKrb5(), webHdfsProperties.getUrl(), webHdfsProperties.getDefaultBucket()));
        }
        if (properties.getDefaultApiType() != null) {
            ossTemplate.setDefaultApiType(properties.getDefaultApiType());
        }
        //register filesystem
        ossTemplate.register(ApiType.FILESYSTEM, new FileSystemApiImpl(properties.getBaseDir()));
        return ossTemplate;
    }

    @SneakyThrows
    public OkHttpClient createOkHttp(OssProperties.ClientConfig clientConfig) {
        ConnectionPool connectionPool = new ConnectionPool(clientConfig.getMaxConnections(), clientConfig.getConnectionMaxIdleMillis(), TimeUnit.MILLISECONDS);
        OkHttpClient.Builder builder = new OkHttpClient()
                .newBuilder()
                .connectionPool(connectionPool)
                .connectTimeout(clientConfig.getConnectionTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(clientConfig.getRequestTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(clientConfig.getRequestTimeout(), TimeUnit.MILLISECONDS)
                .protocols(Collections.singletonList(okhttp3.Protocol.HTTP_1_1));
        String filename = System.getProperty("SSL_CERT_FILE");
        if (clientConfig.isUseHttps()) {
            if (StringUtils.hasText(filename)) {
                try {
                    Collection<? extends Certificate> certificates = null;
                    try (FileInputStream fis = new FileInputStream(filename)) {
                        certificates = CertificateFactory.getInstance("X.509").generateCertificates(fis);
                    }

                    if (certificates == null || certificates.isEmpty()) {
                        throw new IllegalArgumentException("expected non-empty set of trusted certificates");
                    }

                    // Any password will work.
                    char[] password = "password".toCharArray();
                    // Put the certificates a key store.
                    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                    // By convention, 'null' creates an empty key store.
                    keyStore.load(null, password);

                    int index = 0;
                    for (Certificate certificate : certificates) {
                        String certificateAlias = Integer.toString(index++);
                        keyStore.setCertificateEntry(certificateAlias, certificate);
                    }

                    // Use it to build an X509 trust manager.
                    KeyManagerFactory keyManagerFactory =
                            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                    keyManagerFactory.init(keyStore, password);
                    TrustManagerFactory trustManagerFactory =
                            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    trustManagerFactory.init(keyStore);

                    final KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
                    final TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(keyManagers, trustManagers, null);
                    SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                    builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustManagers[0]);
                } catch (GeneralSecurityException | IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                SSLContext sc = SSLContext.getInstance("TLS");
                X509TrustManager x509TrustManager = new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                };
                sc.init(null, new X509TrustManager[]{x509TrustManager}, new SecureRandom());
                return builder.hostnameVerifier((hostname, session) -> true)
                        .sslSocketFactory(sc.getSocketFactory(), x509TrustManager).build();
            }
        }
        return builder.build();
    }

}
