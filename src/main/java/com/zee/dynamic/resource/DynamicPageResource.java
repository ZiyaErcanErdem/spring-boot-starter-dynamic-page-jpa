package com.zee.dynamic.resource;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.zee.dynamic.DynamicPageManager;
import com.zee.dynamic.model.DynamicAuthorizableSearchRequest;
import com.zee.dynamic.model.DynamicAuthorizedSearchResponse;
import com.zee.dynamic.model.PageMetamodel;
import com.zee.dynamic.util.DynamicPaginationUtil;


@RestController
@RequestMapping("/api")
public class DynamicPageResource {

    private final Logger log = LoggerFactory.getLogger(DynamicPageResource.class);
    private final DynamicPageManager dynamicPageManager;

    public DynamicPageResource(DynamicPageManager dynamicPageManager) {
        this.dynamicPageManager = dynamicPageManager;
    }

    @GetMapping("/search/metamodel/{qualifier}")
    public ResponseEntity<PageMetamodel<?>> getQueryMetadataOf(@PathVariable String qualifier) {
        log.debug("REST request to get QueryMetadata of {}", qualifier);
        PageMetamodel<?> metamodel = this.dynamicPageManager.getPageMetamodelOf(qualifier);

        //ResponseEntity<PageMetamodel<?>> test = this.svrProviderService.getPageMetamodelOf("CallPartyContext");
        //log.debug("Test {}", test);

        //Pageable defaultPageable = PageRequest.of(0, 20, Sort.by(Direction.DESC, "id"));
        //String defaultSearch = "(mediaCount=gt=0;project.projectGroups.groupPath=='/Projects/Iletisim Merkezi*';project.projectGroups.status=='ACTIVE')";
        //ResponseEntity<List<?>> testPage = this.svrProviderService.search("CallPartyContext", defaultSearch, defaultPageable);
        //log.debug("Test {}", testPage);

        return new ResponseEntity<>(metamodel, HttpStatus.OK);
    }

    @GetMapping("/search/dynamic/{qualifier}")
    public ResponseEntity<List<?>> search(@PathVariable String qualifier, @RequestParam String search, Pageable pageable) {
        log.debug("REST request to search {} via RSQL {}", qualifier, search);
        Page<?> page = this.dynamicPageManager.search(qualifier, search, pageable);
        HttpHeaders headers = DynamicPaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        //HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/search/dynamic/"+qualifier);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @PostMapping("/search/dynamic/{qualifier}")
    public ResponseEntity<DynamicAuthorizedSearchResponse<?, ?>> authorizableSearch(@PathVariable String qualifier, @NotNull @Valid @RequestBody DynamicAuthorizableSearchRequest request, Pageable pageable) {
        log.debug("REST request to authorizableSearch via RSQL => {}",request);

        String query = request.getQuery();
        Page<?> page = this.dynamicPageManager.search(qualifier, query, pageable);

        DynamicAuthorizedSearchResponse<?,?> response = this.dynamicPageManager.authorize(request, page);
        Page<?> pageAuthorized = response.getPage();
        HttpHeaders headers = DynamicPaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), pageAuthorized);
        //HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(pageAuthorized, "/api/search/dynamic/"+qualifier);
        return new ResponseEntity<>(response, headers, HttpStatus.OK);
    }

}
