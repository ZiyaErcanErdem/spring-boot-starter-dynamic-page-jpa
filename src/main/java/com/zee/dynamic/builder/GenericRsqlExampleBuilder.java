package com.zee.dynamic.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.data.domain.ExampleMatcher;

import com.zee.dynamic.QueryArgumentParser;

import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.LogicalNode;
import cz.jirutka.rsql.parser.ast.LogicalOperator;
import cz.jirutka.rsql.parser.ast.Node;

public class GenericRsqlExampleBuilder<T> {

	// private static final Logger log = LoggerFactory.getLogger(GenericRsqlExampleBuilder.class);

	private QueryArgumentParser parser;
	private BeanWrapper wrapper;
	private boolean hasAndOperator = false;
	private boolean hasOrOperator = false;
	private List<String> includes;

	public GenericRsqlExampleBuilder(BeanWrapper wrapper) {
		this.parser = new QueryArgumentParser();
		this.wrapper = wrapper;
		this.includes = new ArrayList<String>();
	}

	public ExampleMatcher createMatcher(Node node, ExampleMatcher matcher) {
		if(null == matcher) {
			return null;
		}
        if (node instanceof LogicalNode) {
            return createMatcher((LogicalNode) node, matcher);
        }
        if (node instanceof ComparisonNode) {
            return createMatcher((ComparisonNode) node, matcher);
        }
        return null;
    }

    @SuppressWarnings("static-access")
	public ExampleMatcher createMatcher(LogicalNode logicalNode, ExampleMatcher matcher) {
		if(null == matcher) {
			return null;
		}

        if (logicalNode.getOperator() == LogicalOperator.AND) {
            this.hasAndOperator = true;
            matcher = ExampleMatcher.matchingAll();
        } else if (logicalNode.getOperator() == LogicalOperator.OR) {
        	this.hasOrOperator = true;
        	matcher = ExampleMatcher.matchingAny();
        }

        if(this.hasAndOperator && this.hasOrOperator) {
        	return null;
        }

        for (Node node : logicalNode.getChildren()) {
        	matcher = createMatcher(node, matcher);
        }

		if(null == matcher) {
			return null;
		}

        String[] ignoredPaths = Arrays.asList(wrapper.getPropertyDescriptors())
				.stream().map(pe -> pe.getName())
				.filter(s -> !this.includes.contains(s))
				.toArray(size -> new String[size]);

        if(this.hasAndOperator || !this.hasOrOperator) {
        	//return matcher.matchingAny();
            return matcher.withIgnorePaths(ignoredPaths); // matchingAll();
        } else if(this.hasOrOperator) {
        	return matcher.withIgnorePaths(ignoredPaths); // matchingAny();
        }
        return matcher;
    }

    public ExampleMatcher createMatcher(ComparisonNode comparisonNode, ExampleMatcher matcher) {
    	String selector = comparisonNode.getSelector();
    	if(this.includes.contains(selector)) {
    		// Example query is not suitable for same selector with multiple constraint.
    		// Return null and use specification query builder.
    		return null;
    	}
    	this.includes.add(selector);
    	ComparisonOperator operator = comparisonNode.getOperator();
    	List<String> arguments = comparisonNode.getArguments();
    	GenericRsqlExample<T> example = new GenericRsqlExample<T>(wrapper, selector, operator, arguments, this.parser);
        return example.toExampleMatcher(matcher);
    }
}
