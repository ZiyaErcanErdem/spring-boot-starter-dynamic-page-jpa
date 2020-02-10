package com.zee.dynamic.builder;

import javax.persistence.metamodel.Metamodel;
import org.springframework.data.jpa.domain.Specification;
import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;

public class GenericRsqlSpecificationVisitor<T, E> implements RSQLVisitor<Specification<T>, Metamodel> {
	private GenericRsqlSpecificationBuilder<T> builder;
	
	public GenericRsqlSpecificationVisitor() {
		super();
		this.builder = new GenericRsqlSpecificationBuilder<T>();
	}
	
	@Override
	public Specification<T> visit(AndNode node, Metamodel metamodel) {
		return builder.createSpecification(node, metamodel);
	}

	@Override
	public Specification<T> visit(OrNode node, Metamodel metamodel) {
		return builder.createSpecification(node, metamodel);
	}

	@Override
	public Specification<T> visit(ComparisonNode node, Metamodel metamodel) {
		return builder.createSpecification(node, metamodel);
	}
}