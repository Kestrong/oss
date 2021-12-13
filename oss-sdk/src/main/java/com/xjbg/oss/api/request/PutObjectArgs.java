package com.xjbg.oss.api.request;

import com.xjbg.oss.OssConstants;
import lombok.Getter;
import lombok.Setter;

import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * @author kesc
 * @date 2020-08-06 15:39
 */
@Getter
@Setter
public class PutObjectArgs extends ObjectWriteArgs {
    private InputStream inputStream;
    private long partSize = OssConstants.MIN_PART_SIZE;

    public static Builder builder() {
        return new Builder();
    }

    public void setInputStream(InputStream inputStream) {
        if (inputStream instanceof BufferedInputStream) {
            this.inputStream = inputStream;
        } else {
            this.inputStream = new BufferedInputStream(inputStream);
        }
    }

    public static class Builder extends ObjectWriteArgs.Builder<Builder, PutObjectArgs> {
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

        public Builder inputStream(InputStream inputStream) {
            validateNotNull(inputStream, "stream");
            operations.add(args -> args.inputStream = inputStream instanceof BufferedInputStream ? inputStream : new BufferedInputStream(inputStream));
            return this;
        }

        public Builder partSize(Long partSize) {
            validatePartSize(partSize);
            operations.add(args -> args.partSize = partSize);
            return this;
        }
    }

}
