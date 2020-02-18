package com.zee.dynamic;

import java.util.ArrayList;
import java.util.List;

public class DynamicPageConfig {
	private String entityBeanPrefix;
	private int maxAssociationLevel;
	private List<String> maxDeepsForOuterJoin;
	private List<String> maxDeepsForInnerJoin;

	
	public DynamicPageConfig() {
		this.maxDeepsForOuterJoin = new ArrayList<>();
		this.maxDeepsForInnerJoin = new ArrayList<>();
		this.maxAssociationLevel = 2;
	}
	
	public String getEntityBeanPrefix() {
		return entityBeanPrefix;
	}

	public void setEntityBeanPrefix(String entityBeanPrefix) {
		this.entityBeanPrefix = entityBeanPrefix;
	}

	public int getMaxAssociationLevel() {
		return maxAssociationLevel;
	}

	public void setMaxAssociationLevel(int maxLevel) {
		this.maxAssociationLevel = maxLevel;
	}

	public void defineMaxDeepForOuterJoin(String entityName) {
		if(!this.isMaxDeepForOuterJoin(entityName)) {
			this.maxDeepsForOuterJoin.add(entityName);
		}		
	}
	
	public boolean isMaxDeepForOuterJoin(String entityName) {
		if(null == entityName || entityName.trim().isEmpty()) {
			return true;
		}
		return this.maxDeepsForOuterJoin.contains(entityName);
	}
	
	public void defineMaxDeepForInnerJoin(String entityName) {
		if(!this.isMaxDeepForInnerJoin(entityName)) {
			this.maxDeepsForOuterJoin.add(entityName);
		}		
	}
	
	public boolean isMaxDeepForInnerJoin(String entityName) {
		if(null == entityName || entityName.trim().isEmpty()) {
			return true;
		}
		return this.maxDeepsForInnerJoin.contains(entityName);
	}
}
