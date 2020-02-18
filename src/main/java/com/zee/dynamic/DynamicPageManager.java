package com.zee.dynamic;

import java.io.IOException;
import java.util.List;

import javax.persistence.metamodel.Metamodel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.zee.dynamic.builder.DynamicMetaModelBuilder;
import com.zee.dynamic.builder.GenericRsqlExampleVisitor;
import com.zee.dynamic.builder.GenericRsqlSpecificationVisitor;
import com.zee.dynamic.excel.DynamicExcel;
import com.zee.dynamic.excel.ExcelDataContext;
import com.zee.dynamic.excel.ExcelEntity;
import com.zee.dynamic.excel.ExcelEntityProcessResult;
import com.zee.dynamic.model.DynamicAuthorizableSearchRequest;
import com.zee.dynamic.model.DynamicAuthorizedSearchResponse;
import com.zee.dynamic.model.PageMetamodel;
import com.zee.dynamic.model.RelationType;
import com.zee.dynamic.util.DynamicUtils;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.Node;


// @Service
@Transactional
public class DynamicPageManager {
	private final Logger log = LoggerFactory.getLogger(DynamicPageManager.class);
	
	private DynamicPageContext dynamicPageContext;
	private DynamicPageConfig config; 
	
	public DynamicPageManager(DynamicPageContext dynamicPageContext, DynamicPageConfig config) {
		super();
		this.dynamicPageContext = dynamicPageContext;
		this.config = config;
		
	}

	public <T> Example<T> prepareExampleFor(String qualifier, String query) {
		Class<T> entityClass = this.dynamicPageContext.getEntityClassOf(qualifier);
		return this.prepareExampleFor(entityClass, query);
	}
	
	public <T> Example<T> prepareExampleFor(Class<T> cls, String query) {
		Example<T> example = null;
		if (null == cls) {
			return example;
		}
		T bean = DynamicUtils.createBean(cls);
		BeanWrapper wrapper = DynamicUtils.wrap(bean);
		Node rootNode = new RSQLParser().parse(query);		
		ExampleMatcher matcher = rootNode.accept(new GenericRsqlExampleVisitor<T, ExampleMatcher>(), wrapper);	
		if(null != matcher) {
			example = Example.of(bean, matcher);
		}
		return example;
	}
	
	public <T> Specification<T> prepareSpecFor(String qualifier, String query) {
		Class<T> entityClass = this.dynamicPageContext.getEntityClassOf(qualifier);
		return this.prepareSpecFor(entityClass, query);
	}
	
	public <T> Specification<T> prepareSpecFor(Class<T> cls, String query) {		
		Node rootNode = new RSQLParser().parse(query);		
		Metamodel metamodel = this.dynamicPageContext.getMetamodel();
		Specification<T> spec = rootNode.accept(new GenericRsqlSpecificationVisitor<T, Metamodel>(), metamodel);		
		return spec;
	}
	
	public <T, A> DynamicAuthorizedSearchResponse<T, A> authorize(DynamicAuthorizableSearchRequest request, Page<T> page) {
		DynamicAuthorizedSearchResponse<T,A> response = null;
		String qualifier = request.getQualifier();
		DynamicSearchAuthorizer<T, A> authorizer = this.dynamicPageContext.getDynamicSearchAuthorizerBeanOf(qualifier);
		if (null == authorizer) {
			response = new DynamicAuthorizedSearchResponse<T,A>(qualifier);
			response.setPage(page);
			response.setContent(page.getContent());
			return response;
		}		
		response = authorizer.authorize(request, page);
		return response;
	}
	
	public <T, A> DynamicAuthorizedSearchResponse<T,A> authorize(DynamicAuthorizableSearchRequest request, Pageable pageable) {
		
		String qualifier = request.getQualifier();
		String query = request.getQuery();
		
		Page<T> page = this.search(qualifier, query, pageable);
		DynamicAuthorizedSearchResponse<T,A> response = this.authorize(request, page);
		return response;
	}
	
	public <T, A> DynamicAuthorizedSearchResponse<T,A> search(DynamicAuthorizableSearchRequest request, Pageable pageable) {
		
		String qualifier = request.getQualifier();
		String query = request.getQuery();
		
		Page<T> page = this.search(qualifier, query, pageable);
		DynamicAuthorizedSearchResponse<T,A> response = this.authorize(request, page);
		return response;
	}

	public <T> Page<T> search(Class<T> entityClass, String query, Pageable pageable) {
		String qualifier = DynamicUtils.getShortName(entityClass);
		return this.search(qualifier, query, pageable);
	}	
	
	public <T> Page<T> search(String qualifier, String query, Pageable pageable) {		
		if(StringUtils.isEmpty(query)){			
			return this.search(qualifier, pageable);
		}
		
		Example<T> example = this.prepareExampleFor(qualifier, query);
		if(null != example) {
			return this.search(qualifier, example, pageable);
		} else {
			Specification<T> spec = this.prepareSpecFor(qualifier, query);
			return this.search(qualifier, spec, pageable);
		}
	}
	
