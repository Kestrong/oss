package com.xjbg.oss.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * @author kesc
 * @date 2020-04-04 22:17
 */
@Getter
@Setter
public class OssException extends RuntimeException {
    private String code;

    public OssException(OssExceptionEnum exceptionEnum, Object bindingResult) {
        super(exceptionEnum.getMsg());
        this.code = exceptionEnum.getCode();
    }

    public OssException(String message) {
        super(message);
    }

    public OssException(String message, Throwable arg1) {
        super(message, arg1);
    }

    public OssException(String code, String message) {
        super(message);
        this.code = code;
    }

    public OssException(OssExceptionEnum exceptionEnum, Throwable arg1) {
        super(exceptionEnum.getMsg(), arg1);
        this.code = exceptionEnum.getCode();
    }

    public OssException(String code, String message, Throwable arg1) {
        super(message, arg1);
        this.code = code;
    }
}
