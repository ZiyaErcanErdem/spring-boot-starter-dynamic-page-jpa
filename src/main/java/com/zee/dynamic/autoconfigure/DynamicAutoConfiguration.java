package com.zee.dynamic.autoconfigure;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.zee.dynamic.DynamicPageConfig;
import com.zee.dynamic.DynamicPageManager;
import com.zee.dynamic.config.DynamicProperties;

@Configuration
@ConditionalOnClass(DynamicPageManager.class)
@EnableConfigurationProperties(DynamicProperties.class)
@ComponentScan(basePackages= { "com.zee.dynamic.resource", "com.zee.dynamic.excel.resource" }) 
public class DynamicAutoConfiguration {
	private final Logger log = LoggerFactory.getLogger(DynamicAutoConfiguration.class);
	
    @Autowired
    private DynamicProperties dynamicProperties;
 
    @Bean
    @ConditionalOnMissingBean
    public DynamicPageConfig dynamicPageConfig() {
 
        log.warn("Starting DynamicPage auto configuration");
        
        String entityBeanPrefix = dynamicProperties.getJpaEntityPackageName() == null
                ? System.getProperty("dynamic.entity.prefix") 
                : dynamicProperties.getJpaEntityPackageName();
         
          String repositoryBeanPrefix = dynamicProperties.getJpaRepositoryPackageName() == null
                  ? System.getProperty("dynamic.repository.prefix") 
                  : dynamicProperties.getJpaRepositoryPackageName();
           
          DynamicPageConfig config = new DynamicPageConfig();
          config.setEntityBeanPrefix(entityBeanPrefix);
          config.setRepositoryBeanPrefix(repositoryBeanPrefix);
          
          /*
          config.defineMaxDeepForInnerJoin("FlowExecution");
          config.defineMaxDeepForInnerJoin("TaskExecution");
          config.defineMaxDeepForInnerJoin("ActionExecution");
          config.defineMaxDeepForInnerJoin("Agent");
          
          
          config.defineMaxDeepForOuterJoin("Agent");
          config.defineMaxDeepForOuterJoin("Endpoint");         
          config.defineMaxDeepForOuterJoin("Content");
          config.defineMaxDeepForOuterJoin("ContentValidationError");
          */
          
          
          return config;
    }
 
    @Bean
    @ConditionalOnMissingBean
    public DynamicPageManager dynamicPageManager(DynamicPageConfig config, ApplicationContext applicationContext, EntityManager entityManager) {
        return new DynamicPageManager(config, applicationContext, entityManager);
    }

}
