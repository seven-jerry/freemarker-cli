package com.chopsuey.freemarkercli.config;

import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

@Configuration
public class BeanConfig {


    @Bean
    public freemarker.template.Configuration freeMarkerConfiguration(){
        freemarker.template.Configuration configuration = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_30);
        configuration.setObjectWrapper(new DefaultObjectWrapper(freemarker.template.Configuration.VERSION_2_3_30));
        configuration.setLocale(Locale.ENGLISH);
        configuration.setDefaultEncoding("UTF-8");
        configuration.setIncompatibleImprovements(new Version(2, 3, 30));
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        return configuration;
    }
}
