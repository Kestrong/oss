package com.xjbg.oss.application.base;

import com.alibaba.fastjson.JSON;
import com.xjbg.oss.OssConstants;
import com.xjbg.oss.exception.OssException;
import com.xjbg.oss.exception.OssExceptionEnum;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.eclipse.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @author kesc
 * @since 16/10/8
 */
@Aspect
@Order
@Component
public class ApiMessageAdvisor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiMessageAdvisor.class);
    @Autowired(required = false)
    private HttpServletResponse httpServletResponse;

    @Around("execution(public * com.xjbg.oss.application..*..*Controller.*(..))")
    public Object invokeAPI(ProceedingJoinPoint pjp) {
        StopWatch watch = new StopWatch();
        watch.start();
        RequestIdHolder.setRequestId(UUID.randomUUID().toString());
        String method = pjp.getSignature().getName();
        String className = pjp.getTarget().getClass().getSimpleName();
        String apiName = className + "#" + method;
        Class<?> returnClazz = ((MethodSignature) pjp.getSignature()).getReturnType();
        Object returnValue;
        try {
            Object[] args = pjp.getArgs();
            StringBuilder sb = new StringBuilder();
            if (args != null && args.length > 0) {
                for (Object arg : args) {
                    sb.append(logMessage(arg)).append("*****");
                }
                LOGGER.info("@@{} started,request:{}", apiName, sb.toString());
            } else {
                LOGGER.info("@@" + apiName + " started,request: 方法不需要参数");
            }
            returnValue = pjp.proceed();
            this.handleSuccess(returnValue);
        } catch (OssException e) {
            returnValue = this.handleBusinessError(e, apiName, returnClazz);
        } catch (Throwable e) {
            returnValue = this.handleSystemError(e, apiName, returnClazz);
        } finally {
            RequestIdHolder.remove();
        }
        watch.stop();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("@@{} done,response:{},time spends {} ms", apiName, JSON.toJSONString(returnValue), watch.getTotalTimeMillis());
        }
        return returnValue;
    }

    private String logMessage(Object arg) {
        try {
            if ((arg instanceof HttpServletRequest) || (arg instanceof HttpServletResponse)) {
                return arg.getClass().getSimpleName();
            } else if (arg instanceof MultipartFile) {
                String fileName = ((MultipartFile) arg).getName();
                return "file[" + fileName + "]";
            } else {
                return JSON.toJSONString(arg);
            }
        } catch (Exception e) {
            return "param parse error:" + e.getMessage();
        }
    }

    private Object handle(String errorCode, String errorMessage, Class<?> returnClazz) {
        BaseResponse response = new BaseResponse();
        response.setRequestId(RequestIdHolder.getRequestId());
        response.setCode(errorCode);
        response.setMsg(errorMessage);
        if (!returnClazz.isAssignableFrom(BaseResponse.class)) {
            try {
                httpServletResponse.setContentType(OssConstants.APPLICATION_JSON_VALUE);
                httpServletResponse.getOutputStream().write(JSON.toJSONString(response).getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
            }
        }
        return response;
    }

    private Object handleBusinessError(OssException e, String apiName, Class<?> returnClazz) {
        String errorCode = e.getCode();
        String errorMessage = e.getMessage();
        LOGGER.error("@Meet error when do {} [{}]:{}", apiName, errorCode, errorMessage, e);
        return handle(errorCode, errorMessage, returnClazz);
    }

    private Object handleSystemError(Throwable e, String apiName, Class<?> returnClazz) {
        String errorMessage = e.getMessage();
        LOGGER.error("@Meet unkonw error when do {} :{}", apiName, errorMessage, e);
        return handle(OssExceptionEnum.SYS_TOO_BUSY.getCode(), OssExceptionEnum.SYS_TOO_BUSY.getMsg(), returnClazz);
    }

    private void handleSuccess(Object returnValue) {
        if (returnValue instanceof BaseResponse) {
            BaseResponse response = (BaseResponse) returnValue;
            if (StringUtil.isEmpty(response.getCode())) {
                response.setCode(OssExceptionEnum.SUCCESS.getCode());
                response.setMsg(OssExceptionEnum.SUCCESS.getMsg());
            }
            response.setRequestId(RequestIdHolder.getRequestId());
        }
    }
}
