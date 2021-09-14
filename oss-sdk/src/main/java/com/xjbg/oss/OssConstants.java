package com.xjbg.oss;

/**
 * @author kesc
 * @date 2020-11-26 17:51
 */
public class OssConstants {
    public static final String DEFAUL_CONTENT_TYPE = "application/octet-stream";
    public static final long DEFAULT_EXPIRATION_MILLS = 15 * 60 * 1000L;
    public static final long MIN_PART_SIZE = 1024 * 1024 * 5L;
    public static final long MAX_PART_SIZE = 5L * 1024 * 1024 * 1024;
    public static final String MULTIPART_FORM_DATA_VALUE = "multipart/form-data";
    public static final String APPLICATION_JSON_UTF8_VALUE = "application/json;charset=UTF-8";
    public static final String APPLICATION_JSON_VALUE = "application/json";
    public static final String APPLICATION_OCTET_STREAM_VALUE = "application/octet-stream";
}
