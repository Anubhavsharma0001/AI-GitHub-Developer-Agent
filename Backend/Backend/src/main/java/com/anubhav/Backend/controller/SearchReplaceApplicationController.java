package com.anubhav.Backend.controller;

import com.anubhav.Backend.service.SearchReplaceApplicationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
public class SearchReplaceApplicationController {

    private final SearchReplaceApplicationService
            searchReplaceApplicationService;

    public SearchReplaceApplicationController(
            SearchReplaceApplicationService
                    searchReplaceApplicationService
    ) {
        this.searchReplaceApplicationService =
                searchReplaceApplicationService;
    }

    @PostMapping("/apply-change")
    public SearchReplaceApplicationService.SearchReplaceResult
    applyChange(
            @RequestParam Long repositoryId,
            @RequestParam boolean approved,
            @RequestBody String generatedChange
    ) {

        return searchReplaceApplicationService.applyChange(
                repositoryId,
                approved,
                generatedChange
        );
    }
}
