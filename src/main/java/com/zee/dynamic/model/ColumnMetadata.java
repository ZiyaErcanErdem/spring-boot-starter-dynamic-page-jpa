package com.zee.dynamic.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ColumnMetadata {
	
	
	@JsonIgnore
	private String qualifier; 				// ignore
	
	// private String group; 					// ignore
	
	@JsonIgnore
	private String label; 					// ignore
	private String name;
	
	@JsonIgnore
	private String path;		 			// ignore + client-resolve
	
	@JsonIgnore
	private RelationType relType;			// ignore + client-resolve
	
	private ColumnType columnType;
	
	@JsonIgnore
	private int level; 						// ignore
	
	@JsonIgnore
	private List<Operator> operators;		// ignore + client-resolve
		

	private List<OptionContext> options;
	private int order;

	@JsonIgnore
	private boolean idColumn; 				// ignore + client-resolve
	
	@JsonIgnore
	private boolean nullable;				// feature 0
	
	@JsonIgnore
	private boolean searchable;				// feature 1
	
	@JsonIgnore
	private boolean listable;				// feature 2
	
	@JsonIgnore
	private boolean viewable;				// feature 3
	
	@JsonIgnore
	private boolean editable;				// feature 4
	
	@JsonIgnore
	private boolean ignorable;				// feature 5
	
	
	private long min; 						// min
	private long max; 						// max
	private int minLength; 					// minLen
	private int maxLength; 					// maxLen
								//   0     1      2    3    4    5      6
	private String features; 	// |null|search|list|view|edit|ignore|idColumn

	
	@JsonIgnore
	private PageMetamodel<?> parent;
	
	private PageMetamodel<?> metamodel;

	public ColumnMetadata() {
		this.operators = new ArrayList<>();
		this.options = new ArrayList<>();
		this.nullable = true;
		this.listable = true;
		this.searchable = true;
		this.editable = true;
		this.idColumn = false;
		this.ignorable = false;
		this.viewable = true;
		this.min = 0;
		this.max = 0;
		this.minLength = 0;
		this.maxLength = 0;
	}

	public ColumnMetadata(PageMetamodel<?> parent, String qualifier, String path, String name, RelationType relType, ColumnType columnType) {
		this();
		this.parent = parent;
		this.relType = relType;
		this.qualifier = qualifier;
		this.path = path;
		this.name = this.extractName(this.path, this.relType);
		this.label = this.extractLabel(this.qualifier, this.name, this.relType);
		this.columnType = columnType;
		this.setupAvailableOperators();
		this.setupDefaults();
	}


	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ColumnMetadata(PageMetamodel<?> parent, String qualifier, String path, String name, RelationType relType, Class<?> fieldCls) {
		this();
		this.parent = parent;
		this.relType = relType;
		this.qualifier = qualifier;
		this.path = path;
		this.name = name; // this.extractName(this.path, this.group, this.relType);
		this.label = this.extractLabel(this.qualifier, this.name, this.relType);
		this.columnType = typeOf(fieldCls);
		this.setupAvailableOperators();
		this.setupDefaults();
		if(ColumnType.ENUM == this.columnType && null != fieldCls) {
			this.addOptions((Class<? extends Enum>)fieldCls);
		}
	}

	private String extractName(String path, RelationType relType) {
		if(StringUtils.isEmpty(path) || !path.contains(".")){
			return path;
		}
		if(RelationType.OUTER == relType) {
			String[] parts = path.split("\\.");
			if(parts.length > 1) {
				return parts[parts.length-1];
			}
			return path;
		} else if(RelationType.INNER == relType) {
			String[] parts = path.split("\\.");
			if(parts.length > 1) {
				return parts[parts.length-1];
			}
			return path;
		}
		return path;
	}

	private String extractLabel(String prefix, String label, RelationType relType) {
		String lbl = prefix+"."+label;
		return lbl;
	}

	public static ColumnType typeOf(Class<?> cls) {
		if(null == cls) {
			return ColumnType.STRING;
		}
		if(ClassUtils.isAssignable(cls, Integer.class) || ClassUtils.isAssignable(cls, Long.class)) {
			return ColumnType.NUMBER;
		}else if(ClassUtils.isAssignable(cls, Double.class) || ClassUtils.isAssignable(cls, BigDecimal.class)) {
			return ColumnType.DOUBLE;
		}else if(ClassUtils.isAssignable(cls, Boolean.class)) {
			return ColumnType.BOOLEAN;
		}else if(ClassUtils.isAssignable(cls, Date.class) || ClassUtils.isAssignable(cls, Instant.class)) {
			return ColumnType.DATE;
		}else if(Enum.class.isAssignableFrom(cls) /*ClassUtils.isAssignable(cls, Enum.class)*/) {
			return ColumnType.ENUM;
		}

		return ColumnType.STRING;
	}

	private void setupAvailableOperators() {
		if(ColumnType.STRING == this.columnType) {
			this
			//.addOperator(Operator.BETWEEN)
			.addOperator(Operator.EQ)
			.addOperator(Operator.NOT_EQ)
			.addOperator(Operator.EW)
			.addOperator(Operator.NOT_EW)
			.addOperator(Operator.SW)
			.addOperator(Operator.NOT_SW)
			.addOperator(Operator.LIKE)
			.addOperator(Operator.NOT_LIKE)
			.addOperator(Operator.IS_NULL)
			.addOperator(Operator.IS_NOT_NULL);
		} else if(ColumnType.NUMBER == this.columnType || ColumnType.DOUBLE == this.columnType  || ColumnType.DATE == this.columnType) {
			this
			//.addOperator(Operator.BETWEEN)
			.addOperator(Operator.EQ)
			.addOperator(Operator.NOT_EQ)
			.addOperator(Operator.GT)
			.addOperator(Operator.GTE)
			.addOperator(Operator.LT)
			.addOperator(Operator.LTE);
		} else if(ColumnType.BOOLEAN == this.columnType) {
			this
			.addOperator(Operator.EQ)
			.addOperator(Operator.NOT_EQ);
		} else if(ColumnType.ENUM == this.columnType) {
			this
			.addOperator(Operator.EQ)
			.addOperator(Operator.NOT_EQ)
			.addOperator(Operator.IN)
			.addOperator(Operator.NOT_IN);
		}
	}

	private void setupDefaults() {
		if(ColumnType.ENUM == this.columnType) {
			this.nullable = false;
		}else if(ColumnType.NUMBER == this.columnType || ColumnType.DOUBLE == this.columnType) {
			this.nullable = false;
		}else if(ColumnType.DATE == this.columnType) {
			this.nullable = true;
		}else if(ColumnType.STRING == this.columnType) {
			this.nullable = true;
		}else if(ColumnType.BOOLEAN == this.columnType) {
			this.nullable = false;
		}
		if(RelationType.OUTER == this.relType) {
			this.listable = false;
			this.editable = false;
			this.searchable = true;
		}else if (RelationType.INNER == this.relType) {
			this.editable = false;
			this.searchable = true;
		}
		this.setupFeatures();
	}
	
	public void setupFeatures() {
		
		//   0     1      2    3    4    5     6
		// |null|search|list|view|edit|ignore|isId|

		this.features = String.join(
				"|", 
				this.nullable ? "1" : "0",
				this.searchable ? "1" : "0",
				this.listable ? "1" : "0",
				this.viewable ? "1" : "0",
				this.editable ? "1" : "0",
				this.ignorable ? "1" : "0",
				this.idColumn ? "1" : "0"
		);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void addOptions(Class<? extends Enum> enumCls) {
		String enumName = enumCls.getSimpleName();
		EnumSet<?> vals = EnumSet.allOf(enumCls);
		vals.forEach(e -> {
			OptionContext o = new OptionContext(enumName,e.name());
			this.addOption(o);
		});
	}

	public ColumnMetadata addOperator(Operator operator) {
		if(!this.operators.contains(operator)) {
			this.operators.add(operator);
		}
		return this;
	}

	public ColumnMetadata addOption(OptionContext option) {
		if(!this.options.contains(option)) {
			this.options.add(option);
		}
		return this;
	}

	@JsonIgnore
	public String getQualifier() {
		return qualifier;
	}

	public void setQualifier(String qualifier) {
		this.qualifier = qualifier;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@JsonIgnore
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@JsonIgnore
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	@JsonIgnore
	public RelationType getRelType() {
		return relType;
	}

	public void setRelType(RelationType relType) {
		this.relType = relType;
	}

	public ColumnType getColumnType() {
		return columnType;
	}

	public void setColumnType(ColumnType columnType) {
		this.columnType = columnType;
	}

	@JsonIgnore
	public boolean isNullable() {
		return nullable;
	}

	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}

	@JsonIgnore
	public boolean isListable() {
		return listable;
	}

	public void setListable(boolean listable) {
		this.listable = listable;
	}

	@JsonIgnore
	public boolean isSearchable() {
		return searchable;
	}

	public void setSearchable(boolean searchable) {
		this.searchable = searchable;
	}

	@JsonIgnore
	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	@JsonIgnore
	public boolean isIgnorable() {
		return ignorable;
	}

	public void setIgnorable(boolean ignorable) {
		this.ignorable = ignorable;
	}

	@JsonIgnore
	public boolean isViewable() {
		return viewable;
	}

	public void setViewable(boolean viewable) {
		this.viewable = viewable;
	}

	public long getMin() {
		return min;
	}

	public void setMin(long min) {
		this.min = min;
	}

	public long getMax() {
		return max;
	}

	public void setMax(long max) {
		this.max = max;
	}

	public int getMinLength() {
		return minLength;
	}

	public void setMinLength(int minLength) {
		this.minLength = minLength;
	}

	public int getMaxLength() {
		return maxLength;
	}

	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}

	@JsonIgnore
	public boolean isIdColumn() {
		return idColumn;
	}

	public void setIdColumn(boolean idColumn) {
        this.idColumn = idColumn;
        if (this.idColumn){
            this.label = this.name;
        }
	}

	@JsonIgnore
	public List<Operator> getOperators() {
		return operators;
	}

	public void setOperators(List<Operator> operators) {
		this.operators = operators;
	}

	public List<OptionContext> getOptions() {
		return options;
	}

	public void setOptions(List<OptionContext> options) {
		this.options = options;
	}

	@Override
	public String toString() {
		return "Field [qualifier=" + qualifier + ", relType=" + relType + ", path=" + path + ", columnType=" + columnType
				+ ", features=" + features + "]";
	}

	public PageMetamodel<?> getMetamodel() {
		return metamodel;
	}

	public void setMetamodel(PageMetamodel<?> metamodel) {
		this.metamodel = metamodel;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@JsonIgnore
	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public String getFeatures() {
		return features;
	}
}
