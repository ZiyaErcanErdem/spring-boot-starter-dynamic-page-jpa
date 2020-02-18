package com.zee.dynamic.builder;

import java.beans.PropertyDescriptor;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.util.StringUtils;

import com.zee.dynamic.QueryArgumentParser;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;

public class GenericRsqlExample<T>{
	
	private static final Logger log = LoggerFactory.getLogger(GenericRsqlExample.class);
	private QueryArgumentParser parser;
    private ComparisonOperator operator;
    private List<String> comparisonArguments;
    private String selector;
    private BeanWrapper beanwrapper;
    
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
        
	public GenericRsqlExample(){
		super();
	}
	
	public GenericRsqlExample(BeanWrapper beanwrapper,  String selector, ComparisonOperator operator, List<String> arguments, QueryArgumentParser parser) {
        super();
        this.beanwrapper = beanwrapper;
        this.operator = operator;
        this.selector = selector;
        this.comparisonArguments = arguments;        
        this.parser = parser;     
    }


	@SuppressWarnings({ "rawtypes", "unchecked" })

    public ExampleMatcher toExampleMatcher(ExampleMatcher matcher) {   
        
		if(null == matcher) {
			return null;
		}
		String propertyPath = this.selector;
		Class<?> propertyType = this.getPropertyType(this.selector);
        if(null == propertyType) {
        	return null;
        }
        
        log.debug("Cast all arguments to type {}.", propertyType.getName());
    	List<?> arguments = parser.parse(this.comparisonArguments, propertyType);
    	
 
        log.debug("Creating predicate: propertyPath {} {}", new Object[]{operator, arguments});

    	if (ComparisonOperatorProxy.asEnum(operator) != null) {
    		switch (ComparisonOperatorProxy.asEnum(operator)) {
	    		case EQUAL : {
	    			Object argument = arguments.get(0);
	    			if (argument instanceof String) {
	    				return createLike(propertyPath, (String) argument, matcher);
	    			} else if (isNullArgument(argument)) {
	    				return createIsNull(propertyPath, matcher);
	    			} else {
	    				return createEqual(propertyPath, argument, matcher);
	    			}
	    		}
	    		case NOT_EQUAL : {
	    			Object argument = arguments.get(0);
	    			if (argument instanceof String) {
	    				return createNotLike(propertyPath, (String) argument, matcher);
	    			} else if (isNullArgument(argument)) {
	    				return createIsNotNull(propertyPath, matcher);
	    			} else {
	    				return createNotEqual(propertyPath, argument, matcher);
	    			}
	    		}
	    		case GREATER_THAN : {
	    			Object argument = arguments.get(0);
                    if (argument instanceof Date) {
                        int days = 1;
                        return createBetweenThan(propertyPath, modifyDate(argument, days), END_DATE, matcher);
                    } else if (argument instanceof Number || argument == null) {
                        return createGreaterThan(propertyPath, (Number) argument, matcher);
                    } else if (argument instanceof Comparable) {
                        return createGreaterThanComparable(propertyPath, (Comparable) argument, matcher);
                    } else {
                        throw new IllegalArgumentException(buildNotComparableMessage(operator, argument));
                    }
                }
	    		case GREATER_THAN_OR_EQUAL : {
	    			Object argument = arguments.get(0);
                    if (argument instanceof Date){
                        return createBetweenThan(propertyPath, (Date)argument, END_DATE, matcher);
                    } else if (argument instanceof Number || argument == null) {
                    	return createGreaterEqual(propertyPath, (Number)argument, matcher);
                    } else if (argument instanceof Comparable) {
                    	return createGreaterEqualComparable(propertyPath, (Comparable) argument, matcher);
                    } else {
                        throw new IllegalArgumentException(buildNotComparableMessage(operator, argument));
                    }
	    		}
	    		case LESS_THAN : {
	    			Object argument = arguments.get(0);
                    if (argument instanceof Date) {
                        int days = -1;
                        return createBetweenThan(propertyPath, START_DATE, modifyDate(argument, days), matcher);
                    } else if (argument instanceof Number || argument == null) {
                    	return createLessThan(propertyPath, (Number) argument, matcher);
                    } else if (argument instanceof Comparable) {
                    	return createLessThanComparable(propertyPath, (Comparable) argument, matcher);
                    } else {
                        throw new IllegalArgumentException(buildNotComparableMessage(operator, argument));
                    }
                }
	    		case LESS_THAN_OR_EQUAL : {
	    			Object argument = arguments.get(0);
                    if (argument instanceof Date){
                    	return createBetweenThan(propertyPath,START_DATE, (Date)argument, matcher);
                    } else if (argument instanceof Number || argument == null) {
                    	return createLessEqual(propertyPath, (Number)argument, matcher);
                    } else if (argument instanceof Comparable) {
                    	return createLessEqualComparable(propertyPath, (Comparable) argument, matcher);
                    } else {
                        throw new IllegalArgumentException(buildNotComparableMessage(operator, argument));
                    }
                }
	    		case IN : return createIn(propertyPath, arguments, matcher);
	    		case NOT_IN : return createNotIn(propertyPath, arguments, matcher);
    		}
    	}
        throw new IllegalArgumentException("Unknown operator: " + operator);
    }
	
