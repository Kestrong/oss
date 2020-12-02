package com.xjbg.oss.application.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author kesc
 * @date 2020-08-16 21:10
 */
@Component
public class OssCorsFilter extends OncePerRequestFilter implements ApplicationContextAware {
    private ApplicationContext applicationContext;
    private final CorsProcessor processor = new DefaultCorsProcessor();

    private CorsConfigurationSource configSource() {
        return applicationContext.getBean(UrlBasedCorsConfigurationSource.class);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (CorsUtils.isCorsRequest(request)) {
            CorsConfiguration corsConfiguration = this.configSource().getCorsConfiguration(request);
            if (corsConfiguration != null) {
                boolean isValid = this.processor.processRequest(corsConfiguration, request, response);
                if (!isValid || CorsUtils.isPreFlightRequest(request)) {
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
