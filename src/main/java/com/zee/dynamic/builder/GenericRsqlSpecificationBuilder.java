package com.zee.dynamic.builder;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.metamodel.Metamodel;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;

import com.zee.dynamic.QueryArgumentParser;

import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.LogicalNode;
import cz.jirutka.rsql.parser.ast.LogicalOperator;
import cz.jirutka.rsql.parser.ast.Node;

public class GenericRsqlSpecificationBuilder<T> {
	
	// private static final Logger log = LoggerFactory.getLogger(GenericRsqlSpecificationBuilder.class);
	
	private QueryArgumentParser parser;
	
	public GenericRsqlSpecificationBuilder() {
		this.parser = new QueryArgumentParser(); 
	}

	public Specification<T> createSpecification(Node node, Metamodel metamodel) {
        if (node instanceof LogicalNode) {
            return createSpecification((LogicalNode) node, metamodel);
        }
        if (node instanceof ComparisonNode) {
            return createSpecification((ComparisonNode) node, metamodel);
        }
        return null;
    }
 
    public Specification<T> createSpecification(LogicalNode logicalNode, Metamodel metamodel) {
        List<Specification<T>> specs = new ArrayList<Specification<T>>();
        Specification<T> temp;
        for (Node node : logicalNode.getChildren()) {
            temp = createSpecification(node, metamodel);
            if (temp != null) {
                specs.add(temp);
            }
        }
 
        Specification<T> result = specs.get(0);
        if (logicalNode.getOperator() == LogicalOperator.AND) {
            for (int i = 1; i < specs.size(); i++) {
                result = Specification.where(result).and(specs.get(i));
            }
        } else if (logicalNode.getOperator() == LogicalOperator.OR) {
            for (int i = 1; i < specs.size(); i++) {
                result = Specification.where(result).or(specs.get(i));
            }
        }
 
        return result;
    }
 
    public Specification<T> createSpecification(ComparisonNode comparisonNode, Metamodel metamodel) {    	
    	String selector = comparisonNode.getSelector();
    	ComparisonOperator operator = comparisonNode.getOperator();
    	List<String> arguments = comparisonNode.getArguments();    	
    	Specification<T> spec = new GenericRsqlSpecification<T>(metamodel, selector, operator, arguments, this.parser);
    	Specification<T> result = Specification.where(spec);
        return result;
    }
}