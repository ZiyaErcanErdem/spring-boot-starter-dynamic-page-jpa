package com.zee.dynamic.builder;

import java.util.ArrayList;
import java.util.List;

import com.zee.dynamic.DynamicPageConfig;
import com.zee.dynamic.model.PageMetamodel;
import com.zee.dynamic.model.RelationType;

public class AssociationTask<T> {
	
	DynamicPageConfig config;
	private List<ColumnDescriptor<?>> describers;
	private Class<T> propertyType;
	private String qualifier;
	private int minLevel = Integer.MAX_VALUE;
	private int maxLevel = 0;
	private TaskState state;
	private ColumnDescriptor<?> minDescriber;
			
	private PageMetamodel<?> association;
	
	public AssociationTask(DynamicPageConfig config, Class<T> propertyType) {
		this.config = config;
		this.state = TaskState.PENDING;
		this.describers = new ArrayList<ColumnDescriptor<?>>();
		this.propertyType = propertyType;
		this.qualifier = this.propertyType.getSimpleName();
	}
	
	public Class<T> getPropertyType() {
		return this.propertyType;
	}
	
	public String getQualifier() {
		return this.qualifier;
	}
	
	public TaskState getState() {
		return this.state;
	}
	
	public boolean isActive() {
		return (TaskState.PENDING == this.state);
	}
	
	public int getMinLevel() {
		return this.minLevel;
	}
	
	public PageMetamodel<?> getAssociation() {
		return this.association;
	}
	
	public ColumnDescriptor<?> getMinDescriber() {
		return this.minDescriber;
	}
	
	public void addDescriber(ColumnDescriptor<?> describer) {
		if(!this.isActive()) {
			describer.complete(this);
			return;
		}
		this.describers.add(describer);
		int descLevel = describer.getLevel();		
		if(descLevel < this.minLevel && !this.isMaxDeepDescriber(describer)) {
			this.minLevel = describer.getLevel();
			this.minDescriber = describer;
		} else if(descLevel == this.minLevel) {
			if(this.isPreferredDescriber(describer)) {
				this.minLevel = describer.getLevel();
				this.minDescriber = describer;
			}
		} else if(descLevel > this.minLevel) {
			if(this.isPreferredDescriber(describer)) {
				this.minLevel = describer.getLevel();
				this.minDescriber = describer;
			}
		}
		if(describer.getLevel() > this.maxLevel) {
			this.maxLevel = describer.getLevel();
		}
	}
	
	private boolean isMaxDeepDescriber(ColumnDescriptor<?> describer) {
		if(describer.isAssociativeDescriber()) {
			//String associationQualifier = describer.getAssociationQualifier();
			String qualifier = describer.getQualifier();
			if(RelationType.OUTER == describer.getParentRelationType() && this.config.isMaxDeepForOuterJoin(qualifier)) {
				return true;
			} else if(RelationType.INNER == describer.getParentRelationType() && this.config.isMaxDeepForInnerJoin(qualifier)) {
				return true;
			}				
		}
		return false;
	}
	
	private boolean isPreferredDescriber(ColumnDescriptor<?> describer) {
		if(null == this.minDescriber) {
			return true;
		}
		
		if(this.isMaxDeepDescriber(describer)) {
			return false;
		}
		
		int descLevel = describer.getLevel();
		int descOuterJoinCountToTop = describer.getOuterJoinCountToTop();
		int currentLevel = this.minDescriber.getLevel();
		String descPropertyPath = describer.getPropertyPath();
		String currentPropertyPath = this.minDescriber.getPropertyPath();
		boolean descHasLessOuterJoins = descOuterJoinCountToTop < this.minDescriber.getOuterJoinCountToTop();
		boolean descHasSameOuterJoins = descOuterJoinCountToTop == this.minDescriber.getOuterJoinCountToTop();
		
		if(descLevel > currentLevel && descHasLessOuterJoins) {
			if(this.minDescriber.getAssociationRelationType() == RelationType.OUTER) {
				if(describer.getAssociationRelationType() != RelationType.OUTER) {
					// Prefer inner join paths
					if(!descPropertyPath.startsWith(currentPropertyPath) && currentLevel > 1) {
						// (control: ensure that new describer should not be on the same path)
						return true;
					}
				}
			} 
			if(this.minDescriber.getParentRelationType() == RelationType.OUTER) {
				if(describer.getParentRelationType() != RelationType.OUTER) {
					// Prefer inner join paths 
					if(!descPropertyPath.startsWith(currentPropertyPath) && currentLevel > 1) {
						// (control: ensure that new describer should not be on the same path)
						return true;
					}						
				}					
			}
		} else {
			if(describer.getAssociationRelationType() != RelationType.OUTER) {
				if(this.minDescriber.getAssociationRelationType() == RelationType.OUTER) {
					return true;
				}
			} else if(describer.getAssociationRelationType() == this.minDescriber.getAssociationRelationType()) {
				if(describer.getParentRelationType() != RelationType.OUTER) {
					if(this.minDescriber.getParentRelationType() == RelationType.OUTER) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public void complete(PageMetamodel<?> association) {
		this.association = association;
		this.state = TaskState.COMPLETED;
		this.describers.stream().forEach(describer -> {
			describer.complete(this);
		});			
	}

	@Override
	public String toString() {
		return "\r\nAssociationTask [qualifier=" + qualifier + ", state=" + state + ", minLevel=" + minLevel
				+ ", maxLevel=" + maxLevel + ", propertyType=" + propertyType + ", minDescriber=" + minDescriber
				+ "]";
	}
}