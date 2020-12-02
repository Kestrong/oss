package com.xjbg.oss.api.response;

import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;
import java.util.Map;

/**
 * @author kesc
 * @date 2020-11-30 11:13
 */
@Getter
@Setter
public class GetObjectResponse {

    /**
     * The key under which this object is stored
     */
    private String object = null;

    /**
     * The name of the bucket in which this object is contained
     */
    private String bucket = null;

    /**
     * The stream containing the contents of this object from S3
     */
    private InputStream inputStream;

    /**
     * The redirect location for this object
     */
    private String redirectLocation;

    /**
     * Custom user metadata, represented in responses with the x-amz-meta-
     * header prefix
     */
    private Map<String, String> userMetadata;

    /**
     * All other (non user custom) headers such as Content-Length, Content-Type,
     * etc.
     */
    private Map<String, Object> metadata;

}
