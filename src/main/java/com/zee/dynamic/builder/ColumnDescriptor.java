package com.zee.dynamic.builder;

import java.beans.PropertyDescriptor;

import javax.persistence.Column;
import javax.persistence.OneToOne;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zee.dynamic.model.ColumnMetadata;
import com.zee.dynamic.model.ColumnType;
import com.zee.dynamic.model.PageMetamodel;
import com.zee.dynamic.model.RelationType;

public class ColumnDescriptor<T> {
	private PageMetamodel<?> container;
	private String propertyPath;
	private PropertyDescriptor propertyDescriptor;
	private TypeDescriptor typeDescriptor;

	private String propertyName;
	private int level;
	private int outerJoinCountToTop;
	private boolean idColumn;
	private String group;
	private String qualifier;
	private RelationType parentRelType;
	private Class<?> propertyType;

	private RelationType associationRelationType;
	private Class<?> associationType;
	private PageMetamodel<?> association = null;
	private DescriberState state;

	private ColumnMetadata column;

	public ColumnDescriptor(PageMetamodel<?> container, String propertyPath, PropertyDescriptor propertyDescriptor, TypeDescriptor typeDescriptor) {
		this(container, propertyPath, propertyDescriptor, typeDescriptor, false, null, null);
	}

	public ColumnDescriptor(PageMetamodel<?> container, String propertyPath, PropertyDescriptor propertyDescriptor, TypeDescriptor typeDescriptor, boolean idColumn) {
		this(container, propertyPath, propertyDescriptor, typeDescriptor, idColumn, null, null);
	}

	public ColumnDescriptor(PageMetamodel<?> container, String propertyPath, PropertyDescriptor propertyDescriptor, TypeDescriptor typeDescriptor, Class<?> associationType, RelationType associationRelationType) {
		this(container, propertyPath, propertyDescriptor, typeDescriptor, false, associationType, associationRelationType);
	}

	public ColumnDescriptor(PageMetamodel<?> container, String propertyPath, PropertyDescriptor propertyDescriptor, TypeDescriptor typeDescriptor, boolean idColumn, Class<?> associationType, RelationType associationRelationType){
		this.container = container;
		this.propertyPath = propertyPath;
		this.propertyDescriptor = propertyDescriptor;
		this.typeDescriptor = typeDescriptor;
		this.idColumn = idColumn;
		this.group = container.getGroup();
		this.qualifier = container.getQualifier();
		this.level = container.getLevel();
		this.outerJoinCountToTop = container.getOuterJoinCountToTop();
		this.propertyName = propertyDescriptor.getName();
		this.propertyType = propertyDescriptor.getPropertyType();
		this.parentRelType = container.getRelType();
		this.associationType = associationType;

		if(this.isAssociativeDescriber()) {
			this.state = DescriberState.PENDING;
			this.associationRelationType = associationRelationType;
		} else {
			this.state = DescriberState.PENDING;
			this.column = this.define();
			this.enhance();
			this.state = DescriberState.COMPLETED;
			this.container.addColumn(this.column);
		}
	}

	private ColumnMetadata define() {
		if(null != this.column || DescriberState.PENDING != this.state || null == this.container || null == this.propertyType || StringUtils.isEmpty(this.propertyPath)) {
			return this.column;
		}
		ColumnMetadata col = null;
		if(this.isAssociativeDescriber()) {
			col= new ColumnMetadata(this.container.getQualifier(), this.group, this.propertyPath, this.propertyName, this.associationRelationType, ColumnType.ASSOCIATION);
			col.setLevel(this.level);
		} else {
			col= new ColumnMetadata(this.container.getQualifier(), this.group, this.propertyPath, this.propertyName, this.parentRelType, this.propertyType);
			col.setLevel(this.level);
			col.setIdColumn(this.idColumn);
			if(this.idColumn) {
				this.container.setIdColumnName(this.propertyName);
			}
		}
		return col;
	}

	public void complete(AssociationTask<?> task) {

		if(this.state != DescriberState.PENDING) {
			return;
		}

		PageMetamodel<?> association = null == task ? null : task.getAssociation();
		ColumnDescriptor<?> minDescriber = null == task ? null : task.getMinDescriber();
		int validLevel = this.getLevel() + 1;
		int associationLevel = null == association ? -1 : association.getLevel();

		if(null == association || association.isDescribed() || associationLevel < 0) {
			this.state = DescriberState.IGNORED;
		} else if(this == minDescriber) {
			this.association = association;
			this.column = this.define();
			this.enhance();
			this.column.setMetamodel(this.association);
			this.container.addColumn(this.column);
			this.state = DescriberState.COMPLETED;
			if(null != association) {
				association.addAlias(this.propertyPath);
			}
		} else if(validLevel == associationLevel) {
			this.state = DescriberState.IGNORED;
			if(null != association) {
				association.addAlias(this.propertyPath);
			}
		} else {
			this.state = DescriberState.IGNORED;
		}
	}

	public String getPropertyName() {
		return this.propertyName;
	}

	public String getPropertyPath() {
		return this.propertyPath;
	}

