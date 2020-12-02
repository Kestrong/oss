package com.xjbg.oss.api.request;

import lombok.Getter;
import lombok.Setter;

/**
 * @author kesc
 * @date 2020-11-30 14:28
 */
@Getter
@Setter
public class SetBucketPolicyArgs extends BucketArgs {
    /**
     * The policy to apply to the specified bucket.
     */
    private String policyText;

    /**
     * Whether or not this request can remove requester access to the specified bucket
     */
    private Boolean confirmRemoveSelfBucketAccess;

    private String expectedBucketOwner;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BucketArgs.Builder<Builder, SetBucketPolicyArgs> {
        public Builder policyText(String policyText) {
            operations.add(args -> args.policyText = policyText);
            return this;
        }

        public Builder confirmRemoveSelfBucketAccess(Boolean confirmRemoveSelfBucketAccess) {
            operations.add(args -> args.confirmRemoveSelfBucketAccess = confirmRemoveSelfBucketAccess);
            return this;
        }

        public Builder expectedBucketOwner(String expectedBucketOwner) {
            operations.add(args -> args.expectedBucketOwner = expectedBucketOwner);
            return this;
        }
    }
}
