package com.anubhav.Backend.controller;

import com.anubhav.Backend.dto.VectorSearchResult;
import com.anubhav.Backend.service.KeywordSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/keyword-search")
public class KeywordSearchTestController {

    private final KeywordSearchService keywordSearchService;

    public KeywordSearchTestController(
            KeywordSearchService keywordSearchService
    ) {
        this.keywordSearchService = keywordSearchService;
    }

    @GetMapping
    public List<VectorSearchResult> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit
    ) {

        return keywordSearchService.search(
                query,
                limit
        );
    }
}