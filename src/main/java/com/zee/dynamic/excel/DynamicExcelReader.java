package com.zee.dynamic.excel;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.zee.dynamic.model.ColumnMetadata;
import com.zee.dynamic.model.PageMetamodel;

public class DynamicExcelReader<T> extends DynamicExcelBase {
	
	private ExcelDataContext<T> context;	
	
	public DynamicExcelReader(PageMetamodel<T> metamodel) {
		super();
		this.context = new ExcelDataContext<T>(metamodel);
	}
		
	public ExcelDataContext<T> getContext(){
		return this.context;
	}

	public ExcelDataContext<T> read(InputStream resourceInputStream) throws IOException {
		Workbook workbook = null;
		Sheet sheet = null;		
		try {
			workbook = this.readWorkbook(resourceInputStream);
			sheet = this.getSheet(workbook, 0);
			this.parseSheet(sheet);
			workbook.close();
	    } finally {
	    	if(null != workbook) {
	    		try {workbook.close();}catch(Exception e) {}
	    	}
	    	if(null != resourceInputStream) {
	    		try {resourceInputStream.close();}catch(Exception e) {}
	    	}
	    }		
		return this.context;
	}
		
	protected void parseHeader(String rowMetaType, Row headerRow) {
		if("L".equals(rowMetaType)) {
			this.buildLabelMap(headerRow);
		} else if("T".equals(rowMetaType)) {
			this.buildTypeMap(headerRow);
		} else if("F".equals(rowMetaType)) {
			this.buildFormatMap(headerRow);
		}
	}

	protected void parseSheet(Sheet sheet) {
		while (this.hasNextRow(sheet)) {
			Row row = this.readRow(sheet);
			String rowMetaType = this.getRowMetaType(row);
			if(!"D".equals(rowMetaType)) {
				this.parseHeader(rowMetaType, row);
			} else {
				this.parseRow(row);
				
			}
		}
	}
	
	protected void parseRow(Row row) {
		// int rowNum = row.getRowNum();
		ExcelEntity<T> excelEntity = this.context.createEntity();
		
		Iterator<Cell> cellItr = row.iterator();		
		while (cellItr.hasNext()) {
			Cell cell = cellItr.next();	
			int cellNum = cell.getColumnIndex();
			if(cellNum > 0) {
				ColumnMetadata column = this.context.getColumnByIndex(cellNum);
				this.parseCell(cell, column, excelEntity);
			}
			cellNum++;
		}
		excelEntity.convertToBean();
	}
	
	protected void parseCell(Cell cell, ColumnMetadata column, ExcelEntity<T> excelEntity) {
		if(null == cell || null == column || null == excelEntity) {
			return;
		}
		Object value = null;
		if (cell.getCellTypeEnum() == CellType.STRING) {
			value = cell.getStringCellValue();
		} else if (cell.getCellTypeEnum() == CellType.NUMERIC) {
			value = cell.getNumericCellValue();
		} else if (cell.getCellTypeEnum() == CellType.BOOLEAN) {
			value = cell.getBooleanCellValue();
		}
		excelEntity.mapValue(column, value);		
	}

	private void buildLabelMap(Row row) {
		Map<Integer, ColumnMetadata> enhancedColumnsMap = new HashMap<Integer, ColumnMetadata>();
		
		Iterator<Cell> cellItr = row.iterator();
		int cellNum = 0;
		while (cellItr.hasNext()) {
			Cell cell = cellItr.next();	
			if(cellNum > 0) {
				String propPath = cell.getStringCellValue();
				ColumnMetadata col = this.context.getColumnByPath(propPath);
				if(null != col) {
					enhancedColumnsMap.put(cellNum, col);
				}
			}
			cellNum++;
		}
		if(!enhancedColumnsMap.isEmpty()) {
			this.context.setColumnsIndexMap(enhancedColumnsMap);
		}
	}
	
	private void buildTypeMap(Row row) {
		// Do nothing
	}
	
	private void buildFormatMap(Row row) {
		// Do nothing
	}
}
