package com.xjbg.oss.api;

import com.xjbg.oss.enums.ApiType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author kesc
 * @date 2020-08-06 17:27
 */
public class OssTemplate {
    private ApiType defaultApiType = ApiType.FILESYSTEM;
    private final Map<ApiType, OssApi> apis = new HashMap<>();

    public void setDefaultApiType(ApiType defaultApiType) {
        this.defaultApiType = defaultApiType;
    }

    public ApiType getDefaultApiType() {
        return defaultApiType;
    }

    public OssTemplate() {
    }

    public boolean isEnable(ApiType apiType) {
        return apis.containsKey(apiType);
    }

    public void register(ApiType apiType, OssApi ossApi) {
        apis.put(apiType, ossApi);
    }

    public OssApi api() {
        return Objects.requireNonNull(apis.get(defaultApiType));
    }

    public OssApi api(ApiType apiType) {
        return Objects.requireNonNull(apis.get(apiType));
    }

    public OssApi s3Api() {
        return apis.get(ApiType.S3);
    }

    public OssApi minioApi() {
        return apis.get(ApiType.MINIO);
    }

    public OssApi fileApi() {
        return apis.get(ApiType.FILESYSTEM);
    }

    public OssApi fusionApi() {
        return apis.get(ApiType.FUSION);
    }

    public OssApi cephApi() {
        return apis.get(ApiType.CEPH);
    }

    public OssApi webHdfsApi() {
        return apis.get(ApiType.WEBHDFS);
    }
}