	public int getLevel() {
		return this.level;
	}
		
	public int getOuterJoinCountToTop() {
		return outerJoinCountToTop;
	}

	public String getQualifier() {
		return this.qualifier;
	}

	public RelationType getParentRelationType() {
		return this.parentRelType;
	}

	public PropertyDescriptor getPropertyDescriptor() {
		return this.propertyDescriptor;
	}

	public TypeDescriptor getTypeDescriptor() {
		return this.typeDescriptor;
	}

	public RelationType getAssociationRelationType() {
		return this.associationRelationType;
	}

	public String getAssociationQualifier() {
		return this.isAssociativeDescriber() ? this.associationType.getSimpleName() : null;
	}

	public Class<?> getAssociationType() {
		return this.associationType;
	}

	public boolean isAssociativeDescriber() {
		return (null != this.associationType);
	}

	private void enhance() {
		if(null == this.column || null == this.typeDescriptor) {
			return;
        }

        Column columnAnnotation = this.typeDescriptor.getAnnotation(Column.class);
		if(null != columnAnnotation) {

            this.column.setListable(true);
            this.column.setSearchable(true);
            this.column.setViewable(true);
            this.column.setEditable(true);
            this.column.setIgnorable(false);
            this.column.setNullable(true);

			int maxLength = columnAnnotation.length();
			if(maxLength > 0) {
				this.column.setMaxLength(maxLength);
			}
			boolean isNullable = columnAnnotation.nullable();
			if(!isNullable) {
				this.column.setNullable(false);
			}
        }

        Size sizeAnnotation = this.typeDescriptor.getAnnotation(Size.class);
		if(null != sizeAnnotation) {
			int minSize = sizeAnnotation.min();
			int maxSize =  sizeAnnotation.max();

			if(minSize > 0) {
				this.column.setMinLength(minSize);
			}
			if(maxSize < Integer.MAX_VALUE) {
				this.column.setMaxLength(maxSize);
			}
        }

        if(RelationType.OUTER == this.column.getRelType()) {
			this.column.setEditable(false);
			this.column.setIgnorable(true);
			this.column.setViewable(false);
			this.column.setNullable(true);
			this.column.setListable(false);
			this.column.setSearchable(true);
			return;
		}

		if(RelationType.INNER == this.column.getRelType()) {
			this.column.setEditable(false);
			this.column.setNullable(true);
			this.column.setListable(true);
			this.column.setViewable(false);
			if(this.column.isIdColumn()) {
				this.column.setViewable(true);
				this.column.setNullable(false);
			} else {
                OneToOne oneToOneAnnotation = this.typeDescriptor.getAnnotation(OneToOne.class);
                if (null != oneToOneAnnotation){
                    String mappedBy = oneToOneAnnotation.mappedBy();
                    if (StringUtils.isEmpty(mappedBy)){
                        this.column.setNullable(false);
                    }
                }
            }
		}
		
        NotNull notNullAnnotation = this.typeDescriptor.getAnnotation(NotNull.class);
		if(null != notNullAnnotation && this.column.isNullable()) {
			this.column.setNullable(false);
		}

		Max maxAnnotation = this.typeDescriptor.getAnnotation(Max.class);
		if(null != maxAnnotation) {
			long maxValue =  maxAnnotation.value();
			if(maxValue > 0 && maxValue < Long.MAX_VALUE) {
				this.column.setMaxValue(maxValue);
			}
		}

		Min minAnnotation = this.typeDescriptor.getAnnotation(Min.class);
		if(null != minAnnotation) {
			long minValue =  minAnnotation.value();
			if(minValue > 0 && minValue < Long.MAX_VALUE) {
				this.column.setMinValue(minValue);
			}
		}

		boolean  isCreatedDate = this.typeDescriptor.hasAnnotation(CreatedDate.class);
		boolean  isCreatedBy = this.typeDescriptor.hasAnnotation(CreatedBy.class);
		boolean  isLastModifiedDate = this.typeDescriptor.hasAnnotation(LastModifiedDate.class);
		boolean  isLastModifiedBy = this.typeDescriptor.hasAnnotation(LastModifiedBy.class);

		if(isCreatedDate || isCreatedBy || isLastModifiedDate || isLastModifiedBy) {
			this.column.setEditable(false);
			this.column.setNullable(true);
		}

		boolean  isIgnorable = this.typeDescriptor.hasAnnotation(JsonIgnore.class);
		if(isIgnorable) {
			this.column.setIgnorable(true);
			this.column.setListable(false);
		}

		if(this.column.isIdColumn()) {
			this.column.setEditable(false);
			this.column.setIgnorable(false);
			this.column.setNullable(false);
		}
	}

	@Override
	public String toString() {
		return "\r\nColumnDescriptor [level=" + level + ", outerJoinsToTop=" + outerJoinCountToTop + ", state=" + state + ", propertyPath=" + propertyPath + ", associationType=" + associationType
				+ ", parentRelType=" + parentRelType + ", group=" + group + ", propertyName=" + propertyName
				+ ", propertyType=" + propertyType + "]";
	}
}
