package com.zee.dynamic.builder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.RSQLOperators;

public enum ComparisonOperatorProxy {
	EQUAL(RSQLOperators.EQUAL),
	NOT_EQUAL(RSQLOperators.NOT_EQUAL),
	GREATER_THAN(RSQLOperators.GREATER_THAN),
	GREATER_THAN_OR_EQUAL(RSQLOperators.GREATER_THAN_OR_EQUAL),
	LESS_THAN(RSQLOperators.LESS_THAN),
	LESS_THAN_OR_EQUAL(RSQLOperators.LESS_THAN_OR_EQUAL),
	IN(RSQLOperators.IN),
	NOT_IN(RSQLOperators.NOT_IN);

	private ComparisonOperator operator;
	
	private final static Map<ComparisonOperator, ComparisonOperatorProxy> CACHE = 
			Collections.synchronizedMap(new HashMap<ComparisonOperator, ComparisonOperatorProxy>());
	
	static {
		for (ComparisonOperatorProxy proxy : values()) {
			CACHE.put(proxy.getOperator(), proxy);
		}
	}

    private ComparisonOperatorProxy(ComparisonOperator operator) {
    	this.operator = operator;
    }

    public ComparisonOperator getOperator() {
    	return this.operator;
    }

    public static ComparisonOperatorProxy asEnum(ComparisonOperator operator) {
    	return CACHE.get(operator);
    }
}