package com.zee.dynamic.builder;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Metamodel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;

import com.zee.dynamic.QueryArgumentParser;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;

public class GenericRsqlSpecification<T> implements Specification<T>{
	private static final long serialVersionUID = 2324093759834490464L;
	
	private static final Logger log = LoggerFactory.getLogger(GenericRsqlSpecification.class);
	
	private QueryArgumentParser parser;
    private ComparisonOperator operator;
    private List<String> comparisonArguments;
    private String selector;
    private Metamodel metaModel;
    private boolean distinct = false;
    
    public static final Character LIKE_WILDCARD = '*';

    private static final Date START_DATE;
    private static final Date END_DATE;

    static {
        Calendar cal = Calendar.getInstance();
        cal.set( 9999, Calendar.DECEMBER, 31);
        END_DATE = cal.getTime();
        cal.set( 5, Calendar.JANUARY, 1);
        START_DATE = cal.getTime();
    }
        
	public GenericRsqlSpecification(){
		super();
	}
	
	public GenericRsqlSpecification(Metamodel metaModel,  
									String selector, 
									ComparisonOperator operator, 
									List<String> arguments, 
									QueryArgumentParser parser) {
        super();
        this.metaModel = metaModel;
        this.operator = operator;
        this.selector = selector;
        this.comparisonArguments = arguments;        
        this.parser = parser;  
    }

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {   
        Expression propertyPath = findPropertyPath(this.selector, root, builder);
        
        log.info("Cast all arguments to type {}.", propertyPath.getJavaType().getName());
    	List<Object> arguments = parser.parse(this.comparisonArguments, propertyPath.getJavaType());
    	
    	if(this.distinct) {
    		query.distinct(true);
    		log.info("Query will be executed as distinct due to outer join path expression => {}", propertyPath.getJavaType().getName());
    	}
    	
 
        log.info("Creating predicate: propertyPath {} {}", operator, arguments);

    	if (ComparisonOperatorProxy.asEnum(operator) != null) {
    		switch (ComparisonOperatorProxy.asEnum(operator)) {
	    		case EQUAL : {
	    			Object argument = arguments.get(0);
	    			if (argument instanceof String) {
	    				return createLike(propertyPath, (String) argument, builder);
	    			} else if (isNullArgument(argument)) {
	    				return createIsNull(propertyPath, builder);
	    			} else {
	    				return createEqual(propertyPath, argument, builder);
	    			}
	    		}
	    		case NOT_EQUAL : {
	    			Object argument = arguments.get(0);
	    			if (argument instanceof String) {
	    				return createNotLike(propertyPath, (String) argument, builder);
	    			} else if (isNullArgument(argument)) {
	    				return createIsNotNull(propertyPath, builder);
	    			} else {
	    				return createNotEqual(propertyPath, argument, builder);
	    			}
	    		}
	    		case GREATER_THAN : {
	    			Object argument = arguments.get(0);
                    Predicate predicate;
                    if (argument instanceof Date) {
                        int days = 1;
                        predicate = createBetweenThan(propertyPath, modifyDate(argument, days), END_DATE, builder);
                    } else if (argument instanceof Number || argument == null) {
                        predicate = createGreaterThan(propertyPath, (Number) argument, builder);
                    } else if (argument instanceof Comparable) {
                        predicate = createGreaterThanComparable(propertyPath, (Comparable) argument, builder);
                    } else {
                        throw new IllegalArgumentException(buildNotComparableMessage(operator, argument));
                    }
                    return predicate;
                }
	    		case GREATER_THAN_OR_EQUAL : {
	    			Object argument = arguments.get(0);
                    Predicate predicate;
                    if (argument instanceof Date){
                        predicate = createBetweenThan(propertyPath, (Date)argument, END_DATE, builder);
                    } else if (argument instanceof Number || argument == null) {
                        predicate = createGreaterEqual(propertyPath, (Number)argument, builder);
                    } else if (argument instanceof Comparable) {
                        predicate = createGreaterEqualComparable(propertyPath, (Comparable) argument, builder);
                    } else {
                        throw new IllegalArgumentException(buildNotComparableMessage(operator, argument));
                    }
                    return predicate;

	    		}
	    		case LESS_THAN : {
	    			Object argument = arguments.get(0);
                    Predicate predicate;
                    if (argument instanceof Date) {
                        int days = -1;
                        predicate = createBetweenThan(propertyPath, START_DATE, modifyDate(argument, days), builder);
                    } else if (argument instanceof Number || argument == null) {
                        predicate = createLessThan(propertyPath, (Number) argument, builder);
                    } else if (argument instanceof Comparable) {
                        predicate = createLessThanComparable(propertyPath, (Comparable) argument, builder);
                    } else {
                        throw new IllegalArgumentException(buildNotComparableMessage(operator, argument));
                    }
                    return predicate;
                }
	    		case LESS_THAN_OR_EQUAL : {
	    			Object argument = arguments.get(0);

                    Predicate predicate;
                    if (argument instanceof Date){
                        	predicate = createBetweenThan(propertyPath,START_DATE, (Date)argument, builder);
                    } else if (argument instanceof Number || argument == null) {
                        predicate = createLessEqual(propertyPath, (Number)argument, builder);
                    } else if (argument instanceof Comparable) {
                        predicate = createLessEqualComparable(propertyPath, (Comparable) argument, builder);
                    } else {
                        throw new IllegalArgumentException(buildNotComparableMessage(operator, argument));
                    }
                    return predicate;
                }
	    		case IN : return createIn(propertyPath, arguments, builder);
	    		case NOT_IN : return createNotIn(propertyPath, arguments, builder);
    		}
    	}
        throw new IllegalArgumentException("Unknown operator: " + operator);
    }
		
