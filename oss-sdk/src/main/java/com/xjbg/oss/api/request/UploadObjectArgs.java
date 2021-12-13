package com.xjbg.oss.api.request;

import com.xjbg.oss.OssConstants;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kesc
 * @date 2020-08-06 15:44
 */
@Getter
@Setter
public class UploadObjectArgs extends ObjectWriteArgs {
    private String fileName;
    private Long partSize = OssConstants.MIN_PART_SIZE;
    /**
     * 递归查找
     */
    private boolean recursive = false;

    public static Builder builder() {
        return new Builder();
    }


    public static class Builder extends ObjectWriteArgs.Builder<Builder, UploadObjectArgs> {

        protected void validatePartSize(Long partSize) {
            if (partSize > 0) {
                if (partSize < OssConstants.MIN_PART_SIZE) {
                    throw new IllegalArgumentException(
                            "part size " + partSize + " is not supported; minimum allowed 5MiB");
                }

                if (partSize > OssConstants.MAX_PART_SIZE) {
                    throw new IllegalArgumentException(
                            "part size " + partSize + " is not supported; maximum allowed 5GiB");
                }
            }
        }

        public Builder partSize(Long partSize) {
            validatePartSize(partSize);
            operations.add(args -> args.partSize = partSize);
            return this;
        }

        public Builder recursive(boolean recursive) {
            operations.add(args -> args.recursive = recursive);
            return this;
        }

        public Builder fileName(String fileName) {
            validateNotEmptyString(fileName, "fileName");
            operations.add(args -> args.fileName = fileName);
            return this;
        }
    }
}
