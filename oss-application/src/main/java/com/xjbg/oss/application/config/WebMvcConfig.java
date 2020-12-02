package com.xjbg.oss.application.config;

import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.xjbg.oss.application.properties.CorsProperties;
import com.xjbg.oss.enums.DatePatternEnum;
import com.xjbg.oss.enums.Encoding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author kesc
 * @since 2019/2/27
 */
@Configuration
@EnableConfigurationProperties(value = {CorsProperties.class})
public class WebMvcConfig extends WebMvcConfigurationSupport {

    @Autowired
    private LocalValidatorFactoryBean validator;
    @Autowired
    private CorsProperties corsProperties;

    @Bean
    @RefreshScope
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(corsProperties.isAllowCredentials());
        config.setAllowedHeaders(corsProperties.getAllowedHeaders());
        config.setAllowedMethods(corsProperties.getAllowedMethods());
        config.setAllowedOrigins(corsProperties.getAllowedOrigins());
        config.setExposedHeaders(corsProperties.getExposedHeaders());
        config.setMaxAge(corsProperties.getMaxAge());
        source.registerCorsConfiguration(corsProperties.getPathPattern(), config);
        return source;
    }

    @Bean
    public FilterRegistrationBean<OssCorsFilter> corsFilterFilterRegistrationBean(OssCorsFilter ossCorsFilter) {
        FilterRegistrationBean<OssCorsFilter> bean = new FilterRegistrationBean<>(ossCorsFilter);
        bean.setOrder(0);
        return bean;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        super.addInterceptors(registry);
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.addAll(messageConverters());
    }

    private List<HttpMessageConverter<?>> messageConverters() {
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        FastJsonHttpMessageConverter jsonConverter = new FastJsonHttpMessageConverter();
        jsonConverter.setSupportedMediaTypes(Arrays.asList(MediaType.APPLICATION_JSON_UTF8, MediaType.APPLICATION_XML, MediaType.APPLICATION_RSS_XML, MediaType.TEXT_PLAIN, MediaType.TEXT_HTML));
        FastJsonConfig fastJsonConfig = new FastJsonConfig();
        fastJsonConfig.setDateFormat(DatePatternEnum.YYYYMMDDHHMMSS_BYSEP.getFormat());
        fastJsonConfig.setFeatures(Feature.DisableCircularReferenceDetect);
        fastJsonConfig.setSerializerFeatures(SerializerFeature.QuoteFieldNames, SerializerFeature.WriteNullListAsEmpty, SerializerFeature.WriteMapNullValue, SerializerFeature.SkipTransientField);
        jsonConverter.setFastJsonConfig(fastJsonConfig);
        messageConverters.add(jsonConverter);
        return messageConverters;
    }

    @Bean
    public HttpMessageConverters getHttpMessageConverters() {
        return new HttpMessageConverters(messageConverters());
    }

    @Bean
    @Primary
    public CommonsMultipartResolver getCommonsMultipartResolver() {
        CommonsMultipartResolver commonsMultipartResolver = new CommonsMultipartResolver();
        commonsMultipartResolver.setMaxUploadSize(-1);
        commonsMultipartResolver.setMaxInMemorySize(1024 * 10);
        commonsMultipartResolver.setDefaultEncoding(Encoding.UTF_8.getEncoding());
        commonsMultipartResolver.setResolveLazily(Boolean.TRUE);
        return commonsMultipartResolver;
    }

    @Override
    protected Validator getValidator() {
        return validator;
    }

//    @Override
//    protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
//        return new RequestMappingHandlerMapping() {
//            @Override
//            protected boolean isHandler(Class<?> beanType) {
//                return (AnnotatedElementUtils.hasAnnotation(beanType, Controller.class) ||
//                        (AnnotatedElementUtils.hasAnnotation(beanType, RequestMapping.class) && !AnnotatedElementUtils.hasAnnotation(beanType, FeignClient.class))
//                );
//            }
//        };
//    }
}
