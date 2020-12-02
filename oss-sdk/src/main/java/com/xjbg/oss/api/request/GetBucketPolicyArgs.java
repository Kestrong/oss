package com.xjbg.oss.api.request;

import lombok.Getter;
import lombok.Setter;

/**
 * @author kesc
 * @date 2020-12-01 17:29
 */
@Getter
@Setter
public class GetBucketPolicyArgs extends BucketArgs {
    private String expectedBucketOwner;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BucketArgs.Builder<Builder, GetBucketPolicyArgs> {
        public Builder expectedBucketOwner(String expectedBucketOwner) {
            operations.add(args -> args.expectedBucketOwner = expectedBucketOwner);
            return this;
        }
    }

}
