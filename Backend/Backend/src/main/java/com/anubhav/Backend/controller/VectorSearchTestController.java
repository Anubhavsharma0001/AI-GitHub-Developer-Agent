package com.anubhav.Backend.controller;

import com.anubhav.Backend.dto.VectorSearchResult;
import com.anubhav.Backend.service.ChunkEmbeddingService;
import com.anubhav.Backend.service.VectorSearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vector-search")
public class VectorSearchTestController {

    private final ChunkEmbeddingService chunkEmbeddingService;
    private final VectorSearchService vectorSearchService;

    public VectorSearchTestController(
            ChunkEmbeddingService chunkEmbeddingService,
            VectorSearchService vectorSearchService
    ) {
        this.chunkEmbeddingService = chunkEmbeddingService;
        this.vectorSearchService = vectorSearchService;
    }

    @GetMapping
    public List<VectorSearchResult> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit
    ) {

        float[] queryEmbedding =
                chunkEmbeddingService.generateEmbedding(query);

        return vectorSearchService.search(
                queryEmbedding,
                limit
        );
    }
}