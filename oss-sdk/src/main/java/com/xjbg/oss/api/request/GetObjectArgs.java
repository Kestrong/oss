package com.xjbg.oss.api.request;

import lombok.Getter;
import lombok.Setter;

/**
 * @author kesc
 * @date 2020-08-06 15:19
 */
@Getter
@Setter
public class GetObjectArgs extends ObjectArgs {
    private long[] range;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ObjectArgs.Builder<Builder, GetObjectArgs> {

        public GetObjectArgs.Builder withRange(long start, long end) {
            operations.add(args -> args.range = new long[]{start, end});
            return this;
        }

        public GetObjectArgs.Builder withRange(long start) {
            return withRange(start, Long.MAX_VALUE - 1);
        }
    }
}
