package com.zee.dynamic.model;

import java.util.HashMap;
import java.util.Map;

public class DynamicAuthorizableSearchRequest {
	private String qualifier;
	private String query;
	private Map<String, String> authContext;
	private String provider;
	
	public DynamicAuthorizableSearchRequest() {
		super();
		this.authContext = new HashMap<String, String>();
	}
	
	public String getQualifier() {
		return qualifier;
	}
	public void setQualifier(String qualifier) {
		this.qualifier = qualifier;
	}
	public String getQuery() {
		return query;
	}
	public void setQuery(String query) {
		this.query = query;
	}
	public Map<String, String> getAuthContext() {
		return authContext;
	}
	public void setAuthContext(Map<String, String> authContext) {
		this.authContext = authContext;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	@Override
	public String toString() {
		return "DynamicAuthorizableSearchRequest [qualifier=" + qualifier + ", query=" + query + ", provider="
				+ provider + ", authContext=" + authContext + "]";
	}

}