	public <T> Page<T> search(String qualifier, Pageable pageable) {
		JpaRepository<T, ?> repository = this.dynamicPageContext.getJpaRepositoryBeanOf(qualifier);
		Page<T> page =  repository.findAll(pageable);
		return page;
	}
	
	public <T> Page<T> search(String qualifier, Example<T> example, Pageable pageable) {
		JpaRepository<T, ?> repository = this.dynamicPageContext.getJpaRepositoryBeanOf(qualifier);
		Page<T> page = repository.findAll(example, pageable);
		return page;
	}
	
	public <T> Page<T> search(String qualifier, Specification<T> spec, Pageable pageable) {
		JpaSpecificationExecutor<T> repository = this.dynamicPageContext.getSpecificationExecutorBeanOf(qualifier);		
		Page<T> page = repository.findAll(spec, pageable);
		return page;
	}
	
	public <T> Resource exportTemplate(String qualifier) throws IOException {
		Class<T> entityClass = this.dynamicPageContext.getEntityClassOf(qualifier);
		return this.exportTemplate(entityClass);
	}
	
	public <T> Resource exportTemplate(Class<T> entityClass)  throws IOException{
		List<T> entities = null;
		PageMetamodel<T> metamodel = this.getPageMetamodelOf(entityClass);
		DynamicExcel<T> dynamicExcel = new DynamicExcel<T>(metamodel);
		
		Resource resource = dynamicExcel.write(entities);
		return resource;
	}
	
	public <T> Resource exportEntity(String qualifier, String query, Pageable pageable) throws IOException {
		Class<T> entityClass = this.dynamicPageContext.getEntityClassOf(qualifier);
		return this.exportEntity(entityClass, query, pageable);
	}
	
	public <T> Resource exportEntity(Class<T> entityClass, String query, Pageable pageable)  throws IOException{
		String qualifier = DynamicUtils.getShortName(entityClass);
		Page<T> searchResult = search(qualifier, query, pageable);		
		List<T> entities = (null != searchResult ? searchResult.getContent() : null);
		
		PageMetamodel<T> metamodel = this.getPageMetamodelOf(entityClass);
		DynamicExcel<T> dynamicExcel = new DynamicExcel<T>(metamodel);
		
		Resource resource =  dynamicExcel.write(entities);
		return resource;
	}
	
	public <T> Resource importEntity(String qualifier, Resource resource) throws IOException {
		Class<T> entityClass = this.dynamicPageContext.getEntityClassOf(qualifier);
		return this.importEntity(entityClass, resource);
	}
	
	public <T> Resource importEntity(Class<T> entityClass, Resource resource) throws IOException {
		JpaRepository<T, ?> repository = this.dynamicPageContext.getJpaRepositoryBeanOf(entityClass);
		
		PageMetamodel<T> metamodel = this.getPageMetamodelOf(entityClass);
		DynamicExcel<T> dynamicExcel = new DynamicExcel<T>(metamodel);
		ExcelDataContext<T> readContext = dynamicExcel.read(resource);
		List<ExcelEntity<T>> data = readContext.getData();
		
		if(null != data) {
			data.forEach(d -> {
				try {
					if(!d.hasError()) {
						T updated = repository.save(d.getEntity());
						d.setEntity(updated);
						d.setResult(ExcelEntityProcessResult.SUCCESS);
					}
					
				}catch(Exception ex) {
					d.setProcessingError(ExcelEntityProcessResult.DATABASE_ERROR, ex);
				}
			});
		}
		
		Resource processed = dynamicExcel.write(readContext);
		return processed;
	}
	
	public <T> PageMetamodel<?> getPageMetamodelOf(String qualifier) {
		if(StringUtils.isEmpty(qualifier)){
			return new PageMetamodel<T>(null, qualifier, RelationType.SELF, "", 0);
		}

		PageMetamodel<?> qm = this.dynamicPageContext.lookupCachedMetamodel(qualifier);
		if(null != qm) {
			return qm;
		}
		Class<T> entityClass = this.dynamicPageContext.getEntityClassOf(qualifier);
		if(null == entityClass) {
			return new PageMetamodel<T>(null, qualifier, RelationType.SELF, "", 0);
		}
		qm = this.getPageMetamodelOf(entityClass);
		this.dynamicPageContext.cacheMetamodel(qualifier, qm);
		return qm;
	}
	
	public <T> PageMetamodel<T> getPageMetamodelOf(Class<T> cls) {
		DynamicMetaModelBuilder<T> builder = new DynamicMetaModelBuilder<>(this.config, cls);
		PageMetamodel<T> metamodel = builder.build();
		//metamodel.enhance();
		return metamodel;
	}
	
}

