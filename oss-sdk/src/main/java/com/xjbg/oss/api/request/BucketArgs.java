package com.xjbg.oss.api.request;

import lombok.Getter;
import lombok.Setter;

/**
 * @author kesc
 * @date 2020-11-27 9:59
 */
@Getter
@Setter
public class BucketArgs extends BaseArgs {
    /**
     * 目标bucket
     */
    protected String bucket;

    public static class Builder<B extends Builder<B, A>, A extends BucketArgs> extends BaseArgs.Builder<B, A> {

        @SuppressWarnings("unchecked")
        public B bucket(String name) {
            validateNotNull(name, "bucket name");
            operations.add(args -> args.bucket = name);
            return (B) this;
        }
    }
}
