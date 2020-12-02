package com.xjbg.oss.api.request;

import lombok.Getter;
import lombok.Setter;

/**
 * @author kesc
 * @date 2020-08-06 15:35
 */
@Getter
@Setter
public class ListObjectsArgs extends BucketArgs {
    /**
     * 前缀匹配
     */
    private String prefix = "";
    /**
     * 后缀匹配
     */
    private String suffix;
    /**
     * 最大文件数
     */
    private Integer max = 1000;
    /**
     * 递归查找
     */
    private Boolean recursive = false;

    /**
     * 同suffix
     */
    private String maker;

    private String expectedBucketOwner;

    private String delimiter = "";

    private Boolean useUrlEncoding = true;

    public String getDelimiter() {
        if (recursive) {
            return "";
        }
        return (delimiter == null || delimiter.isEmpty() ? "/" : delimiter);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BucketArgs.Builder<Builder, ListObjectsArgs> {
        public Builder prefix(String prefix) {
            operations.add(args -> args.prefix = prefix);
            return this;
        }

        public Builder max(Integer max) {
            operations.add(args -> args.max = max);
            return this;
        }

        public Builder suffix(String suffix) {
            operations.add(args -> args.suffix = suffix);
            return this;
        }

        public Builder recursive(boolean recursive) {
            operations.add(args -> args.recursive = recursive);
            return this;
        }

        public Builder maker(String maker) {
            operations.add(args -> args.maker = maker);
            return this;
        }

        public Builder expectedBucketOwner(String expectedBucketOwner) {
            operations.add(args -> args.expectedBucketOwner = expectedBucketOwner);
            return this;
        }

        public Builder delimiter(String delimiter) {
            operations.add(args -> args.delimiter = delimiter);
            return this;
        }

        public Builder useUrlEncoding(boolean useUrlEncoding) {
            operations.add(args -> args.useUrlEncoding = useUrlEncoding);
            return this;
        }
    }
}
