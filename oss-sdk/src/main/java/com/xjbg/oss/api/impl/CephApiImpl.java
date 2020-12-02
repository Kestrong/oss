package com.xjbg.oss.api.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.xjbg.oss.enums.ApiType;
import lombok.NonNull;

/**
 * @author kesc
 * @date 2020-11-06 11:38
 */
public class CephApiImpl extends S3ApiImpl {

    public CephApiImpl(@NonNull AmazonS3 s3Client, String defaultBucket) {
        super(s3Client, defaultBucket);
    }

    public CephApiImpl(@NonNull AmazonS3 s3Client, String defaultBucket, boolean autoCreateBucket) {
        super(s3Client, defaultBucket, autoCreateBucket);
    }

    @Override
    public ApiType apiType() {
        return ApiType.CEPH;
    }
}
