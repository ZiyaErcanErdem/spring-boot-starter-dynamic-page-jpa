package com.zee.dynamic;

import java.util.HashMap;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.Metamodel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.support.Repositories;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.zee.dynamic.model.PageMetamodel;
import com.zee.dynamic.util.DynamicUtils;

public class DynamicPageContext {
	private final Logger log = LoggerFactory.getLogger(DynamicPageContext.class);
	
	private DynamicPageConfig config;
	private ApplicationContext applicationContext;
	private EntityManager entityManager;
	private Repositories repositories = null;
	
	private HashMap<String, PageMetamodel<?>> queryMetadataCache;
	private HashMap<String, DynamicSearchAuthorizer<?, ?>> authorizerCache;	
	
	public DynamicPageContext(DynamicPageConfig config, ApplicationContext applicationContext, EntityManager entityManager) {
		super();
		this.config = config;
		this.applicationContext = applicationContext;
		this.entityManager = entityManager;
		this.repositories = new Repositories(this.applicationContext);
		
		this.queryMetadataCache = new HashMap<>();
		this.authorizerCache = new HashMap<String, DynamicSearchAuthorizer<?, ?>>();
		
		log.warn("DynamicPage EntityBeanPrefix: " + this.config.getEntityBeanPrefix());
	}
	
	public Metamodel getMetamodel() {
		return this.entityManager.getMetamodel();
	}


	public <T> Class<T> getEntityClassOf(String qualifier) {
		Class<?> clazz = null;
		if (StringUtils.isEmpty(this.config.getEntityBeanPrefix())) {
			clazz = this.getMetamodel().getEntities().stream()
					.filter(e -> e.getJavaType().getSimpleName().equals(qualifier))
					.findFirst().map(e -> e.getJavaType()).orElse(null);

		} else {
			clazz = this.getMetamodel().getEntities().stream()
					.filter(e -> e.getJavaType().getName().equals(this.config.getEntityBeanPrefix() + "." + qualifier))
					.findFirst().map(e -> e.getJavaType()).orElse(null);
			// return DynamicUtils.castTo(this.config.getEntityBeanPrefix(), qualifier);
		}
		return clazz == null ? null : DynamicUtils.castTo(clazz);	
	}
	
	@SuppressWarnings("unchecked")
	private <R> R getRepositoryBeanOfDomainType(Class<?> domainType){
		return  domainType == null ? null : this.repositories.getRepositoryFor(domainType).map(r -> (R) r).orElse(null);

	}
		
	public <T> JpaSpecificationExecutor<T> getSpecificationExecutorBeanOf(String qualifier){
		Class<?> domainType = this.getEntityClassOf(qualifier);
		return domainType == null ? null : getRepositoryBeanOfDomainType(domainType);
	}
	
	public <T> JpaRepository<T, ?> getJpaRepositoryBeanOf(String qualifier){
		Class<?> domainType = this.getEntityClassOf(qualifier);
		return domainType == null ? null : getRepositoryBeanOfDomainType(domainType);
	}
	
	public <T> JpaRepository<T, ?> getJpaRepositoryBeanOf(Class<T> domainType){
		return domainType == null ? null : getRepositoryBeanOfDomainType(domainType);
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
