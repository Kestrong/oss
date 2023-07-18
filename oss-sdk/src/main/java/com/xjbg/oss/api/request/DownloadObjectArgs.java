package com.xjbg.oss.api.request;

import lombok.Getter;
import lombok.Setter;

/**
 * @author kesc
 * @date 2020-08-06 15:28
 */
@Getter
@Setter
public class DownloadObjectArgs extends ObjectArgs {
    /**
     * 文件名
     */
    private String fileName;
    private long[] range;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ObjectArgs.Builder<Builder, DownloadObjectArgs> {
        public Builder fileName(String fileName) {
            validateNotEmptyString(fileName, "fileName");
            operations.add(args -> args.fileName = fileName);
            return this;
        }

        public Builder withRange(long start, long end) {
            operations.add(args -> args.range = new long[]{start, end});
            return this;
        }

        public Builder withRange(long start) {
            return withRange(start, Long.MAX_VALUE - 1);
        }
    }
}
