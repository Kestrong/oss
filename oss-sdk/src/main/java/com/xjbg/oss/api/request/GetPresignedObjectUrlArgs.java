package com.xjbg.oss.api.request;

import com.amazonaws.HttpMethod;
import com.xjbg.oss.OssConstants;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * @author kesc
 * @date 2020-09-30 16:19
 */
@Getter
@Setter
public class GetPresignedObjectUrlArgs extends ObjectArgs {
    /**
     * @see HttpMethod
     */
    private HttpMethod method;

    /**
     * expires Expiry in seconds; defaults to 15 minutes.
     */
    private Date expiration;

    public HttpMethod getMethod() {
        return method == null ? HttpMethod.GET : method;
    }

    public Date getExpire() {
        if (expiration == null) {
            return new Date(System.currentTimeMillis() + OssConstants.DEFAULT_EXPIRATION_MILLS);
        }
        return expiration;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ObjectArgs.Builder<Builder, GetPresignedObjectUrlArgs> {
        public Builder method(HttpMethod method) {
            operations.add(args -> args.method = method);
            return this;
        }

        public Builder expiration(Date expiration) {
            operations.add(args -> args.expiration = expiration);
            return this;
        }
    }
}
