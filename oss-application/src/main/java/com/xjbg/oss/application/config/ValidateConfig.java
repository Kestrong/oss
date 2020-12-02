package com.xjbg.oss.application.config;

import com.xjbg.oss.enums.Encoding;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import java.util.Properties;

/**
 * @author kesc
 * @since 2019/3/14
 */
@Configuration
public class ValidateConfig {
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setDefaultEncoding(Encoding.UTF_8.getEncoding());
        messageSource.setCacheSeconds(31536000);
        messageSource.setUseCodeAsDefaultMessage(false);
        messageSource.setBasename("i18n/messages");
        messageSource.setFallbackToSystemLocale(true);
        return messageSource;
    }

    @Bean
    public LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();
        validatorFactoryBean.setProviderClass(org.hibernate.validator.HibernateValidator.class);
        validatorFactoryBean.setValidationMessageSource(messageSource());
        Properties properties = new Properties();
        properties.setProperty("hibernate.validator.fail_fast", "true");
        validatorFactoryBean.setValidationProperties(properties);
        return validatorFactoryBean;
    }

    @Bean
    public MethodValidationPostProcessor methodValidator() {
        MethodValidationPostProcessor postProcessor = new MethodValidationPostProcessor();
        postProcessor.setValidator(validator());
        return postProcessor;
    }

}
