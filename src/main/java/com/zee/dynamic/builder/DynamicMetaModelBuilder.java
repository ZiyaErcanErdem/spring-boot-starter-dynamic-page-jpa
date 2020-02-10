package com.zee.dynamic.builder;

import java.beans.PropertyDescriptor;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.StringUtils;

import com.zee.dynamic.DynamicPageConfig;
import com.zee.dynamic.MetamodelContext;
import com.zee.dynamic.model.PageMetamodel;
import com.zee.dynamic.model.RelationType;

public class DynamicMetaModelBuilder<T> {
	private DynamicPageConfig config;
	private Class<T> entityClass;
	private MetamodelContext<T> context;
	
	public DynamicMetaModelBuilder(DynamicPageConfig config, Class<T> entityClass) {
		this.config = config;
		this.entityClass = entityClass;
	}
	
	public PageMetamodel<T> build() {
		if(null == this.entityClass) {
			return null;
		}
		String topPathPrefix = "";
		int topLevel = 1;
		int outerJoinCountToTop = 0;
		RelationType topRelationType = RelationType.SELF;
		
		PageMetamodel<T> topMetamodel = this.defineMetamodel(this.entityClass, topRelationType, topLevel, outerJoinCountToTop);
		this.context = new MetamodelContext<T>(this.config, topMetamodel);
		
		this.defineColumns(this.context, topMetamodel, topPathPrefix, this.entityClass);
		
		while(this.context.hasActiveTasks()) {
			AssociationTask<?> task = this.context.getNextActiveTask();
			if(null == task) {
				continue;
			}
			this.processTask(this.context, task);
		}	
		return this.context.getMetamodel();
	}
	
	private void processTask(MetamodelContext<T> context, AssociationTask<?> task) {
		ColumnDescriptor<?> descriptor = task.getMinDescriber();
		
		if(null == descriptor) {
			task.complete(null);
			return;
		}
		
		//String parentPropertyName = descriptor.getPropertyName();
		String parentPath = descriptor.getPropertyPath();
		//RelationType parentRelationType = descriptor.getParentRelationType();
		int associationLevel = descriptor.getLevel() + 1;		
		RelationType associationRelationType = descriptor.getAssociationRelationType();
		//Class<?> associationEntityClass = columnDescriptor.getAssociationType();
		Class<?> associationEntityClass = task.getPropertyType();
		int associationOuterJoinCountToTop = descriptor.getOuterJoinCountToTop();
		if(RelationType.OUTER == associationRelationType) {
			associationOuterJoinCountToTop++;
		}

		PageMetamodel<?> association = this.defineMetamodel(associationEntityClass, associationRelationType, associationLevel, associationOuterJoinCountToTop);
		this.defineColumns(context, association, parentPath, associationEntityClass);
		association.describe(task);
		// task.complete(association);
	}
	
	private <M> PageMetamodel<M> defineMetamodel(Class<M> entityClass, RelationType relationType, int level, int outerJoinCountToTop) {
		if(null == entityClass) {
			return null;
		}
		PageMetamodel<M> metamodel = new PageMetamodel<M>(entityClass, relationType, level, outerJoinCountToTop);
		return metamodel;
	}
		
