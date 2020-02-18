package com.zee.dynamic.excel.resource;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.zee.dynamic.DynamicPageManager;

/**
 * REST controller for managing MediaFile.
 */
@RestController
@RequestMapping("/api")
public class ExcelResource {

    private final Logger log = LoggerFactory.getLogger(ExcelResource.class);
    private final DynamicPageManager dynamicPageManager;

    public ExcelResource(DynamicPageManager dynamicPageManager) {
        this.dynamicPageManager = dynamicPageManager;
    }

    @GetMapping("/dynamic/excel/export/{qualifier}")
    public ResponseEntity<Resource> exportExcel(@PathVariable String qualifier, @RequestParam String search, Pageable pageable) throws IOException {
        log.debug("REST request to export {} via Excel {}", qualifier, search);
        Resource resource = this.dynamicPageManager.exportEntity(qualifier, search, pageable);

        String filename = qualifier + "Export.xlsx";
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"").body(resource);
    }

    @GetMapping("/dynamic/excel/template/{qualifier}")
    public ResponseEntity<Resource> exportExcelTemplate(@PathVariable String qualifier) throws IOException {
        log.debug("REST request to export excel template of {}", qualifier);
        Resource resource = this.dynamicPageManager.exportTemplate(qualifier);

        String filename = qualifier + "Template.xlsx";
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"").body(resource);
    }

    @PostMapping("/dynamic/excel/import/{qualifier}")
    public ResponseEntity<Resource> importExcel(@PathVariable String qualifier, @RequestParam("file") MultipartFile document) throws IOException {
        log.debug("REST request to import excel document of {}", qualifier);
        InputStreamResource resource = new InputStreamResource(document.getInputStream());
        Resource result = this.dynamicPageManager.importEntity(qualifier, resource);

        String filename = qualifier + "ImportResult.xlsx";
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"").body(result);
    }
}
