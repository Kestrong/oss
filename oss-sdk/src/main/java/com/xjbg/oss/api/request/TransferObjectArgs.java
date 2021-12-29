package com.xjbg.oss.api.request;

import com.xjbg.oss.api.OssApi;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kesc
 * @date 2021-12-29 14:57
 */
@Getter
@Setter
public class TransferObjectArgs extends ObjectArgs {
    /**
     * 源bucket
     */
    private String srcBucket;
    /**
     * 源文件名
     */
    private String srcObject;
    /**
     * 目标存储类型
     */
    private OssApi targetApi;

    public static TransferObjectArgs.Builder builder() {
        return new TransferObjectArgs.Builder();
    }

    public static class Builder extends ObjectArgs.Builder<TransferObjectArgs.Builder, TransferObjectArgs> {

        public TransferObjectArgs.Builder srcObject(String srcObject) {
            validateNotEmptyString(srcObject, "srcObject name");
            operations.add(args -> args.srcObject = srcObject);
            return this;
        }

        public TransferObjectArgs.Builder srcBucket(String srcBucket) {
            validateNotEmptyString(srcBucket, "srcBucket name");
            operations.add(args -> args.srcBucket = srcBucket);
            return this;
        }

        public TransferObjectArgs.Builder targetApi(OssApi targetApi) {
            validateNotNull(targetApi, "targetApi");
            operations.add(args -> args.targetApi = targetApi);
            return this;
        }

    }
}
