package com.xjbg.oss.application.base;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.xjbg.oss.exception.OssExceptionEnum;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kesc
 * @date 2020-04-04 22:18
 */
@Getter
@Setter
public class BaseResponse<T> {
    private String requestId;
    private String code = OssExceptionEnum.SUCCESS.getCode();
    private String msg = OssExceptionEnum.SUCCESS.getMsg();
    @JSONField(serialzeFeatures = SerializerFeature.WriteMapNullValue)
    private T data;

    public BaseResponse() {
    }

    public BaseResponse(T data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return "0".equals(code);
    }
}
