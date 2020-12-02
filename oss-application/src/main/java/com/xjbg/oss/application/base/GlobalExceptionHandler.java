package com.xjbg.oss.application.base;

import com.xjbg.oss.exception.OssException;
import com.xjbg.oss.exception.OssExceptionEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import java.util.*;

/**
 * @author: kesc
 * @Date: 2018/6/11
 * @Description: 通用异常处理
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseBody
    public BaseResponse handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.error("缺少请求参数", e);
        return getResponseMessage(OssExceptionEnum.INVALID_REQUEST_BODY.getCode(),
                "required_parameter_is_not_present");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public BaseResponse handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("参数解析失败", e);
        return getResponseMessage(OssExceptionEnum.INVALID_REQUEST_BODY.getCode(),
                "could_not_read_parameter");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public BaseResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<String> errors = new ArrayList<>();
        for (ObjectError error : e.getBindingResult().getAllErrors()) {
            FieldError fError = (FieldError) error;
            String params = fError.getField();
            String errorMsg = fError.getDefaultMessage();
            errors.add(params + ":" + errorMsg);
        }
        String desc = Arrays.toString(errors.toArray());
        return getResponseMessage(OssExceptionEnum.INVALID_REQUEST_BODY.getCode(), OssExceptionEnum.INVALID_REQUEST_BODY.getMsg() + "," + desc);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    @ResponseBody
    public BaseResponse handleBindException(BindException e) {
        log.error("参数绑定失败", e);
        return getResponseMessage(OssExceptionEnum.INVALID_REQUEST_BODY.getCode(),
                OssExceptionEnum.INVALID_REQUEST_BODY.getMsg());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public BaseResponse handleServiceException(ConstraintViolationException e) {
        log.error("参数验证失败", e);
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        ConstraintViolation<?> violation = violations.iterator().next();
        String message = violation.getMessage();
        return getResponseMessage(OssExceptionEnum.INVALID_REQUEST_BODY.getCode(), OssExceptionEnum.INVALID_REQUEST_BODY.getMsg() + (",parameter:" + message));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ValidationException.class)
    @ResponseBody
    public BaseResponse handleValidationException(ValidationException e) {
        log.error("参数验证失败", e);
        return getResponseMessage(OssExceptionEnum.INVALID_REQUEST_BODY.getCode(), OssExceptionEnum.INVALID_REQUEST_BODY.getMsg());
    }

    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseBody
    public BaseResponse handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("不支持当前请求方法", e);
        return getResponseMessage(OssExceptionEnum.INVALID_REQUEST_METHOD.getCode(), OssExceptionEnum.INVALID_REQUEST_METHOD.getMsg());
    }


    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseBody
    public BaseResponse handleHttpMediaTypeNotSupportedException(Exception e) {
        log.error("不支持当前媒体类型", e);
        return getResponseMessage(OssExceptionEnum.INVALID_CONTENT_TYPE.getCode(), OssExceptionEnum.INVALID_CONTENT_TYPE.getMsg());
    }


    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public BaseResponse system(Exception e) {
        log.error("系统错误", e);
        return getResponseMessage(OssExceptionEnum.SYS_TOO_BUSY.getCode(), OssExceptionEnum.SYS_TOO_BUSY.getMsg());
    }

    @ExceptionHandler(OssException.class)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public BaseResponse ossError(OssException e) {
        return getResponseMessage(e.getCode(), e.getMessage());
    }


    private BaseResponse getResponseMessage(String responseCode, String responseMessage) {
        if (RequestIdHolder.getRequestId() == null) {
            RequestIdHolder.setRequestId(UUID.randomUUID().toString());
        }
        BaseResponse response = new BaseResponse();
        response.setRequestId(RequestIdHolder.getRequestId());
        response.setCode(responseCode);
        response.setMsg(responseMessage);
        return response;
    }

}
