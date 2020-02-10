package com.zee.dynamic.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import com.zee.dynamic.model.ColumnMetadata;
import com.zee.dynamic.model.ColumnType;

public class DynamicExcelWriter<T> extends DynamicExcelBase {

	private ExcelDataContext<T> context;	
	private XSSFCellStyle metaCellStyle;
	private XSSFCellStyle headerCellStyleLabel;
	private XSSFCellStyle headerCellStyleType;
	private XSSFCellStyle headerCellStyleFormat;
	private XSSFCellStyle headerCellStyleId;
	private XSSFCellStyle headerCellStyleAssociation;
	private XSSFCellStyle dataCellStyleSuccess;
	private XSSFCellStyle dataCellStyleError;
	private XSSFCellStyle dataCellStyleId;	
	private XSSFCellStyle dataCellStyleAssociation;
	private XSSFCellStyle dataCellStyleReadonly;	

	public DynamicExcelWriter(ExcelDataContext<T> context) {
		super();
		this.context = context;
	}
	
	public ExcelDataContext<T> getContext(){
		return this.context;
	}

	public Resource write() throws IOException {
		
		SXSSFWorkbook workbook = null;
		SXSSFSheet sheet = null;
		ByteArrayOutputStream outputStream = null;
		Resource resource = null;
		
		try {
			workbook = this.creatWorkbook(100);
			sheet = this.createSheet(workbook);
			this.createStyles(workbook, sheet);
			this.writeHeader(workbook, sheet);
			this.writeSheet(sheet);
			this.configure(workbook, sheet);
			outputStream = new ByteArrayOutputStream();
			workbook.write(outputStream);
			resource = new ByteArrayResource(outputStream.toByteArray());
	    } finally {
	    	if(null != outputStream) {
	    		try {outputStream.close();}catch(Exception e) {}
	    	}
	    	if(null != workbook) {
	    		try {workbook.dispose();}catch(Exception e) {}
	    		try {workbook.close();}catch(Exception e) {}
	    	}
	    }

		return resource;
	}
	
	protected void configure(SXSSFWorkbook workbook, SXSSFSheet sheet) {
		try {
			sheet.trackAllColumnsForAutoSizing();
			IntStream.range(0, this.context.getColumnSize()).forEach(num -> sheet.autoSizeColumn(num));	
			sheet.createFreezePane(1, 3);
			workbook.setSheetName(0, this.context.getQualifier());
		} catch (Exception e) {}
	}
	
	protected void createStyles(SXSSFWorkbook workbook, SXSSFSheet sheet) {
		this.metaCellStyle = this.createCellStyle(workbook, ExcelCellStyleType.META);
		this.headerCellStyleLabel = this.createCellStyle(workbook, ExcelCellStyleType.HEADER_LABEL);
		this.headerCellStyleType = this.createCellStyle(workbook, ExcelCellStyleType.HEADER_TYPE);
		this.headerCellStyleFormat = this.createCellStyle(workbook, ExcelCellStyleType.HEADER_FORMAT);		
		this.headerCellStyleId = this.createCellStyle(workbook, ExcelCellStyleType.HEADER_ID);
		this.headerCellStyleAssociation = this.createCellStyle(workbook, ExcelCellStyleType.HEADER_ASSOCIATION);
		this.dataCellStyleId = this.createCellStyle(workbook, ExcelCellStyleType.DATA_ID);
		this.dataCellStyleAssociation = this.createCellStyle(workbook, ExcelCellStyleType.DATA_ASSOCIATION);
		this.dataCellStyleSuccess = this.createCellStyle(workbook, ExcelCellStyleType.DATA_SUCCESS);
		this.dataCellStyleError = this.createCellStyle(workbook, ExcelCellStyleType.DATA_ERROR);
		this.dataCellStyleReadonly = this.createCellStyle(workbook, ExcelCellStyleType.DATA_READONLY);
		
	}
		
