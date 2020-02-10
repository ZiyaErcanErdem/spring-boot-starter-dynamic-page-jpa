package com.zee.dynamic.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

public class ColumnMetadata {
	private String qualifier;
	private String group;
	private String label;
	private String name;
	private String path;
	private RelationType relType;
	private ColumnType columnType;
	private List<Operator> operators;
	private Object defaultValue;
	private Operator defaultOperator;
	private List<OptionContext> options;
	private int order;

	private boolean idColumn;
	private boolean nullable;
	private boolean listable;
	private boolean searchable;
	private boolean editable;

	private boolean ignorable;
	private boolean viewable;
	private long minValue;
	private long maxValue;
	private int minLength;
	private int maxLength;
	private int level;

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
		this.minValue = 0;
		this.maxValue = 0;
		this.minLength = 0;
		this.maxLength = 0;
	}

	public ColumnMetadata(String qualifier, String group, String path, String name, RelationType relType, ColumnType columnType) {
		this();
		this.relType = relType;
		this.qualifier = qualifier;
		this.group = group;
		this.path = path;
		this.name = this.extractName(this.path, this.group, this.relType);
		this.label = this.extractLabel(this.qualifier, this.name, this.relType);
		this.columnType = columnType;
		this.setupAvailableOperators();
		this.setupDefaults();
	}


	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ColumnMetadata(String qualifier, String group, String path, String name, RelationType relType, Class<?> fieldCls) {
		this();
		this.relType = relType;
		this.qualifier = qualifier;
		this.group = group;
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

	private String extractName(String path, String group, RelationType relType) {
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
			this.defaultOperator = Operator.EQ;
		}else if(ColumnType.NUMBER == this.columnType || ColumnType.DOUBLE == this.columnType) {
			this.nullable = false;
			this.defaultValue = 0;
			this.defaultOperator = Operator.GT;
		}else if(ColumnType.DATE == this.columnType) {
			this.nullable = true;
			this.defaultValue = null;
			this.defaultOperator = Operator.GT;
		}else if(ColumnType.STRING == this.columnType) {
			this.nullable = true;
			this.defaultValue = "";
			this.defaultOperator = Operator.LIKE;
		}else if(ColumnType.BOOLEAN == this.columnType) {
			this.nullable = false;
			this.defaultValue = false;
			this.defaultOperator = Operator.EQ;
		}
		if(RelationType.OUTER == this.relType) {
			this.listable = false;
			this.editable = false;
			this.searchable = true;
		}else if (RelationType.INNER == this.relType) {
			this.editable = false;
			this.searchable = true;
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void addOptions(Class<? extends Enum> enumCls) {
		String enumName = enumCls.getSimpleName();
		EnumSet<?> vals = EnumSet.allOf(enumCls);
		vals.forEach(e -> {
			OptionContext o = new OptionContext(enumName,e.name());
			this.addOption(o);
			this.defaultValue = e.name();
			this.defaultOperator = Operator.EQ;
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

	public String getQualifier() {
		return qualifier;
	}

	public void setQualifier(String qualifier) {
		this.qualifier = qualifier;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

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

	public boolean isNullable() {
		return nullable;
	}

	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}

	public boolean isListable() {
		return listable;
	}

	public void setListable(boolean listable) {
		this.listable = listable;
	}

	public boolean isSearchable() {
		return searchable;
	}

	public void setSearchable(boolean searchable) {
		this.searchable = searchable;
	}

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public boolean isIgnorable() {
		return ignorable;
	}

	public void setIgnorable(boolean ignorable) {
		this.ignorable = ignorable;
	}

	public boolean isViewable() {
		return viewable;
	}

	public void setViewable(boolean viewable) {
		this.viewable = viewable;
	}

	public long getMinValue() {
		return minValue;
	}

	public void setMinValue(long minValue) {
		this.minValue = minValue;
	}

	public long getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(long maxValue) {
		this.maxValue = maxValue;
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

	public boolean isIdColumn() {
		return idColumn;
	}

	public void setIdColumn(boolean idColumn) {
        this.idColumn = idColumn;
        if (this.idColumn){
            this.label = this.name;
        }
	}

	public List<Operator> getOperators() {
		return operators;
	}

	public void setOperators(List<Operator> operators) {
		this.operators = operators;
	}

	public Object getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	public Operator getDefaultOperator() {
		return defaultOperator;
	}

	public void setDefaultOperator(Operator defaultOperator) {
		this.defaultOperator = defaultOperator;
	}

	public List<OptionContext> getOptions() {
		return options;
	}

	public void setOptions(List<OptionContext> options) {
		this.options = options;
	}

	@Override
	public String toString() {
		return "Field [group=" + group + ", relType=" + relType + ", path=" + path + ", columnType=" + columnType
				+ ", defaultValue=" + defaultValue + ", defaultOperator=" + defaultOperator + "]";
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

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}
}
