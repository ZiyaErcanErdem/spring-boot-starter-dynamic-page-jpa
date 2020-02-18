package com.zee.dynamic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryArgumentParser {

	private static final Logger log = LoggerFactory.getLogger(QueryArgumentParser.class);

    private static final String DATE_PATTERN = "yyyy-MM-dd"; //ISO 8601
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"; //ISO 8601

    private IllegalArgumentException formatException(String argument, Class<?> type) {
    	return new IllegalArgumentException("Cannot cast '" + argument + "' to type " + type);
    }
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> T parse(String argument, Class<T> type) throws IllegalArgumentException {

    	log.debug("Parsing argument ''{}'' as type {}", new Object[] {argument, type.getSimpleName()});

        if (argument == null || "null".equals(argument.trim().toLowerCase())) {
        	return (T) null;
        }

        try {
            if (type.equals(String.class)) return (T) argument;
            if (type.equals(Integer.class) || type.equals(int.class)) return (T) Integer.valueOf(argument);
            if (type.equals(Boolean.class) || type.equals(boolean.class)) return (T) Boolean.valueOf(argument);
            if (type.isEnum()) return (T) Enum.valueOf((Class<Enum>)type, argument);
            if (type.equals(Float.class)   || type.equals(float.class)) return (T) Float.valueOf(argument);
            if (type.equals(Double.class)  || type.equals(double.class)) return (T) Double.valueOf(argument);
            if (type.equals(Long.class)    || type.equals(long.class)) return (T) Long.valueOf(argument);
            if (type.equals(BigDecimal.class) ) return (T) new BigDecimal(argument);
        } catch (IllegalArgumentException ex) {
        	throw this.formatException(argument, type);
        }

        if (type.equals(Date.class)) {
            return (T) parseDate(argument, type);
        } else if (type.equals(Instant.class)) {
            return (T) parseInstant(argument, type);
        }

        try {
        	log.debug("Trying to get and invoke valueOf(String s) method on {}", type);
            Method method = type.getMethod("valueOf", String.class);
            return (T) method.invoke(type, argument);
        } catch (InvocationTargetException ex) {
        	throw this.formatException(argument, type);
        } catch (ReflectiveOperationException ex) {
        	log.warn("{} does not have method valueOf(String s) or method is inaccessible", type);
        	throw new IllegalArgumentException("Cannot parse argument type " + type);
        }
    }

    private <T> Date parseDate(String argument, Class<T> type) {
        try {
        	return new SimpleDateFormat(DATE_TIME_PATTERN).parse(argument);
        } catch (ParseException ex) {
        	log.debug("Not a date time format, lets try with date format.");
        }
        try {
        	return  new SimpleDateFormat(DATE_PATTERN).parse(argument);
        } catch (ParseException ex1) {
        	throw this.formatException(argument, type);
        }
    }
    
    private <T> Instant parseInstant(String argument, Class<T> type) {
    	Date date = parseDate(argument, type);
    	return (null == date ? null : date.toInstant());        
    }

	public <T> List<T> parse(List<String> arguments, Class<T> type) throws IllegalArgumentException {
    	List<T> castedArguments = new ArrayList<T>(arguments.size());
    	for (String argument : arguments) {
    		castedArguments.add(this.parse(argument, type));
    	}
		return castedArguments;
	}
}