	private void defineColumns(MetamodelContext<T> context, PageMetamodel<?> targetMetamodel, String parentPathPrefix, Class<?> entityClass) {
		
		if(null == entityClass) {
			return;
		}
		BeanWrapper entityBean = new BeanWrapperImpl(entityClass);		
		PropertyDescriptor[] pds = entityBean.getPropertyDescriptors();
		
		for (PropertyDescriptor propertyDescriptor : pds) {
			String propertyName = propertyDescriptor.getName();
			if(!this.isEditable(entityBean, propertyName)) {
				continue;
			}
			Class<?> propertyType = propertyDescriptor.getPropertyType();	
			String propertyPath = this.buildPath(parentPathPrefix, propertyName);						
			TypeDescriptor typeDescriptor = entityBean.getPropertyTypeDescriptor(propertyName);
			
			if(this.isAssociation(entityBean, propertyName)) {
				if(this.isManyToOneAssociation(entityBean, propertyName) || this.isOneToOneAssociation(entityBean, propertyName)) {			
					context.defineColumn(targetMetamodel, propertyPath, propertyDescriptor, typeDescriptor, propertyType, RelationType.INNER);
				}else if(this.isOneToManyAssociation(entityBean, propertyName)){
					Class<?> parameterizedType = this.getElementTypeOfCollectionProperty(entityBean, propertyName);
					if(null != parameterizedType) {								
						context.defineColumn(targetMetamodel, propertyPath, propertyDescriptor, typeDescriptor, parameterizedType, RelationType.OUTER);
					}				
				}
			} else {
				boolean isIdColumn = this.isIdColumn(entityBean, propertyName);
				context.defineColumn(targetMetamodel, propertyPath, propertyDescriptor, typeDescriptor, isIdColumn);
			}
		}
	}
	
	private String buildPath(String parentPath, String propertyName) {
		String path = StringUtils.isEmpty(parentPath) ? propertyName : parentPath + "." + propertyName;
		return path;
	}
	
	private boolean isAssociation(BeanWrapper bean, String propertyName) {
		TypeDescriptor typeDescriptor = bean.getPropertyTypeDescriptor(propertyName);
		boolean mayBeAssociationColumn = (null == typeDescriptor.getAnnotation(Column.class) && null == typeDescriptor.getAnnotation(Id.class));
		return mayBeAssociationColumn;
	}
	
	private boolean isManyToOneAssociation(BeanWrapper bean, String propertyName) {
		TypeDescriptor typeDescriptor = bean.getPropertyTypeDescriptor(propertyName);
		ManyToOne manyToOneAnnotation = typeDescriptor.getAnnotation(ManyToOne.class);
		return (null != manyToOneAnnotation);
	}
	
	private boolean isOneToOneAssociation(BeanWrapper bean, String propertyName) {
		TypeDescriptor typeDescriptor = bean.getPropertyTypeDescriptor(propertyName);
		OneToOne oneToOneAnnotation = typeDescriptor.getAnnotation(OneToOne.class);
		return (null != oneToOneAnnotation);
	}
	
	private boolean isOneToManyAssociation(BeanWrapper bean, String propertyName) {
		TypeDescriptor typeDescriptor = bean.getPropertyTypeDescriptor(propertyName);
		OneToMany oneToManyAnnotation = typeDescriptor.getAnnotation(OneToMany.class);
		return this.isCollection(bean, propertyName) && (null != oneToManyAnnotation);
	}
	
	private boolean isCollection(BeanWrapper bean, String propertyName) {
		TypeDescriptor typeDescriptor = bean.getPropertyTypeDescriptor(propertyName);
		return typeDescriptor.isCollection();
	}	
	
	private boolean isIdColumn(BeanWrapper bean, String propertyName) {
		TypeDescriptor typeDescriptor = bean.getPropertyTypeDescriptor(propertyName);
		return (null != typeDescriptor.getAnnotation(Id.class));
	}
	
	private boolean isEditable(BeanWrapper bean, String propertyName) {
		return (bean.isReadableProperty(propertyName) || bean.isWritableProperty(propertyName));
	}
	
	private Class<?> getElementTypeOfCollectionProperty(BeanWrapper bean, String propertyName) {
		TypeDescriptor typeDescriptor = bean.getPropertyTypeDescriptor(propertyName);
		TypeDescriptor elementTypeDescriptor = typeDescriptor.getElementTypeDescriptor();
		if(null == elementTypeDescriptor) {
			return null;
		}
		Class<?> parameterizedType = elementTypeDescriptor.getType();
		return parameterizedType;
	}

	@Override
	public String toString() {
		return "DynamicMetaModelBuilder [entityClass=" + entityClass + ", \r\ncontext=" + context + "]";
	}
}
