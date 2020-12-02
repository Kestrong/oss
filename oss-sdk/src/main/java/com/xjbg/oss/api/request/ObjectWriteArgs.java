package com.xjbg.oss.api.request;

import com.xjbg.oss.OssConstants;
import lombok.Getter;
import lombok.Setter;

import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * @author kesc
 * @date 2020-11-30 10:17
 */
@Getter
@Setter
public class ObjectWriteArgs extends ObjectArgs {
    /**
     * media types see:<a href="https://www.iana.org/assignments/media-types/media-types.xhtml"></a>
     */
    protected String contentType = OssConstants.DEFAUL_CONTENT_TYPE;
    protected InputStream inputStream;
    protected Long contentLength = -1L;

    @SuppressWarnings("unchecked")
    public static class Builder<B extends Builder<B, A>, A extends ObjectWriteArgs> extends ObjectArgs.Builder<B, A> {

        public B contentType(String contentType) {
            operations.add(args -> args.contentType = contentType);
            return (B) this;
        }

        public B inputStream(InputStream inputStream) {
            validateNotNull(inputStream, "stream");
            operations.add(args -> args.inputStream = inputStream instanceof BufferedInputStream ? inputStream : new BufferedInputStream(inputStream));
            return (B) this;
        }

        public B contentLength(Long contentLength) {
            validateNotNull(contentLength, "contentLength");
            operations.add(args -> args.contentLength = contentLength);
            return (B) this;
        }
    }
}
