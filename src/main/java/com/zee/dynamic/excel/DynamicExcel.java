package com.zee.dynamic.excel;

import java.io.IOException;
import java.util.List;

import org.springframework.core.io.Resource;

import com.zee.dynamic.model.PageMetamodel;

public class DynamicExcel<T> {
	private PageMetamodel<T> metamodel;
	
	public DynamicExcel(PageMetamodel<T> metamodel) {		
		this.metamodel = metamodel;
	}
	
	public PageMetamodel<T> getPageMetamodel() {
		return this.metamodel;
	}
	
	public DynamicExcelWriter<T> createWriter() {
		ExcelDataContext<T> context = new ExcelDataContext<T>(this.getPageMetamodel());
		DynamicExcelWriter<T> writer = new DynamicExcelWriter<T>(context);
		return writer;
	}
	
	public DynamicExcelWriter<T> createWriter(ExcelDataContext<T> context) {
		DynamicExcelWriter<T> writer = new DynamicExcelWriter<T>(context);
		return writer;
	}
	
	public DynamicExcelReader<T> createReader() {		
		DynamicExcelReader<T> reader = new DynamicExcelReader<T>(this.getPageMetamodel());
		return reader;
	}
	
	public ExcelDataContext<T> read(Resource resource) throws IOException {
		ExcelDataContext<T> context = null;
		DynamicExcelReader<T> reader = this.createReader();
		context = reader.read(resource.getInputStream());	
		return context;
	}
	
	public Resource write(List<T> entities) throws IOException {
		ExcelDataContext<T> context = null;
		DynamicExcelWriter<T> writer = this.createWriter();
		ExcelDataContext<T> ctx = writer.getContext();	
		context = ctx;
		if(null != entities && !entities.isEmpty()) {
			entities.forEach(entity -> ctx.createEntity(entity));
		}
		Resource resource = writer.write();	
		return resource;
	}
	
	public Resource write(ExcelDataContext<T> context)  throws IOException {
		DynamicExcelWriter<T> writer = this.createWriter(context);
		ExcelDataContext<T> ctx = writer.getContext();	
		context = ctx;			
		Resource resource = writer.write();
		return resource;
	}
}
