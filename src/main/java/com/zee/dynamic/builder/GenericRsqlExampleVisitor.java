package com.zee.dynamic.builder;

import org.springframework.beans.BeanWrapper;
import org.springframework.data.domain.ExampleMatcher;

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;

public class GenericRsqlExampleVisitor<T, E> implements RSQLVisitor<ExampleMatcher, BeanWrapper> {
	private GenericRsqlExampleBuilder<T> builder;
	private ExampleMatcher matcher;
	
	public GenericRsqlExampleVisitor() {
		super();
		this.matcher = ExampleMatcher.matchingAny().withIgnoreNullValues();
	}
	
	private GenericRsqlExampleBuilder<T> getBuilder(BeanWrapper wrapper) {
		if(null == this.builder) {
			this.builder = new GenericRsqlExampleBuilder<T>(wrapper);
			this.matcher = this.initializeMatcher(wrapper);
		}
		return this.builder;
	}
	
	private ExampleMatcher initializeMatcher(BeanWrapper wrapper) {
        ExampleMatcher m = ExampleMatcher.matchingAny().withIgnoreNullValues();	
		return m;
	}

	@Override
	public ExampleMatcher visit(AndNode node, BeanWrapper wrapper) {
		this.matcher = this.getBuilder(wrapper).createMatcher(node, this.matcher);
		return this.matcher;
	}

	@Override
	public ExampleMatcher visit(OrNode node, BeanWrapper wrapper) {
		this.matcher =  this.getBuilder(wrapper).createMatcher(node, this.matcher);
		return this.matcher;
	}

	@Override
	public ExampleMatcher visit(ComparisonNode node, BeanWrapper wrapper) {
		this.matcher =  this.getBuilder(wrapper).createMatcher(node, this.matcher);
		return this.matcher;
	}
}