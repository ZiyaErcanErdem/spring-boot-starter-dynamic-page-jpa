package com.zee.dynamic.excel;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import com.zee.dynamic.model.ColumnMetadata;
import com.zee.dynamic.model.ColumnType;
import com.zee.dynamic.model.RelationType;

public class ExcelDataTypeConverter<T> {
	private static final String dateFormat = "yyyy-MM-dd HH:mm:ss";
	private final DateTimeFormatter DATE_TIME_FORMATTER;
	
	private Class<?> entityType;
	private BeanWrapper typeWrapper;
	
	public ExcelDataTypeConverter(Class<?> entityType) {
		this.entityType = entityType;
		this.DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(dateFormat).withZone(ZoneId.systemDefault());
		this.typeWrapper = new BeanWrapperImpl(this.entityType);
	}
	
	public BeanWrapper wrap(T entity) {
		BeanWrapper wrapper = new BeanWrapperImpl(entity);
		return wrapper;
	}
	
	public Class<?> getPropertyType(ColumnMetadata column) {
		if(null == column) {
			return null;
		}
		return this.typeWrapper.getPropertyType(column.getPath());
	}
	
	public String getDataFormat(ColumnMetadata column) {
		String format = "";
		if (null != column && column.getColumnType() == ColumnType.DATE){
			format = dateFormat;
		}
		return format;
	}
	
	@SuppressWarnings("unchecked")
	private Object parseEnum(ColumnMetadata column, Object value) {
		if (null != value && column.getColumnType() == ColumnType.ENUM) {
			Class<?> enumClass = this.getPropertyType(column);
			if (null != enumClass) {
				Enum<?> enumValue = Enum.valueOf(enumClass.asSubclass(Enum.class), value.toString());
				return enumValue;
			}
		}
		return value;
	}
	
	private Object parseDate(ColumnMetadata column, Object value) {
		if(null != value && column.getColumnType() == ColumnType.DATE) {
			LocalDateTime localDateTime = LocalDateTime.parse(value.toString(), this.DATE_TIME_FORMATTER);
			Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
			return instant;
		}
		return value;
	}
	
	private boolean parseBoolean(ColumnMetadata column, Object value) {
		if(null != value && column.getColumnType() == ColumnType.BOOLEAN) {
			String textValue = value.toString();
			if("Evet".equalsIgnoreCase(textValue) || "Yes".equalsIgnoreCase(textValue) || "True".equalsIgnoreCase(textValue)) {
				return true;
			} else if("Hayir".equalsIgnoreCase(textValue) || "No".equalsIgnoreCase(textValue) || "False".equalsIgnoreCase(textValue)) {
				return false;
			} else {
				return false;
			}
		}
		return false;
	}

	public Object parse(ColumnMetadata column, Object value) {
		String textValue  = (value == null ? null : value.toString());
		
		if(null == textValue || textValue.isEmpty()) {
			return null;
		}
		if(null == column) {
			return value;
		}
		
		if(column.getColumnType() == ColumnType.ASSOCIATION) {
			if (column.getRelType() == RelationType.INNER) {
				return Long.parseLong(textValue);
			}
		} else if(column.getColumnType() == ColumnType.BOOLEAN) {
			return parseBoolean(column, value);
		} else if(column.getColumnType() == ColumnType.DOUBLE) {
			return value;
		}  else if(column.getColumnType() == ColumnType.NUMBER) {
			BigDecimal number = new BigDecimal(textValue);
			return number.longValue();
		}  else if(column.getColumnType() == ColumnType.STRING) {
			if (value instanceof String) {
			   return textValue;
			} else if (value instanceof Double) {
				BigDecimal number = new BigDecimal(textValue);
				return ""+number.longValue();
			}
			return textValue;
		}  else if(column.getColumnType() == ColumnType.ENUM) {
			return parseEnum(column, value);			
		} else if(column.getColumnType() == ColumnType.DATE) {
			return parseDate(column, value);
		}
		return value;		
	}
	
	public String format(ColumnMetadata column, Object value) {
		if(null == value) {
			return "";
		}
		if(null == column) {
			return value.toString();
		}
		
		String formattedValue = null;

		if(column.getColumnType() == ColumnType.ASSOCIATION) {
			if (column.getRelType() == RelationType.INNER) {
				return value.toString();
			}
		} else if(column.getColumnType() == ColumnType.DOUBLE) {
			formattedValue = value.toString();
		} else if(column.getColumnType() == ColumnType.NUMBER) {
			formattedValue = value.toString();
		} else if(column.getColumnType() == ColumnType.STRING) {
			formattedValue = value.toString();
		} else if(column.getColumnType() == ColumnType.BOOLEAN) {
			formattedValue = value.toString();
		} else if(column.getColumnType() == ColumnType.DATE) {
			Instant typedVal = (Instant)value;
			formattedValue = this.DATE_TIME_FORMATTER.format(typedVal);
		} else if(column.getColumnType() == ColumnType.ENUM) {
			formattedValue = value.toString();
		} else {
			formattedValue = value.toString();
		}
		return formattedValue;
	}
}
