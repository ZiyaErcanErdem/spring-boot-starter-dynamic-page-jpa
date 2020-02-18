package com.zee.dynamic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zee.dynamic")
public class DynamicProperties {
	private String jpaEntityPackageName;
	private int maxAssociationLevel;
	
	public String getJpaEntityPackageName() {
		return jpaEntityPackageName;
	}
	public void setJpaEntityPackageName(String jpaEntityPackageName) {
		this.jpaEntityPackageName = jpaEntityPackageName;
	}
	public int getMaxAssociationLevel() {
		return maxAssociationLevel;
	}
	public void setMaxAssociationLevel(int maxAssociationLevel) {
		this.maxAssociationLevel = maxAssociationLevel;
	}

}
