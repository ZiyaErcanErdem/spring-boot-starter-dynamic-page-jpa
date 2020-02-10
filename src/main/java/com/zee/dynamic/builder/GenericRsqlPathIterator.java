package com.zee.dynamic.builder;

import java.util.Iterator;
import java.util.Set;

import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.PluralAttribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericRsqlPathIterator implements Iterator<Path<?>> {
	private static final Logger log = LoggerFactory.getLogger(GenericRsqlPathIterator.class);

	private String propertyPath;
	private String[] graph;
	private Path<?>[] paths;
	private int max = -1;
	private int current = -1;
	
	private Metamodel metaModel;
	private Path<?> startRoot;
	ManagedType<?> currentClassMetadata;
	private Path<?> root;
	private boolean distinct;
	
	private String currentVal = null;
	private String childVal = null;
	private String grandParentVal = null;
	private String grandParentPath = null;
	private String parentVal = null;
	private String parentPath = null;
	private String currentPath = null;
	
	public GenericRsqlPathIterator(Metamodel metaModel, Path<?> start, String path) {
		this.metaModel = metaModel;
		this.startRoot = start;
		this.root = this.startRoot;
		this.propertyPath = path;
		this.distinct = false;
		if(null == path || path.trim().isEmpty()) {
			this.graph = new String[]{};
			this.paths = new Path<?>[]{};
		} else {
			this.graph = this.propertyPath.split("\\.");
			this.paths = new Path<?>[this.graph.length];
			this.max = this.graph.length - 1;
		}
		
	}

	public boolean isDistinct() {
		return this.distinct;
	}
	
	@Override
	public boolean hasNext() {
		return this.current < this.max;
	}
	
	public String nextProperty() {
		if(this.max < 0) {			
			return null;
		}
		int index = this.current + 1;
		if(index <= this.max) {
			this.grandParentVal = this.parentVal;
			this.parentVal = this.currentVal;
			this.currentVal = this.graph[index];
			this.currentPath = (null == this.currentPath || this.currentPath.isEmpty() ? this.currentVal : this.currentPath + "." + this.currentVal);
			this.currentClassMetadata = this.getCurrentManagedType();
		} else {
			this.grandParentVal = this.parentVal;
			this.parentVal = this.currentVal;
			this.currentVal = null;
			this.childVal = null;
		}
		this.current = Math.min(index, this.max);
		if(this.hasNext()) {
			this.grandParentPath = this.parentPath;
			this.parentPath = (null == this.parentPath || this.parentPath.isEmpty() ? this.currentVal : this.parentPath + "." + this.currentVal);
			this.childVal = this.current < this.max ? this.graph[this.current + 1] : null;
		} else {
			this.childVal = null;
			this.currentPath = this.propertyPath;
		}
		
		return this.currentVal;
	}
	
	public String childProperty() {
		return this.childVal;
	}
	
	public ManagedType<?> getCurrentManagedType() {
		ManagedType<?> classMetadata = this.metaModel.managedType(this.root.getJavaType());
		return classMetadata;
	}
	
	public <T> boolean  hasMetamodelPropertyName(String property, ManagedType<T> classMetadata) {
        Set<Attribute<? super T, ?>> names = classMetadata.getAttributes();
        for (Attribute<? super T, ?> name : names) {
            if (name.getName().equals(property)) return true;
        }
        return false;
    }
	
	private <T> Class<?> getPropertyType(String attributeName) {
    	Class<?> propertyType = null;
    	if (this.currentClassMetadata.getAttribute(attributeName).isCollection()) {
    		propertyType = ((PluralAttribute<?, ? , ?>)this.currentClassMetadata.getAttribute(attributeName)).getBindableJavaType();
    	} else {
    		propertyType = this.currentClassMetadata.getAttribute(attributeName).getJavaType();
    	}
        return propertyType;
    }
	
	public void validateMetamodelProperty() {
	    if (!this.hasMetamodelPropertyName(this.currentVal, this.currentClassMetadata)) {
	        throw new IllegalArgumentException("Unknown property: " + this.currentVal + " from entity " + this.currentClassMetadata.getJavaType().getName());
	    }
	}
	
	public boolean isAssociationType(String attributeName){
    	return this.currentClassMetadata.getAttribute(attributeName).isAssociation();
    }
    
	public boolean isCollectionType(String attributeName){
    	return this.currentClassMetadata.getAttribute(attributeName).isCollection();
    }
    
	public boolean isEmbeddedType(String attributeName){
        return this.currentClassMetadata.getAttribute(attributeName).getPersistentAttributeType() == PersistentAttributeType.EMBEDDED;
    }
	
	public String currentProperty() {
		return this.currentVal;
	}
	
	public String parentProperty() {
		return this.parentVal;
	}
	
	public String grandParentProperty() {
		return this.grandParentVal;
	}
	
	public String parentPropertyPath() {
		return this.parentPath;
	}
	
	public String grandParentPropertyPath() {
		return this.grandParentPath;
	}
	
	public String currentPropertyPath() {
		return this.currentPath;
	}
	
	public boolean isFirst() {
		return this.current == 0;
	}
	
	public boolean isLast() {
		return this.current > -1 && this.current == this.max;
	}
	
	public int maxIndex() {
		return this.max;
	}
	
	public int currentIndex() {
		return this.current;
	}
	
	public Join<?, ?> getOrCreateJoin(From<?, ?> from, String attribute) {
		for (Join<?, ?> join : from.getJoins()) {
			boolean sameName = join.getAttribute().getName().equals(attribute);
			if (sameName && join.getJoinType().equals(JoinType.INNER)) {
				return join;
			}
		}
		return from.join(attribute, JoinType.INNER);
	}
		
	public Path<?> findNearestParentJoinPath(){
		for(int i = this.current; i >= 0; i--) {
			Path<?> p = this.paths[i];
			if(p instanceof From) {
				return p;
			}
		}
		return null;
	}
	
	public int findNearestParentJoinIndex(){
		for(int i = this.current; i >= 0; i--) {
			Path<?> p = this.paths[i];
			if(p instanceof From) {
				return i;
			}
		}
		return -1;
	}
	
	private Path<?> forceJoinStartingWith(int startIndex){
		Path<?> cur = null;
		if(startIndex < 0) {
			return null;
		}
		for(int i = startIndex; i < this.current; i++) {
			Path<?> parent = this.paths[i];
			int curIndex = i + 1;
			String prop = this.getPropertyFor(curIndex);
			if(parent instanceof From && null != prop) {
				//cur = this.joinWith((From<?, ?>)parent, prop);
				cur = this.getOrCreateJoin((From<?, ?>)parent, prop);
				this.paths[curIndex] = cur;
			}
		}
		return cur;
	}
	
	public String getPropertyFor(int index) {
		String prop = index < this.max ? this.graph[index] : null;
		return prop;
	}
	
	public Path<?> getJoinPathFor(int index) {
		Path<?> p = index < this.max ? this.paths[index] : null;
		return p;
	}
	
	private void checkRoot() {
        if(null == this.root.getModel() && !this.isFirst() && !this.isLast()) {            	
        	int startIndex = this.findNearestParentJoinIndex();
        	Path<?> forcedRoot =  this.forceJoinStartingWith(startIndex);
        	if(null != forcedRoot) {
        		this.root = forcedRoot;
        	}
    	 }
	}
	
	public Path<?> getNextAssociationPath() {
        if (this.root instanceof Join) {
        	Join<?, ?> join = (Join<?, ?>)this.root;
            this.root = join.get(this.currentVal);	
            this.paths[this.current] = this.root;
            this.checkRoot();
        } else if (this.root instanceof From){		        	
        	   log.info("Create join between {} and {}.", this.parentVal, this.currentVal);
        	   //this.root = this.joinWith((From<?, ?>)this.root, this.currentVal); 
        	   this.root = this.getOrCreateJoin((From<?, ?>)this.root, this.currentVal);
        	   this.paths[this.current] = this.root;
        } else {		        	
        	this.root = this.root.get(this.currentVal);
        	this.checkRoot();
        } 
        return this.root;
	}
	
	public Path<?> getNextPropertyPath() {
    	log.info("Create property path for type {} property {}.", this.parentVal, this.currentVal);
        root = root.get(this.currentVal);	
        this.paths[this.current] = this.root;
        return this.root;
	}
	
	public Path<?> next(){
    	this.nextProperty();
    	this.validateMetamodelProperty();
    	
	    if (this.isAssociationType(this.currentVal)) {
	        if(this.isCollectionType(this.currentVal)) {
	        	this.distinct = true;
	        }
	        Class<?> associationType = this.getPropertyType(this.currentVal);
	        this.currentClassMetadata = this.metaModel.managedType(associationType);
	        
	        if(this.childVal != null){					
				if(!this.hasMetamodelPropertyName(this.childVal, this.currentClassMetadata)){
					//look for subtype that have the property
					for (EntityType<?> entityType : this.metaModel.getEntities()) {
						IdentifiableType<?> supertype = entityType.getSupertype();
						if(this.currentClassMetadata.equals(supertype)){
							if(this.hasMetamodelPropertyName(this.childVal,entityType)){
								this.currentClassMetadata = entityType;
								break;
							}	
						}
					}
				}
			}
	        
	        this.root = this.getNextAssociationPath();
 
	    } else {	         
	    	this.root = this.getNextPropertyPath();
	        if (this.isEmbeddedType(this.currentVal)) {
	            Class<?> embeddedType = this.getPropertyType(this.currentVal);
	            this.currentClassMetadata = metaModel.managedType(embeddedType);
	        }
	    }
	    
	    return this.root;
	}

	@Override
	public String toString() {
		return "PropertyPath(" + this.current + "/" + this.max + ")[graph=" + this.propertyPath + ", parentGraph=" + this.parentPath + ", grand=" + this.grandParentVal + ", parent=" + this.parentVal
				+ ", current=" + this.currentVal + ", child=" + this.childVal + "]";
	}
	
	

}
