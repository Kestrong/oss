package com.xjbg.oss.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * @author kesc
 * @since 2023-07-18 9:19
 */
@Getter
@Setter
public class ObjectMetadataResponse {
    private String bucket;
    private String object;
    private Date lastModified;
    private long length;
    private String etag;
    private String contentType;

    public ObjectMetadataResponse() {
    }

    public ObjectMetadataResponse(String bucket, String object, Date lastModified, long length, String etag, String contentType) {
        this.bucket = bucket;
        this.object = object;
        this.lastModified = lastModified;
        this.length = length;
        this.etag = etag;
        this.contentType = contentType;
    }
}
