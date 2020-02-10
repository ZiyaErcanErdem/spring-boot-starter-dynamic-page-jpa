package com.zee.dynamic.excel;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import com.zee.dynamic.model.ColumnMetadata;

public class ExcelEntity<T> {
	private ExcelDataContext<T> context;
	private int rowNum;
	private T entity;
	private BeanWrapper wrapper;
	private Map<String, MappedValue> mappedValues;
	private String message;
	private ExcelEntityProcessResult result;
	
	public ExcelEntity(ExcelDataContext<T> context) {
		super();
		this.context = context;
		this.mappedValues = new HashMap<String, MappedValue>();
		this.rowNum = this.context.getRowSize();
		this.result = ExcelEntityProcessResult.NONE;
	}
	
	public ExcelEntity(ExcelDataContext<T> context, T entity) {
		this(context);
		this.entity = entity;
		this.result = ExcelEntityProcessResult.NONE;
	}
	
	@SuppressWarnings("unchecked")
	public T convertToBean() {
		T entity = null;
		if(this.getResult() == ExcelEntityProcessResult.PARSE_ERROR) {
			return entity;
		}
		try {
			BeanWrapper beanWrapper = new BeanWrapperImpl(this.context.getEntityType());
			beanWrapper.setAutoGrowNestedPaths(true);			
			Map<String, Object> convertedValues = this.getConvertedValues();			
			beanWrapper.setPropertyValues(convertedValues);
			this.wrapper = beanWrapper;
			entity = ((T)beanWrapper.getWrappedInstance());
			this.setEntity(entity);
			this.setResult(ExcelEntityProcessResult.NONE);
		} catch (Exception err) {
			this.setProcessingError(ExcelEntityProcessResult.PARSE_ERROR, err);
		}
		return entity;
	}
	
	public BeanWrapper getWrapper() {
		if(null == this.entity) {
			return null;
		} else {
			if(null == this.wrapper) {
				this.wrapper = this.context.wrapEntity(this.entity);
			}
		}
		return this.wrapper;
	}
	
	public void mapValue(ColumnMetadata column, Object value) {
		try {
			String key = column.getPath();
			MappedValue mapped = new MappedValue(column, value);
			this.mappedValues.put(key, mapped);
			Object converted = this.context.parseColumnValue(column, value);
			mapped.setConverted(converted);
			
		} catch (Exception err) {
			this.setParseError(column, err);
		}		
	}
	
	public MappedValue getMappedValue(ColumnMetadata column) {
		String key = column.getPath();		
		return this.mappedValues.get(key);			
	}
	
	public String getWrappedValue(ColumnMetadata col) {
		BeanWrapper bw = this.getWrapper();
		String textValue = null;
		if(null != bw) {
			textValue = this.context.formatWrappedValue(bw, col);
		}else {
			MappedValue mapped = this.getMappedValue(col);
			if(null != mapped) {
				textValue = mapped.getOriginal() == null ? null : mapped.getOriginal().toString();
			}
		}
		return textValue;
	}
	
	public Map<String, MappedValue> getMappedValues() {
		return this.mappedValues;
	}

	public int getRowNum() {
		return rowNum;
	}
	
	public boolean hasError() {
		return (ExcelEntityProcessResult.PARSE_ERROR == this.result || ExcelEntityProcessResult.DATABASE_ERROR == this.result);
	}
	
	public boolean isSuccess() {
		return (ExcelEntityProcessResult.SUCCESS == this.result);
	}

	public T getEntity() {
		return entity;
	}

	public void setEntity(T entity) {
		this.entity = entity;
	}
	
	public void setParseError(ColumnMetadata column, Exception ex) {
		this.result = ExcelEntityProcessResult.PARSE_ERROR;
		this.appendMessage(this.extractMessage(ex, column));
		this.context.updateMessageCount(this.result);
	}
	
	public void setProcessingError(ExcelEntityProcessResult result, Exception ex) {
		this.result = result;
		this.appendMessage(this.extractMessage(ex));
		this.context.updateMessageCount(this.result);
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public ExcelEntityProcessResult getResult() {
		return result;
	}

	public void setResult(ExcelEntityProcessResult result) {
		this.result = result;
		this.context.updateMessageCount(this.result);
	}
	
	private void appendMessage(String message) {
		if(null == message) {
			return;
		}
		if(null == this.message) {
			this.message = "";
		}
		this.message = this.message + " - " + message;
	}
		
	private String extractMessage(Exception ex, ColumnMetadata column) {
		String msg = this.extractMessage(ex);
		if (null != column) {
			msg = "Field : " + column.getPath() + " => " + msg;
		}
		return msg;
	}

	private String extractMessage(Exception ex) {
		if(null == ex) {
			return null;
		}
		if(null != ex.getMessage()) {
			return ex.getClass().getSimpleName() + " => " + ex.getMessage();
		}else {
			return ex.toString();
		}
	}
		
	private Map<String, Object> getConvertedValues(){
		Map<String, Object> convertedValues = new HashMap<>();
		this.mappedValues.values().stream()
			.filter(mv -> (mv.getColumn().isIdColumn() || mv.getColumn().isEditable()))
			.forEach((mv) -> convertedValues.put(mv.getColumn().getPath(), mv.getConverted()));
		return convertedValues;
	}

	@Override
	public String toString() {
		return "ExcelEntity [row=" + rowNum + ", result=" + result + ", entity=" + entity + ", message=" + message + "]";
	}

}
