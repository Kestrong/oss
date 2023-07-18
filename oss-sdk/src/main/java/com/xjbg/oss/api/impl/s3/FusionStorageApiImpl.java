package com.xjbg.oss.api.impl.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.xjbg.oss.enums.ApiType;
import lombok.NonNull;

/**
 * @author kesc
 * @date 2020-11-06 11:39
 */
public class FusionStorageApiImpl extends S3ApiImpl {

    public FusionStorageApiImpl(@NonNull AmazonS3 amazonS3, String defaultBucket) {
        super(amazonS3, defaultBucket);
    }

    public FusionStorageApiImpl(@NonNull AmazonS3 amazonS3, String defaultBucket, boolean autoCreateBucket) {
        super(amazonS3, defaultBucket, autoCreateBucket);
    }

    @Override
    public ApiType apiType() {
        return ApiType.FUSION;
    }

}
