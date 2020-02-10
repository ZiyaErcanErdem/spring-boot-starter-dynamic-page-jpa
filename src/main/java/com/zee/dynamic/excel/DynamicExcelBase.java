package com.zee.dynamic.excel;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public abstract class DynamicExcelBase {
	
	private int currentRow = 0;	
	private Iterator<Row> rowIterator = null;
	
	public DynamicExcelBase() {
		
	}

	protected SXSSFWorkbook creatWorkbook(int windowSize) {
		SXSSFWorkbook workbook = new SXSSFWorkbook(windowSize);
		return workbook;
	}

	protected SXSSFSheet createSheet(SXSSFWorkbook workbook) {
		SXSSFSheet sheet = workbook.createSheet();
		return sheet;
	}
	
	protected Workbook readWorkbook(InputStream is) throws IOException {
		Workbook workbook = new XSSFWorkbook(is);
		return workbook;
	}

	protected Sheet getSheet(Workbook workbook, int sheetNum) {
		Sheet sheet = workbook.getSheetAt(sheetNum);
		return sheet;
	}
	
	protected XSSFCellStyle createCellStyle(SXSSFWorkbook workbook) {
		XSSFCellStyle cellStyle = (XSSFCellStyle) workbook.createCellStyle();
		return cellStyle;
	}
			
	protected XSSFFont createFont(SXSSFWorkbook workbook) {
		XSSFFont font = (XSSFFont) workbook.createFont();
		return font;
	}
		
	protected Row createRow(Sheet sheet) {
		Row row = sheet.createRow(this.currentRow);
		this.currentRow++;
		return row;
	}
	
	protected Row readRow(Sheet sheet) {
		if(null == this.rowIterator) {
			this.rowIterator = sheet.iterator();
		}
		
		Row row = null;		
		if (this.rowIterator.hasNext()) {
			row = this.rowIterator.next();
			this.currentRow++;	
		}
		return row;
	}
	
	protected boolean hasNextRow(Sheet sheet) {
		if(null == this.rowIterator) {
			this.rowIterator = sheet.iterator();
		}
		return this.rowIterator.hasNext();
	}
		
	protected XSSFCellStyle createCellStyle(SXSSFWorkbook workbook, ExcelCellStyleType cellStyleType) {
		XSSFCellStyle cellStyle = this.createCellStyle(workbook);
		XSSFFont cellFont = this.createFont(workbook);		
		
		if(ExcelCellStyleType.META == cellStyleType){
			cellFont.setBold(true);
			cellFont.setFontHeightInPoints((short) 9);
			cellFont.setColor(IndexedColors.BLACK.index);			
			cellStyle.setFont(cellFont);
			XSSFColor colorGrey = new XSSFColor(new Color(210, 210, 210)); // Grey
			cellStyle.setFillForegroundColor(colorGrey);
			cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND );
			cellStyle.setAlignment(HorizontalAlignment.CENTER);
		} else if(ExcelCellStyleType.HEADER_LABEL == cellStyleType || ExcelCellStyleType.HEADER_ID == cellStyleType || ExcelCellStyleType.HEADER_ASSOCIATION == cellStyleType) {
			cellFont.setBold(true);
			cellFont.setFontHeightInPoints((short) 12);
			if(ExcelCellStyleType.HEADER_ID == cellStyleType) {
				cellFont.setColor(IndexedColors.BLACK.index);
				XSSFColor colorGreen = new XSSFColor(new Color(48, 240, 16)); // Green
				cellStyle.setFillForegroundColor(colorGreen);
			} else if(ExcelCellStyleType.HEADER_ASSOCIATION == cellStyleType) {
				cellFont.setColor(IndexedColors.BLACK.index);
				XSSFColor colorGold = new XSSFColor(new Color(255, 230, 155)); // Gold
				cellStyle.setFillForegroundColor(colorGold);
			} else {
				XSSFColor colorBlue = new XSSFColor(new Color(0, 112, 192));
				cellFont.setColor(colorBlue);
				cellStyle.setFillBackgroundColor(IndexedColors.BLACK.index);
			}
			cellStyle.setFont(cellFont);				
			cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND );
			cellStyle.setAlignment(HorizontalAlignment.CENTER);
		} else if(ExcelCellStyleType.HEADER_TYPE == cellStyleType || ExcelCellStyleType.HEADER_FORMAT == cellStyleType){
			//cellFont.setBold(true);
			cellFont.setFontHeightInPoints((short) 9);
			cellFont.setColor(IndexedColors.BLACK.index);			
			cellStyle.setFont(cellFont);
			XSSFColor colorGrey = new XSSFColor(new Color(210, 210, 210)); // Grey
			cellStyle.setFillForegroundColor(colorGrey);
			cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND );
			cellStyle.setAlignment(HorizontalAlignment.CENTER);
		} else if(ExcelCellStyleType.DATA_ERROR == cellStyleType){
			cellFont.setBold(true);
			cellFont.setFontHeightInPoints((short) 9);
			cellFont.setColor(IndexedColors.BLACK.index);			
			cellStyle.setFont(cellFont);
			
			XSSFColor colorRed = new XSSFColor(new Color(255, 0, 0)); // Red
			cellStyle.setFillForegroundColor(colorRed);
			cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND );
		} else if(ExcelCellStyleType.DATA_SUCCESS == cellStyleType){
			cellFont.setBold(true);
			cellFont.setFontHeightInPoints((short) 9);
			cellFont.setColor(IndexedColors.BLACK.index);			
			cellStyle.setFont(cellFont);
			
			XSSFColor colorGreen = new XSSFColor(new Color(48, 240, 16)); // Green
			cellStyle.setFillForegroundColor(colorGreen);
			cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND );
		} else if(ExcelCellStyleType.DATA_ID == cellStyleType){
			XSSFColor colorGreen = new XSSFColor(new Color(48, 240, 16)); // Green
			cellStyle.setFillForegroundColor(colorGreen);
			cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND );
		} else if(ExcelCellStyleType.DATA_ASSOCIATION == cellStyleType){
			XSSFColor colorGold = new XSSFColor(new Color(255, 230, 155)); // Gold
			cellStyle.setFillForegroundColor(colorGold);
			cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND );
		} else if(ExcelCellStyleType.DATA_READONLY == cellStyleType){
			XSSFColor colorGrey = new XSSFColor(new Color(210, 210, 210)); // Grey
			cellStyle.setFillForegroundColor(colorGrey);
			cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND );
		}
		
		return cellStyle;
	}

	protected String getRowMetaType(Row row) {
		Cell metaCell = row.getCell(0);
		String metaValue = metaCell.getStringCellValue();
		if("L".equals(metaValue)) {
			return "L";
		} else if("T".equals(metaValue)) {
			return "T";
		} else if("F".equals(metaValue)) {
			return "F";
		}
		return "D";
	}
}
