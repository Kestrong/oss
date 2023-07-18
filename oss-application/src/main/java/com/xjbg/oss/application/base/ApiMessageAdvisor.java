package com.xjbg.oss.application.base;

import com.alibaba.fastjson.JSON;
import com.xjbg.oss.exception.OssExceptionEnum;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * @author kesc
 * @since 16/10/8
 */
@Aspect
@Order
@Component
public class ApiMessageAdvisor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiMessageAdvisor.class);

    @Around("execution(public * com.xjbg.oss.application..*..*Controller.*(..))")
    public Object invokeApi(ProceedingJoinPoint pjp) throws Throwable {
        StopWatch watch = new StopWatch();
        watch.start();
        String method = pjp.getSignature().getName();
        String className = pjp.getTarget().getClass().getSimpleName();
        String apiName = className + "#" + method;
        Object returnValue = null;
        try {
            logParam(apiName, pjp);
            returnValue = pjp.proceed();
            this.handleSuccess(returnValue);
            return returnValue;
        } finally {
            watch.stop();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("@@{} done, time spends {} ms, response:{}", apiName, watch.getTotalTimeMillis(), JSON.toJSONString(returnValue));
            }
        }
    }

    private void logParam(String apiName, ProceedingJoinPoint pjp) {
        try {
            Object[] args = pjp.getArgs();
            if (args != null && args.length > 0) {
                String[] parameterNames = ((MethodSignature) pjp.getSignature()).getParameterNames();
                Map<String, Object> paramsMap = new HashMap<>(args.length * 3 / 2);
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    if ((arg instanceof HttpServletRequest) || (arg instanceof HttpServletResponse)) {
                        paramsMap.put(parameterNames[i], arg.getClass().getSimpleName());
                    } else if (arg instanceof MultipartFile) {
                        paramsMap.put(parameterNames[i], ((MultipartFile) arg).getOriginalFilename());
                    } else {
                        paramsMap.put(parameterNames[i], arg);
                    }
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("@@{} started, request:{}", apiName, JSON.toJSONString(paramsMap));
                }
            } else {
                LOGGER.debug("@@{} started, request:方法不需要参数", apiName);
            }
        } catch (Exception e) {
            LOGGER.warn("@@{} started, param parse error:", apiName, e);
        }
    }

    private void handleSuccess(Object returnValue) {
        if (returnValue instanceof BaseResponse) {
            BaseResponse response = (BaseResponse) returnValue;
            if (StringUtils.isEmpty(response.getCode())) {
                response.setCode(OssExceptionEnum.SUCCESS.getCode());
                response.setMsg(OssExceptionEnum.SUCCESS.getMsg());
            }
            response.setRequestId(RequestIdHolder.getRequestId());
        }
    }
}