	public Path<?> findPropertyPath(String propertyPath, Path<?> startRoot, CriteriaBuilder builder) {
    	GenericRsqlPathIterator pathIterator = new GenericRsqlPathIterator(this.metaModel, startRoot, propertyPath);        
        Path<?> currentPath = startRoot;        
        while (pathIterator.hasNext()) {
        	currentPath = pathIterator.next();
        }   
        this.distinct = pathIterator.isDistinct();
        return currentPath;
    }
    
    private boolean isNullArgument(Object argument) {
        return argument == null;
    }
	
    @SuppressWarnings({ "rawtypes", "unchecked" })
	private Predicate createBetweenThan(Expression propertyPath, Date start, Date end, CriteriaBuilder builder) {
       	return builder.between(propertyPath, start, end);
    }

    private Predicate createLike(Expression<String> propertyPath, String argument, CriteriaBuilder builder) {
        String like = argument.replace(LIKE_WILDCARD, '%');
        return builder.like(builder.lower(propertyPath), like.toLowerCase());
    }

    private Predicate createIsNull(Expression<?> propertyPath, CriteriaBuilder builder) {
    	return builder.isNull(propertyPath);
    }

    private Predicate createEqual(Expression<?> propertyPath, Object argument, CriteriaBuilder builder) {
    	return builder.equal(propertyPath, argument);
    }

    private Predicate createNotEqual(Expression<?> propertyPath, Object argument, CriteriaBuilder builder) {
        return builder.notEqual(propertyPath, argument);
    }

    private Predicate createNotLike(Expression<String> propertyPath, String argument, CriteriaBuilder builder) {
         return builder.not(createLike(propertyPath, argument, builder));
    }

    private Predicate createIsNotNull(Expression<?> propertyPath, CriteriaBuilder builder) {
        return builder.isNotNull(propertyPath);
    }

    private Predicate createGreaterThan(Expression<? extends Number> propertyPath, Number argument, CriteriaBuilder builder) {
        return builder.gt(propertyPath, argument);
    }

    private <Y extends Comparable<? super Y>> Predicate createGreaterThanComparable(Expression<? extends Y> propertyPath, Y argument, CriteriaBuilder builder) {
        return builder.greaterThan(propertyPath, argument);
    }

    private Predicate createGreaterEqual(Expression<? extends Number> propertyPath, Number argument, CriteriaBuilder builder) {
        return builder.ge(propertyPath, argument);
    }

    private <Y extends Comparable<? super Y>> Predicate createGreaterEqualComparable(Expression<? extends Y> propertyPath, Y argument, CriteriaBuilder builder) {
        return builder.greaterThanOrEqualTo(propertyPath, argument);
    }

    private Predicate createLessThan(Expression<? extends Number> propertyPath, Number argument, CriteriaBuilder builder) {
        return builder.lt(propertyPath, argument);
    }

    private <Y extends Comparable<? super Y>> Predicate createLessThanComparable(Expression<? extends Y> propertyPath, Y argument, CriteriaBuilder builder) {
        return builder.lessThan(propertyPath, argument);
    }

    private Predicate createLessEqual(Expression<? extends Number> propertyPath, Number argument, CriteriaBuilder builder) {
        return builder.le(propertyPath, argument);
    }

    private <Y extends Comparable<? super Y>> Predicate createLessEqualComparable(Expression<? extends Y> propertyPath, Y argument, CriteriaBuilder builder) {
        return builder.lessThanOrEqualTo(propertyPath, argument);
    }

    private Predicate createIn(Expression<?> propertyPath, List<?> arguments, CriteriaBuilder builder) {
    	return propertyPath.in(arguments);
    }

    private Predicate createNotIn(Expression<?> propertyPath, List<?> arguments, CriteriaBuilder builder) {
    	return builder.not(createIn(propertyPath,arguments, builder));
    }
    
    private Date modifyDate(Object argument, int days) {
        Date date = (Date) argument;
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, days);
        date = c.getTime();
        return date;
    }

    private String buildNotComparableMessage(ComparisonOperator operator, Object argument) {
        return String.format("Invalid type for comparison operator: %s type: %s must implement Comparable<%s>",
                operator,
                argument.getClass().getName(),
                argument.getClass().getSimpleName());
    }    
}