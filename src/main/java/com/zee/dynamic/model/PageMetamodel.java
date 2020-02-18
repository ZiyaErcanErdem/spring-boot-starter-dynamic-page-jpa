package com.zee.dynamic.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zee.dynamic.builder.AssociationTask;

public class PageMetamodel<T> {
	@JsonIgnore
	private Class<T> entityType;
	
	private AtomicInteger columnIndex;
	
	@JsonIgnore
	private boolean described;
	
	@JsonIgnore
	private PageMetamodel<?> parent;
	

	
	private String qualifier; 				// qualifier;	
	private String group; 
	private String path;
	private int level; 						// level
	private int distance; 					// outerJoinCountToTop
	private RelationType relType; 			// relType;
	private List<ColumnMetadata> columns; 		// columns;
	private List<String> aliases;
	
	private String key;						// idColumnName;
	private Map<ColumnType, List<Operator>> operators;
	
	
	@JsonCreator
	public PageMetamodel() {
		super();
	}

	public PageMetamodel(PageMetamodel<?> parent, String qualifier, RelationType relType, String path, int distance) {
		super();
		
		this.parent = parent;
		this.qualifier = qualifier;
		this.relType = relType;
		this.path = path;
		this.distance = distance;
		this.group = StringUtils.uncapitalize(this.qualifier);
		
		this.level = parent == null ? 1 : parent.getLevel() + 1;
		this.columns = new ArrayList<>();
		this.aliases = new ArrayList<>();		
		this.operators = new HashMap<>();
		
		this.columnIndex = new AtomicInteger();			
		
		if(this.level == 1) {
			this.described = true;
		}else {
			this.described = false;
		}
		
		this.setupAvailableOperators();
	}
	
	public PageMetamodel(PageMetamodel<?> parent, Class<T> entityType, RelationType relType, String path, int distance) {		
		this(parent, entityType.getSimpleName(), relType, path, distance);
		this.entityType = entityType;
	}

	public void describe(AssociationTask<?> task) {
		if(this.isDescribed()) {
			return;
		}
		task.complete(this);
		this.described = true;
	}
	
	@JsonIgnore
	public boolean isDescribed() {
		return this.described;
	}
	
	
	@JsonIgnore
	public Class<T> getEntityType() {
		return this.entityType;
	}
	
	@JsonIgnore
	public PageMetamodel<?> getParent() {
		return parent;
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

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public RelationType getRelType() {
		return relType;
	}

	public void setRelType(RelationType relType) {
		this.relType = relType;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
	
	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}
		
	public int getDistance() {
		return distance;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	public PageMetamodel<T> filter(Predicate<? super ColumnMetadata> filterPredicate) {
		if(null != filterPredicate) {
			List<ColumnMetadata> old = this.columns; 
			this.columns = new ArrayList<>();
			old.stream().filter(filterPredicate).forEach( cmd -> this.addColumn(cmd));
		}
		return this;
	}
	
	public void addColumn(ColumnMetadata column) {
		if(null == column || StringUtils.isEmpty(column.getName())) {
			return;
		}
		if(!this.columns.contains(column)) {
			this.columns.add(column);
			this.setColumnOrder(column);
		}
	}
	
	private void setColumnOrder(ColumnMetadata column) {
		if(null == column) {
			return;
		}
		int defaultOrder = (this.columnIndex.incrementAndGet());
		int colLevel = column.getLevel();
		int calculatedOrder = 0;
		
		if(!column.isIdColumn()) {
			calculatedOrder = 100;
		} 

		if(column.getColumnType() == ColumnType.ASSOCIATION) {
			calculatedOrder = calculatedOrder + colLevel * 300;
		}
		if(column.isIdColumn()) {
			calculatedOrder = calculatedOrder + defaultOrder;
		} else if(!column.isEditable()) {
			calculatedOrder = calculatedOrder + 100 + defaultOrder;
		} else {
			calculatedOrder = calculatedOrder + defaultOrder;
		}
		column.setOrder(calculatedOrder);
	}
	
	public List<ColumnMetadata> getColumns() {
		return this.columns;
	}

	public void setColumns(List<ColumnMetadata> cols) {
		this.columns = cols;
	}
		
	public List<String> getAliases() {
		return aliases;
	}

	public void setAliases(List<String> aliases) {
		this.aliases = aliases;
	}
	
	public void addAlias(String alias) {
		if(null == alias || StringUtils.isEmpty(alias)) {
			return;
		}
		if(!this.aliases.contains(alias)) {
			this.aliases.add(alias);
		}		
	}
	
	public void appendAliases(List<String> aliases) {
		if(null == aliases || aliases.isEmpty()) {
			return;
		}
		aliases.forEach(a -> this.addAlias(a));
	}
	

	public Map<ColumnType, List<Operator>> getOperators() {
		return operators;
	}

	public void setOperators(Map<ColumnType, List<Operator>> opers) {
		this.operators = opers;
	}
	
	private PageMetamodel<T> addOperator(ColumnType colType, Operator ...operator) {
		
		if (null == colType || ColumnType.UNKNOWN == colType) {
			return this;
		}
		
		List<Operator> ops = this.operators.get(colType);
		if (null == ops) {
			ops = new ArrayList<>();
			this.operators.put(colType, ops);
		}
		
		if (null == operator || 0 == operator.length) {
			return this;
		}
		
		for (Operator op : operator) {
			if(Operator.UNKNOWN != op && !ops.contains(op)) {
				ops.add(op);
			}
		}
		return this;
	}
	
	private void setupAvailableOperators() {		 
		this
		//.addOperator(ColumnType.STRING, Operator.BETWEEN)
		.addOperator(ColumnType.STRING, Operator.EQ, Operator.NOT_EQ, Operator.EW, Operator.NOT_EW, Operator.SW, Operator.NOT_SW, Operator.LIKE, Operator.NOT_LIKE, Operator.IS_NULL, Operator.IS_NOT_NULL)
		.addOperator(ColumnType.TEXT, Operator.EQ, Operator.NOT_EQ, Operator.EW, Operator.NOT_EW, Operator.SW, Operator.NOT_SW, Operator.LIKE, Operator.NOT_LIKE, Operator.IS_NULL, Operator.IS_NOT_NULL)
		.addOperator(ColumnType.NUMBER, Operator.EQ, Operator.NOT_EQ, Operator.GT, Operator.GTE, Operator.LT, Operator.LTE)
		.addOperator(ColumnType.DOUBLE, Operator.EQ, Operator.NOT_EQ, Operator.GT, Operator.GTE, Operator.LT, Operator.LTE)
		.addOperator(ColumnType.DATE, Operator.EQ, Operator.NOT_EQ, Operator.GT, Operator.GTE, Operator.LT, Operator.LTE)
		.addOperator(ColumnType.BOOLEAN, Operator.EQ, Operator.NOT_EQ)
		.addOperator(ColumnType.ENUM, Operator.EQ, Operator.NOT_EQ, Operator.IN, Operator.NOT_IN)
		.addOperator(ColumnType.ASSOCIATION, Operator.UNKNOWN);
	}
	
	@Override
	public String toString() {
		return "PageMetamodel [q=" + qualifier + ", path=" + path + ", lvl=" + level + ", rel=" + relType + ", d="
				+ distance + ", aliases=" + aliases+ "]";
	}
}
