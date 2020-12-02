package com.xjbg.oss.application.base;

/**
 * @author kesc
 * @date 2020-04-05 15:16
 */
public class RequestIdHolder {
    public static ThreadLocal<String> requestIdLocal = new ThreadLocal<>();

    public static void setRequestId(String requestId) {
        requestIdLocal.set(requestId);
    }


    public static String getRequestId() {
        return requestIdLocal.get();
    }

    public static void remove() {
        requestIdLocal.remove();
    }
}