	private boolean isCollection(Class<?> propType) {
		  return Collection.class.isAssignableFrom(propType) || Map.class.isAssignableFrom(propType);
	}
	
	private Class<?> getPropertyType(String selectorPath) {
		if(selectorPath.contains(".")) {		
			String[] graph = selectorPath.split("\\.");
			String currentSelector = "";
			for(String part : graph) {
				currentSelector = currentSelector.isEmpty() ? part : currentSelector + "." + part;
				PropertyDescriptor pd = this.beanwrapper.getPropertyDescriptor(currentSelector);
				Class<?>  nestedPropType = pd.getPropertyType();
				if ((nestedPropType != null) && isCollection(nestedPropType)) {
					return null;
				}
			}
			
		}		
        PropertyDescriptor pd = this.beanwrapper.getPropertyDescriptor(selectorPath);
        return pd.getPropertyType();		
	}
	
    private boolean isNullArgument(Object argument) {
        return argument == null;
    }
	
	private ExampleMatcher createBetweenThan(String propertyPath, Date start, Date end, ExampleMatcher matcher) {
       	return null;
    }

    @SuppressWarnings("static-access")
	private ExampleMatcher createLike(String propertyPath, String argument, ExampleMatcher matcher) {
    	if(StringUtils.isEmpty(argument)){
    		return null;
    	}
    	boolean startsWith = false;
    	if(argument.endsWith("*") || argument.endsWith("%")) {
    		startsWith = true;
    	}
    	
    	boolean endsWith = false;
    	if(argument.startsWith("*") || argument.startsWith("%")) {
    		endsWith = true;
    	}
    	
        String like = argument.replaceAll("\\"+LIKE_WILDCARD, "");
        this.beanwrapper.setPropertyValue(propertyPath, like);
        if((startsWith && endsWith) || (!startsWith && !endsWith)) {
        	 return matcher
              		.withMatcher(propertyPath, match -> match.contains().ignoreCase());
        } else if(startsWith && !endsWith) {
       	 	return matcher
          		.withMatcher(propertyPath, match -> match.startsWith().ignoreCase());
        } else if(!startsWith && endsWith) {
       	 	return matcher
              		.withMatcher(propertyPath, match -> match.endsWith().ignoreCase());
        } else {
        	return createEqual(propertyPath, argument, matcher);
        }
    }

    private ExampleMatcher createIsNull(String propertyPath, ExampleMatcher matcher) {
    	return null;
    }

    @SuppressWarnings("static-access")
	private ExampleMatcher createEqual(String propertyPath, Object argument, ExampleMatcher matcher) {
    	this.beanwrapper.setPropertyValue(propertyPath, argument);
        return matcher
        		.withMatcher(propertyPath, match -> match.ignoreCase());
    }

    private ExampleMatcher createNotEqual(String propertyPath, Object argument, ExampleMatcher matcher) {
        return null;
    }

    private ExampleMatcher createNotLike(String propertyPath, String argument, ExampleMatcher matcher) {
         return null;
    }

    private ExampleMatcher createIsNotNull(String propertyPath, ExampleMatcher matcher) {
        return null;
    }

    private ExampleMatcher createGreaterThan(String propertyPath, Number argument, ExampleMatcher matcher) {
        return null;
    }

    private <Y extends Comparable<? super Y>> ExampleMatcher createGreaterThanComparable(String propertyPath, Y argument, ExampleMatcher matcher) {
        return null;
    }

    private ExampleMatcher createGreaterEqual(String propertyPath, Number argument, ExampleMatcher matcher) {
        return null;
    }

    private <Y extends Comparable<? super Y>> ExampleMatcher createGreaterEqualComparable(String propertyPath, Y argument, ExampleMatcher matcher) {
        return null;
    }

    private ExampleMatcher createLessThan(String propertyPath, Number argument, ExampleMatcher matcher) {
        return null;
    }

    private <Y extends Comparable<? super Y>> ExampleMatcher createLessThanComparable(String propertyPath, Y argument, ExampleMatcher matcher) {
        return null;
    }

    private ExampleMatcher createLessEqual(String propertyPath, Number argument, ExampleMatcher matcher) {
        return null;
    }

    private <Y extends Comparable<? super Y>> ExampleMatcher createLessEqualComparable(String propertyPath, Y argument, ExampleMatcher matcher) {
        return null;
    }

    private ExampleMatcher createIn(String propertyPath, List<?> arguments, ExampleMatcher matcher) {
    	return null;
    }

    private ExampleMatcher createNotIn(String propertyPath, List<?> arguments, ExampleMatcher matcher) {
    	return null;
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