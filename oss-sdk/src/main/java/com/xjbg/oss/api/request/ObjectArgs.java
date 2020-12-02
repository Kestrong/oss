package com.xjbg.oss.api.request;

import lombok.Getter;
import lombok.Setter;

/**
 * @author kesc
 * @date 2020-11-27 10:04
 */
@Getter
@Setter
public class ObjectArgs extends BucketArgs {
    /**
     * 目标文件名
     */
    protected String object;

    public static class Builder<B extends Builder<B, A>, A extends ObjectArgs> extends BucketArgs.Builder<B, A> {
        protected void validateObjectName(String name) {
            validateNotEmptyString(name, "object name");
        }

        @SuppressWarnings("unchecked")
        public B object(String name) {
            validateObjectName(name);
            operations.add(args -> args.object = name);
            return (B) this;
        }
    }
}
