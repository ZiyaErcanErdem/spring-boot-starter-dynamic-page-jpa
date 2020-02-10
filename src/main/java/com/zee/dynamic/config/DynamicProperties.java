package com.zee.dynamic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zee.dynamic")
public class DynamicProperties {
	private String jpaEntityPackageName;
	private String jpaRepositoryPackageName;
	
	public String getJpaEntityPackageName() {
		return jpaEntityPackageName;
	}
	public void setJpaEntityPackageName(String jpaEntityPackageName) {
		this.jpaEntityPackageName = jpaEntityPackageName;
	}
	public String getJpaRepositoryPackageName() {
		return jpaRepositoryPackageName;
	}
	public void setJpaRepositoryPackageName(String jpaRepositoryPackageName) {
		this.jpaRepositoryPackageName = jpaRepositoryPackageName;
	}
}
