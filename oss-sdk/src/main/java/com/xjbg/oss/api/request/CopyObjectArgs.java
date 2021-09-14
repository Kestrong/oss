package com.xjbg.oss.api.request;

import lombok.Getter;
import lombok.Setter;

/**
 * @author kesc
 * @date 2020-08-06 15:11
 */
@Getter
@Setter
public class CopyObjectArgs extends ObjectArgs {
    /**
     * 源bucket
     */
    private String srcBucket;
    /**
     * 源文件名
     */
    private String srcObject;

    /**
     * 拷贝完删除源文件或目录
     */
    private Boolean delete;

    public Boolean getDelete() {
        return delete == null ? Boolean.FALSE : delete;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ObjectArgs.Builder<Builder, CopyObjectArgs> {
        public Builder srcObject(String srcObject) {
            validateNotEmptyString(srcObject, "srcObject name");
            operations.add(args -> args.srcObject = srcObject);
            return this;
        }

        public Builder srcBucket(String srcBucket) {
            validateNotEmptyString(srcBucket, "srcBucket name");
            operations.add(args -> args.srcBucket = srcBucket);
            return this;
        }

        @Override
        public Builder object(String name) {
            operations.add(args -> args.object = name);
            return this;
        }

        public Builder delete(Boolean delete) {
            operations.add(args -> args.delete = delete);
            return this;
        }
    }
}
