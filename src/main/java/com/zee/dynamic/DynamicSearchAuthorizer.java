package com.zee.dynamic;

import org.springframework.data.domain.Page;

import com.zee.dynamic.model.DynamicAuthorizableSearchRequest;
import com.zee.dynamic.model.DynamicAuthorizedSearchResponse;

public interface DynamicSearchAuthorizer<E, A> {
	DynamicAuthorizedSearchResponse<E, A> authorize(DynamicAuthorizableSearchRequest request, Page<E> page);
}
