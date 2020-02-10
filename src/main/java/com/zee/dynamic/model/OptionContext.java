package com.zee.dynamic.model;

public class OptionContext {
	private String name;
	private String label;
	private Object value;
	
	public OptionContext() {
		
	}

	public OptionContext(String enumName, String name) {
		this.name = name;
		this.label = enumName+"."+name;
		this.value = name;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}
	
	
}
