package com.xjbg.oss.api.request;

import lombok.Getter;
import lombok.Setter;

/**
 * @author kesc
 * @date 2020-12-01 17:27
 */
@Getter
@Setter
public class GetObjectAclArgs extends ObjectArgs {
    private String expectedBucketOwner;
    private String versionId;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ObjectArgs.Builder<Builder, GetObjectAclArgs> {
        public Builder expectedBucketOwner(String expectedBucketOwner) {
            operations.add(args -> args.expectedBucketOwner = expectedBucketOwner);
            return this;
        }

        public Builder versionId(String versionId) {
            operations.add(args -> args.versionId = versionId);
            return this;
        }
    }
}
