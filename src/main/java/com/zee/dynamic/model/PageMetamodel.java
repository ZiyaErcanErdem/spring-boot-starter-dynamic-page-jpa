package com.zee.dynamic.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zee.dynamic.builder.AssociationTask;

public class PageMetamodel<T> {
	@JsonIgnore
	private Class<T> entityType;
	
	@JsonIgnore
	private boolean described;
	
	private String qualifier;
	private RelationType relType;
	private String idColumnName;
	private String group;
	private int level;
	private int outerJoinCountToTop;
	private List<ColumnMetadata> columns;
	private List<String> aliases;	
	private AtomicInteger columnIndex;
	
	@JsonCreator
	public PageMetamodel() {
		super();
	}

	public PageMetamodel(String qualifier, RelationType relType, int level, int outerJoinCountToTop) {
		super();
		this.qualifier = qualifier;
		this.relType = relType;
		this.level = level;
		this.outerJoinCountToTop = outerJoinCountToTop;
		this.group = StringUtils.uncapitalize(this.qualifier);
		this.columns = new ArrayList<>();
		this.aliases = new ArrayList<>();
		this.columnIndex = new AtomicInteger();
		
		if(this.level == 1) {
			this.described = true;
		}else {
			this.described = false;
		}
	}
	
	public PageMetamodel(Class<T> entityType, RelationType relType, int level, int outerJoinCountToTop) {		
		this(entityType.getSimpleName(), relType, level, outerJoinCountToTop);
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
	
	public String getQualifier() {
		return qualifier;
	}

	public void setQualifier(String qualifier) {
		this.qualifier = qualifier;
	}

	public RelationType getRelType() {
		return relType;
	}

	public void setRelType(RelationType relType) {
		this.relType = relType;
	}

	public String getIdColumnName() {
		return idColumnName;
	}

	public void setIdColumnName(String idColumnName) {
		this.idColumnName = idColumnName;
	}
		
	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}
	
	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}
		
	public int getOuterJoinCountToTop() {
		return outerJoinCountToTop;
	}

	public void setOuterJoinCountToTop(int outerJoinCountToTop) {
		this.outerJoinCountToTop = outerJoinCountToTop;
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
	
	/*
	public ColumnMetadata createColumn(String group, String path, RelationType relType, Class<?> columnCls) {
		if(null == columnCls || StringUtils.isEmpty(path)) {
			return null;
		}
		ColumnMetadata column= new ColumnMetadata(this.getQualifier(), group, path, relType, columnCls);		
		return column;
	}
	
	public <A> ColumnMetadata createAssociation(String group, String path, PageMetamodel<A> metamodel) {
		if(null == metamodel || null == metamodel.getColumns() || metamodel.getColumns().isEmpty()) {
			return null;
		}
		ColumnMetadata column= new ColumnMetadata(this.getQualifier(), group, path, metamodel.getRelType(), ColumnType.ASSOCIATION);
		column.setMetamodel(metamodel);
		return column;
	}*/
	
	public List<ColumnMetadata> getColumns() {
		return this.columns;
	}

	public void setColumns(List<ColumnMetadata> columns) {
		this.columns = columns;
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
	
	@Override
	public String toString() {
		return "PageMetamodel [level=" + level + ", outerJoinsToTop=" + outerJoinCountToTop + ", qualifier=" + qualifier + ", group=" + group + ", aliases="
				+ aliases + ", relType=" + relType + "]";
	}
	
	
}
