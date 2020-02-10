package com.zee.dynamic.excel;

import com.zee.dynamic.model.ColumnMetadata;

public class MappedValue {
	ColumnMetadata column;
	private Object original;
	private Object converted;

	public MappedValue(ColumnMetadata column, Object original) {
		super();
		this.column = column;
		this.original = original;
	}
	
	public MappedValue(ColumnMetadata column, Object original, Object converted) {
		super();
		this.column = column;
		this.original = original;
		this.converted = converted;
	}

	public Object getOriginal() {
		return original;
	}

	public void setOriginal(Object original) {
		this.original = original;
	}

	public Object getConverted() {
		return converted;
	}

	public void setConverted(Object converted) {
		this.converted = converted;
	}

	public ColumnMetadata getColumn() {
		return column;
	}

	public void setColumn(ColumnMetadata column) {
		this.column = column;
	}

	@Override
	public String toString() {
		return "MappedValue [path=" + (null != column ? column.getPath() : "null") + ", original=" + original + ", converted=" + converted + "]";
	}


	
}