	protected void writeHeader(SXSSFWorkbook workbook, SXSSFSheet sheet) {
		Row labelRow = this.createRow(sheet);
		Cell labelMetaCell = labelRow.createCell(0);
		labelMetaCell.setCellValue("L");
		labelMetaCell.setCellStyle(this.metaCellStyle);
		
		Row typeRow = this.createRow(sheet);
		Cell typeMetaCell = typeRow.createCell(0);
		typeMetaCell.setCellValue("T");
		typeMetaCell.setCellStyle(this.metaCellStyle);
		
		Row formatRow = this.createRow(sheet);
		Cell formatMetaCell = formatRow.createCell(0);
		formatMetaCell.setCellValue("F");
		formatMetaCell.setCellStyle(this.metaCellStyle);
		
		AtomicInteger columnIndex = new AtomicInteger();
		this.context.getColumns().stream().sorted(Comparator.comparing(ColumnMetadata::getOrder)).forEachOrdered(col -> {
			int cellnum = columnIndex.incrementAndGet();
			Cell labelCell = labelRow.createCell(cellnum);
			String propertyPath = this.context.getPropertyPath(col);
			labelCell.setCellValue(propertyPath);
			if(col.isIdColumn()) {
				labelCell.setCellStyle(this.headerCellStyleId);
			} else if(col.getColumnType() == ColumnType.ASSOCIATION) {
				labelCell.setCellStyle(this.headerCellStyleAssociation);
			}else {
				labelCell.setCellStyle(this.headerCellStyleLabel);
			}
						
			Cell typeCell = typeRow.createCell(cellnum);
			String columnType = col.getColumnType().toString();
			if(col.getColumnType() == ColumnType.ASSOCIATION) {
				if(!col.isNullable()) {
					columnType = columnType + "(*)";
				}
				columnType = columnType + "(A)";

			} else if(!col.isEditable() && !col.isIdColumn()) {
				columnType = columnType + "(I)";
			} else if(!col.isNullable()) {
				columnType = columnType + "(*)";
			}
			typeCell.setCellValue(columnType);
			typeCell.setCellStyle(this.headerCellStyleType);
			
			Cell formatCell = formatRow.createCell(cellnum);
			String format = this.context.getDataFormat(col);
			formatCell.setCellValue(format);
			formatCell.setCellStyle(this.headerCellStyleFormat);
		 });
				
	
		if(this.context.getMessageCount() > 0) {
			int cellnum = columnIndex.incrementAndGet();
			Cell labelCell = labelRow.createCell(cellnum);
			labelCell.setCellValue("Message");
			labelCell.setCellStyle(this.headerCellStyleLabel);
			
			Cell typeCell = typeRow.createCell(cellnum);
			String messageType = "Result";
			if(this.context.hasError()) {
				messageType = "Error";
			}
			typeCell.setCellValue(messageType);
			typeCell.setCellStyle(this.headerCellStyleType);
			
			Cell formatCell = formatRow.createCell(cellnum);
			String format = "";
			formatCell.setCellValue(format);
			formatCell.setCellStyle(this.headerCellStyleFormat);
		}
	}

	protected void writeSheet(Sheet sheet) {
		if(null == this.context || this.context.getRowSize() <= 0) {
			return;
		}
		List<ExcelEntity<T>> dataRows = this.context.getData();
		for (ExcelEntity<T> dataRow : dataRows) {
			Row row = this.createRow(sheet);
			this.mapEntityToRow(dataRow, row);
		}
	}

	protected void mapEntityToRow(ExcelEntity<T> data, Row row) {
		Cell dataMetaCell = row.createCell(0);
		dataMetaCell.setCellValue("U");
		if(data.hasError()) {
			dataMetaCell.setCellStyle(this.dataCellStyleError);
		}else if(data.isSuccess()) {
			dataMetaCell.setCellStyle(this.dataCellStyleSuccess);
		}else{
			dataMetaCell.setCellStyle(this.metaCellStyle);
		}
		
		AtomicInteger columnIndex = new AtomicInteger();
		this.context.getColumns().stream().sorted(Comparator.comparing(ColumnMetadata::getOrder)).forEachOrdered(col -> {
			int cellnum = columnIndex.incrementAndGet();
			Cell cell = row.createCell(cellnum);
			String textValue = data.getWrappedValue(col);
			cell.setCellValue(textValue);
			if(col.isIdColumn()) {
				cell.setCellStyle(this.dataCellStyleId);
			} else if(col.getColumnType() == ColumnType.ASSOCIATION) {
				cell.setCellStyle(this.dataCellStyleAssociation);
			} else if(!col.isEditable()) {
				cell.setCellStyle(this.dataCellStyleReadonly);
			} 
		});

		if(this.context.getMessageCount() > 0) {
			int cellnum = columnIndex.incrementAndGet();
			Cell cell = row.createCell(cellnum);
			String textValue = null;
			if(data.hasError()) {
				textValue = data.getMessage();
			}else if(data.isSuccess()) {
				textValue = "OK";
			}else{
				textValue = "";
			}
			cell.setCellValue(textValue);
		}
	}
}
