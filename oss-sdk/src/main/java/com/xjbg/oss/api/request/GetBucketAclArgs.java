package com.xjbg.oss.api.request;

import lombok.Getter;
import lombok.Setter;

/**
 * @author kesc
 * @date 2020-12-01 17:27
 */
@Getter
@Setter
public class GetBucketAclArgs extends BucketArgs {
    private String expectedBucketOwner;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BucketArgs.Builder<Builder, GetBucketAclArgs> {
        public Builder expectedBucketOwner(String expectedBucketOwner) {
            operations.add(args -> args.expectedBucketOwner = expectedBucketOwner);
            return this;
        }
    }
}
