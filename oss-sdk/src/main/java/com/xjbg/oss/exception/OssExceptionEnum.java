package com.xjbg.oss.exception;

import lombok.Getter;

/**
 * @author kesc
 * @date 2020-04-04 22:17
 */
@Getter
public enum OssExceptionEnum {
    SUCCESS("0", "成功"),
    BUCKET_OPERATION_ERROR("OSS-0000001", "bucket操作异常"),
    BUCKET_NAME_NOT_EXIST("OSS-0000002", "bucketName不能为空"),
    COPY_OBJECT_ERROR("OSS-0000003", "复制文件失败"),
    GET_OBJECT_ERROR("OSS-0000004", "获取文件失败"),
    PUT_OBJECT_ERROR("OSS-0000005", "上传文件失败"),
    DELETE_OBJECT_ERROR("OSS-0000006", "删除文件失败"),
    GET_OBJECT_URL_ERROR("OSS-0000006", "获取文件Url失败"),
    FILE_NOT_EXIST("OSS-0000007", "文件不存在"),
    OBJECT_NAME_NOT_EXIST("OSS-0000008", "objectName不能为空"),
    SYS_TOO_BUSY("OSS-0000009", "系统太忙啦！"),
    INVALID_CONTENT_TYPE("OSS-0000010", "Content-Type Not Supported"),
    INVALID_REQUEST_METHOD("OSS-0000011", "Method Not Allowed"),
    INVALID_REQUEST_BODY("OSS-0000012", "请求参数错误"),
    BUCKET_ACL_ERROR("OSS-0000013", "操作bucket权限失败"),
    BUCKET_POLICY_ERROR("OSS-0000014", "操作bucket策略失败"),
    OBJECT_ACL_ERROR("OSS-0000015", "操作object权限失败"),
    ;
    private String code;
    private String msg;

    OssExceptionEnum(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public OssException getException() {
        return new OssException(this.getCode(), this.getMsg());
    }

    public OssException getException(Throwable e) {
        return new OssException(this.getCode(), this.getMsg(), e);
    }
}
