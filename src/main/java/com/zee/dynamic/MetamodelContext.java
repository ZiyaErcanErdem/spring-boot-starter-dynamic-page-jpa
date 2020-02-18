package com.zee.dynamic;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.core.convert.TypeDescriptor;

import com.zee.dynamic.builder.AssociationTask;
import com.zee.dynamic.builder.ColumnDescriptor;
import com.zee.dynamic.model.PageMetamodel;
import com.zee.dynamic.model.RelationType;

public class MetamodelContext<T> {
	
	private DynamicPageConfig config;
	private PageMetamodel<T> topMetaModel;
	private List<AssociationTask<?>> tasks;

	public MetamodelContext(DynamicPageConfig config, PageMetamodel<T> top) { 
		this.config = config;
		this.tasks = new ArrayList<AssociationTask<?>>();
		this.topMetaModel = top;
	}
	
	public PageMetamodel<T> getMetamodel(){
		return this.topMetaModel;
	}
	
	public boolean hasActiveTasks() {
		return this.tasks.stream().anyMatch(t -> t.isActive());
	}
	
	public AssociationTask<?> getNextActiveTask(){
		AssociationTask<?> task = null;
		Optional<AssociationTask<?>> result = this.tasks.stream().filter(t -> t.isActive()).min(Comparator.comparing(AssociationTask::getMinLevel));
		if(result.isPresent()) {
			task = result.get();
		}
		return task;
	}
	
	private AssociationTask<?> getOrAddTask(Class<?> associationType) {
		String qualifier = associationType.getSimpleName();
		if(this.topMetaModel.getQualifier().equals(qualifier)) {
			return null;
		}
		AssociationTask<?> task = null;

		Optional<AssociationTask<?>> result = this.tasks.stream().filter(t -> t.getQualifier().equals(qualifier)).findFirst();
		if(result.isPresent()) {
			task = result.get();
		}else {
			task =  new AssociationTask<>(this.config, associationType);
			this.tasks.add(task);
		}
		return task;
	}
	
	/*
	public <E, A> void defineColumn(PageMetamodel<E> container, String propertyPath, PropertyDescriptor propertyDescriptor, TypeDescriptor typeDescriptor) {
		this.defineColumn(container, propertyPath, propertyDescriptor, typeDescriptor, false, null, null);		
	}
	*/
	
	public <E, A> void defineColumn(PageMetamodel<E> container, String propertyPath, PropertyDescriptor propertyDescriptor, TypeDescriptor typeDescriptor, boolean isIdColumn) {
		this.defineColumn(container, propertyPath, propertyDescriptor, typeDescriptor, isIdColumn, null, null);		
	}
		
	public <E, A> void defineColumn(PageMetamodel<E> container, String propertyPath, PropertyDescriptor propertyDescriptor, TypeDescriptor typeDescriptor, boolean isIdColumn, Class<A> associationType, RelationType associationRelationType) {
		ColumnDescriptor<E> descriptor = new ColumnDescriptor<E>(container, propertyPath, propertyDescriptor, typeDescriptor, isIdColumn, associationType, associationRelationType);
		if(descriptor.isAssociativeDescriber()) {
			AssociationTask<?> task = this.getOrAddTask(associationType);
			if(null != task && descriptor.getLevel() < this.config.getMaxAssociationLevel()) {
				task.addDescriber(descriptor);
			}
		}
	}

	@Override
	public String toString() {
		return "MetaModelContext [tasks=" + tasks + "]";
	}

}
