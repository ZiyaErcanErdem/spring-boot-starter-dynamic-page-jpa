package com.zee.dynamic.excel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.beans.BeanWrapper;

import com.zee.dynamic.model.ColumnMetadata;
import com.zee.dynamic.model.ColumnType;
import com.zee.dynamic.model.PageMetamodel;
import com.zee.dynamic.model.RelationType;

public class ExcelDataContext<T> {
	private Class<?> entityType;	
	private PageMetamodel<T> metamodel;
	private ExcelDataTypeConverter<T> typeConverter;
	private List<ExcelEntity<T>> data;
	private int messageCount = 0;
	private int errorCount = 0;
	private List<ColumnMetadata> columns;
	private Map<Integer, ColumnMetadata> columnsIndexMap = new HashMap<Integer, ColumnMetadata>();
	private Map<String, ColumnMetadata> columnsPathMap = new HashMap<String, ColumnMetadata>();
	
	public ExcelDataContext(PageMetamodel<T> metamodel) {
		super();
		this.entityType = metamodel.getEntityType();
		this.metamodel = metamodel;
		this.typeConverter = new ExcelDataTypeConverter<T>(this.entityType);
		this.data = new ArrayList<ExcelEntity<T>>();
		this.prepare();
	}
	
	public ExcelDataContext(PageMetamodel<T> metamodel, List<T> entities) {
		this(metamodel);
		this.populateData(entities);
	}
	
	private void prepare() {
		this.columns = this.metamodel.getColumns().stream()
				.filter(col -> col.getRelType() != RelationType.OUTER)
				.sorted(Comparator.comparing(ColumnMetadata::getOrder))
				.collect(Collectors.toList());
		
		final AtomicInteger columnIndex = new AtomicInteger();
		
		this.columnsIndexMap = this.columns.stream()
				.map(col -> this.getOrMapAssociationColumn(col))
				.sorted(Comparator.comparing(ColumnMetadata::getOrder))
				.collect(Collectors.toMap(c -> columnIndex.incrementAndGet(), c -> c));

		this.columnsPathMap = this.columns.stream()
				.map(col -> this.getOrMapAssociationColumn(col))
				.collect(Collectors.toMap(c -> c.getPath(), c -> c));

	}
	
	private ColumnMetadata getOrMapAssociationColumn(ColumnMetadata column) {
		if(column.getRelType() == RelationType.INNER) {
			PageMetamodel<?> associationMetaModel = column.getMetamodel();
			String associationIdentifierName = associationMetaModel.getKey();
			Optional<ColumnMetadata> associationColumnSearch = associationMetaModel.getColumns().stream().filter(a -> a.getName().equals(associationIdentifierName)).findFirst();
			if(associationColumnSearch.isPresent()) {
				ColumnMetadata associationColumn = associationColumnSearch.get();
				associationColumn.setOrder(column.getOrder());
				return associationColumn;
			}
		}
		return column;
	}
		

	
	public Class<?> getEntityType() {
		return entityType;
	}
		
	public String getQualifier() {
		return this.metamodel.getQualifier();
	}
	
	public int getColumnSize() {
		if(this.messageCount <= 0) {
			return this.columns.size() + 1;
		}else {
			return this.columns.size() + 2;
		}		
	}
	
	public int getRowSize() {
		return this.data.size();		
	}
	
	public ColumnMetadata getColumnByIndex(int index) {
		return this.columnsIndexMap.get(index);
	}
	
	public ColumnMetadata getColumnByPath(String path) {
		if(null == path || path.isEmpty()) {
			return null;
		}
		return this.columnsPathMap.get(path);
	}
	
	public String getPropertyPath(ColumnMetadata column) {
		String propertyPath = column.getPath();
		if(column.getRelType() == RelationType.INNER) {
			PageMetamodel<?> associationMetaModel = column.getMetamodel();
			String associationIdentifierName = associationMetaModel.getKey();
			Optional<ColumnMetadata> associationColumnSearch = associationMetaModel.getColumns().stream().filter(a -> a.getName().equals(associationIdentifierName)).findFirst();
			if(associationColumnSearch.isPresent()) {
				ColumnMetadata associationColumn = associationColumnSearch.get();
				propertyPath = associationColumn.getPath();
			}
		}
		return propertyPath;
	}
	
	public void setColumnsIndexMap(Map<Integer, ColumnMetadata> columnsIndexMap) {
		if(null == columnsIndexMap || columnsIndexMap.isEmpty()) {
			return;
		}
		this.columnsIndexMap = columnsIndexMap;
	}
		
	public PageMetamodel<T> getMetamodel() {
		return this.metamodel;
	}

	public List<ColumnMetadata> getColumns() {
		return this.columns;
	}

	public List<ExcelEntity<T>> getData() {
		return this.data;
	}

	public int getMessageCount() {
		return this.messageCount;
	}
	
	public boolean hasError() {
		return this.errorCount > 0;
	}

	public void populateData(List<T> entities) {
		if(null == entities || entities.isEmpty()) {
			return;
		}		
		for (T entity : entities) {
			this.createEntity(entity);
		}
	}
	
	public ExcelEntity<T> createEntity() {
		ExcelEntity<T> ee = new ExcelEntity<T>(this);
		this.addEntity(ee);
		return ee;
	}
	
	public ExcelEntity<T> createEntity(T entity) {
		if(null == entity) {
			return null;
		}
		ExcelEntity<T> ee = new ExcelEntity<T>(this, entity);
		this.addEntity(ee);
		return ee;
	}
	
	private void addEntity(ExcelEntity<T> excelEntity) {
		if(null == excelEntity) {
			return;
		}
		this.data.add(excelEntity);
		this.updateMessageCount(excelEntity.getResult());
	}
	
	protected void updateMessageCount(ExcelEntityProcessResult result) {
		if(ExcelEntityProcessResult.NONE != result) {
			this.messageCount++;
		}
		if(ExcelEntityProcessResult.DATABASE_ERROR == result || ExcelEntityProcessResult.PARSE_ERROR == result) {
			this.errorCount++;
		}
	}
	
	public BeanWrapper wrapEntity(T entity) {
		return this.typeConverter.wrap(entity);
	}
	
	public Object readWrappedValue(BeanWrapper wrapper, ColumnMetadata column) {
		String propertyPath = this.getPropertyPath(column);	
		Object value = null;
		if(column.getColumnType() != ColumnType.ASSOCIATION) {
			value = wrapper.getPropertyValue(propertyPath);
		} else {
			String parentPropertyPath = column.getName();
			Object parentVal = wrapper.getPropertyValue(parentPropertyPath);
			if(null != parentVal) {
				value = wrapper.getPropertyValue(propertyPath);
			}
		}
		return value;
	}
	
	public String formatWrappedValue(BeanWrapper wrapper, ColumnMetadata column) {
		Object value = this.readWrappedValue(wrapper, column);
		String formattedValue = this.formatColumnValue(column, value);
		return formattedValue;
	}

	public Object parseColumnValue(ColumnMetadata column, Object value) {		
		return this.typeConverter.parse(column, value);			
	}
	
	public String formatColumnValue(ColumnMetadata column, Object value) {
		return this.typeConverter.format(column, value);
	}
	
	public String getDataFormat(ColumnMetadata column) {
		return this.typeConverter.getDataFormat(column);
	}

}
