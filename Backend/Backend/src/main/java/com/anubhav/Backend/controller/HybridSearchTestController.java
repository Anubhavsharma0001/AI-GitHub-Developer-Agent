package com.anubhav.Backend.controller;

import com.anubhav.Backend.dto.HybridSearchResult;
import com.anubhav.Backend.service.ChunkEmbeddingService;
import com.anubhav.Backend.service.HybridSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hybrid-search")
public class HybridSearchTestController {

    private final ChunkEmbeddingService chunkEmbeddingService;
    private final HybridSearchService hybridSearchService;

    public HybridSearchTestController(
            ChunkEmbeddingService chunkEmbeddingService,
            HybridSearchService hybridSearchService
    ) {
        this.chunkEmbeddingService = chunkEmbeddingService;
        this.hybridSearchService = hybridSearchService;
    }

    @GetMapping
    public List<HybridSearchResult> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit
    ) {
        float[] queryEmbedding =
                chunkEmbeddingService.generateEmbedding(query);

        return hybridSearchService.search(
                query,
                queryEmbedding,
                limit
        );
    }
}