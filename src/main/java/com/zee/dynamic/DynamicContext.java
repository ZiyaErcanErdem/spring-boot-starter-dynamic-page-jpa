package com.zee.dynamic;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.zee.dynamic.model.PageMetamodel;
import com.zee.dynamic.util.DynamicUtils;

public class DynamicContext {
	private final Logger log = LoggerFactory.getLogger(DynamicContext.class);
	
	private DynamicPageConfig config;
	private ApplicationContext applicationContext;
	
	private HashMap<String, PageMetamodel<?>> queryMetadataCache;
	private HashMap<String, DynamicSearchAuthorizer<?, ?>> authorizerCache;	
	
	public DynamicContext(DynamicPageConfig config, ApplicationContext applicationContext) {
		super();
		this.config = config;
		this.applicationContext = applicationContext;
		
		this.queryMetadataCache = new HashMap<>();
		this.authorizerCache = new HashMap<String, DynamicSearchAuthorizer<?, ?>>();
		
		log.warn("DynamicPage EntityBeanPrefix: " + this.config.getEntityBeanPrefix());
		log.warn("DynamicPage RepositoryBeanPrefix: " + this.config.getRepositoryBeanPrefix());
	}


	public <T> Class<T> getEntityClassOf(String qualifier) {
		return DynamicUtils.castTo(this.config.getEntityBeanPrefix(), qualifier);		
	}
	
	public <T> Class<T> getRepositoryClassOf(String qualifier) {
		if(StringUtils.isEmpty(qualifier)){
			return null;
		}
		return DynamicUtils.castTo(this.config.getRepositoryBeanPrefix(), qualifier + "Repository");		
	}
	
	public <T> Class<T> getRepositoryClassOf(Class<?> entityClass) {
		if(null == entityClass){
			return null;
		}
		String qualifier = DynamicUtils.getShortName(entityClass);
		return DynamicUtils.castTo(this.config.getEntityBeanPrefix(), qualifier + "Repository");		
	}
	
	@SuppressWarnings("unchecked")
	public <R> R getRepositoryBeanOf(Class<?> beanType){
		if(null == beanType){
			return null;
		}
		Map<String, ?> beans = this.applicationContext.getBeansOfType(beanType);
		return (null == beans || beans.isEmpty()) ? null : beans.values().stream().findFirst().map(b -> (R) b).orElse(null);

	}
		
	public <T> JpaSpecificationExecutor<T> getSpecificationExecutorBeanOf(String qualifier){
		Class<?> beanType = this.getRepositoryClassOf(qualifier);
		return beanType == null ? null : getRepositoryBeanOf(beanType);
	}
	
	public <T> JpaRepository<T, ?> getJpaRepositoryBeanOf(String qualifier){
		Class<?> beanType = this.getRepositoryClassOf(qualifier);
		return beanType == null ? null : getRepositoryBeanOf(beanType);
	}
	
	public <T> JpaRepository<T, ?> getJpaRepositoryBeanOf(Class<T> entityClass){
		Class<?> beanType = this.getRepositoryClassOf(entityClass);
		return beanType == null ? null : getRepositoryBeanOf(beanType);
	}

	@SuppressWarnings("unchecked")
	public <T, A> DynamicSearchAuthorizer<T, A> getDynamicSearchAuthorizerBeanOf(String qualifier){
		if(StringUtils.isEmpty(qualifier)){
			return null;
		}

		if(this.authorizerCache.containsKey(qualifier)) {
			return (DynamicSearchAuthorizer<T, A>)this.authorizerCache.get(qualifier);
		
		}
		String[] beanNames = this.applicationContext.getBeanNamesForType(DynamicSearchAuthorizer.class);
		if (null == beanNames) {
			return null;
		}
		for (String beanName : beanNames) {
			Class<?> beanClass = this.applicationContext.getType(beanName);
			Class<?> componentClass = ClassUtils.getUserClass(beanClass);
			DynamicQueryAuthorizer annotation = AnnotationUtils.getAnnotation(componentClass, DynamicQueryAuthorizer.class);
	    	if (null == annotation) {
	    		continue;
	    	}
	    	
	    	Object value = AnnotationUtils.getValue(annotation, "entities");
	    	if (null == value) {
	    		continue;
	    	}
			if (Class[].class.isInstance(value)) {
				Class<?>[] entities = Class[].class.cast(value);
                for (Class<?> clazz : entities) {
					String classsName = clazz.getSimpleName();
					if (qualifier.equals(classsName)) {
						return this.applicationContext.getBean(beanName, DynamicSearchAuthorizer.class);
					}
				}
            }
		}
		return null;
	}
	
	public PageMetamodel<?> lookupCachedMetamodel(String qualifier) {
		if(StringUtils.isEmpty(qualifier)){
			return null;
		}
		String cacheKey = DynamicUtils.toFullPath(this.config.getEntityBeanPrefix(), qualifier);
		return this.queryMetadataCache.get(cacheKey);
	}
	public void cacheMetamodel(String qualifier, PageMetamodel<?> qm) {
		if(StringUtils.isEmpty(qualifier)){
			return;
		}
		String cacheKey = DynamicUtils.toFullPath(this.config.getEntityBeanPrefix(), qualifier);
		this.queryMetadataCache.put(cacheKey, qm);
	}

}
