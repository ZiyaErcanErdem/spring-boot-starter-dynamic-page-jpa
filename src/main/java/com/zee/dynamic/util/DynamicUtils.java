package com.zee.dynamic.util;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

public class DynamicUtils {
	
	public static String getShortName(Class<?> clazz) {
		return null == clazz ? null : ClassUtils.getShortName(clazz);
	}
	
	public static Class<?> classForName(String classFullName) {
		if(StringUtils.isEmpty(classFullName)){
			return null;
		}
		Class<?> cls = null;
		try {
			cls = ClassUtils.forName(classFullName, null);
		} catch (ClassNotFoundException e) {
		} catch (LinkageError e) {
		}
		return cls;
	}
	
	public static Class<?> classForName(String packageName, String className) {
		if(StringUtils.isEmpty(packageName) || StringUtils.isEmpty(className)){
			return null;
		}
		return classForName(toFullPath(packageName, className));
	}
	
	
	public static <T> T createBean(Class<T> clazz) {
		T bean = BeanUtils.instantiateClass(clazz);
		return bean;
	}
	
	public static <T> BeanWrapper wrap(Class<T> clazz) {
		if (null == clazz) {
			return null;
		}
		return wrap(createBean(clazz));
	}
	
	public static <T> BeanWrapper wrap(T bean) {
		if (null == bean) {
			return null;
		}
		BeanWrapper wrapper = new BeanWrapperImpl(bean);
		wrapper.setAutoGrowNestedPaths(true);
		return wrapper;
	}

	@SuppressWarnings("unchecked")
	public static <T> Class<T> castTo(Class<?> clazz) {
		Class<T> cls = null;
		try {
			cls = (Class<T>) clazz;
		} catch (Exception e) {}
		return cls;
	}
	
	public static <T> Class<T> castTo(String classFullName) {
		if(StringUtils.isEmpty(classFullName)){
			return null;
		}
		return castTo(classForName(classFullName));
	}
	
	public static <T> Class<T> castTo(String packageName, String className) {
		if(StringUtils.isEmpty(packageName) || StringUtils.isEmpty(className)){
			return null;
		}
		String classFullName = toFullPath(packageName, className);
		return castTo(classForName(classFullName));
	}
	
	public static String toFullPath(String packageName, String className) {
		if(StringUtils.isEmpty(packageName) || StringUtils.isEmpty(className)){
			return className;
		}
		if(!className.contains("\\.")) {
			className = packageName + "." + className;
		}
		return className;
	}

}
