package com.xjbg.oss.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;

/**
 * @author kesc
 * @date 2020-08-13 11:31
 */
@SpringBootApplication(scanBasePackages = "com.xjbg.oss", exclude = {HttpMessageConvertersAutoConfiguration.class, MultipartAutoConfiguration.class, DataSourceAutoConfiguration.class, SessionAutoConfiguration.class, RedisAutoConfiguration.class})
public class OssApplication {

    public static void main(String[] args) {
        SpringApplication.run(OssApplication.class, args);
    }

}
