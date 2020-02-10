package com.zee.dynamic.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DynamicAuthorizedSearchResponse<E, A> {
	private String qualifier;
	private List<E> content;
	private Map<Long, A> authMap;
	
	@JsonIgnore
	private Page<E> page;
		
	public DynamicAuthorizedSearchResponse(String qualifier) {
		super();
		this.qualifier = qualifier;
		this.content = new ArrayList<E>();
		this.authMap = new HashMap<Long, A>();
	}
	
	public String getQualifier() {
		return qualifier;
	}
		
	public Page<E> getPage() {
		return this.page;
	}
	
	public void setPage(Page<E> page) {
		this.page = page;
	}

	public void setQualifier(String qualifier) {
		this.qualifier = qualifier;
	}

	public List<E> getContent() {
		return content;
	}
	
	public void setContent(List<E> content) {
		this.content = content;
	}
	
	public Map<Long, A> getAuthMap() {
		return authMap;
	}
	
	public void setAuthMap(Map<Long, A> authMap) {
		this.authMap = authMap;
	}
	
	public void addAuthorizedEntity(Long entityId, E entity, A auth) {
		if (null == entityId || null == entity || entityId.longValue() <= 0) {
			return;
		}
		this.content.add(entity);
		
		if(null != auth) {
			this.authMap.put(entityId, auth);
		}
	}
}
