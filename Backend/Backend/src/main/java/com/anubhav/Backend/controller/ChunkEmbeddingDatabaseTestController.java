package com.anubhav.Backend.controller;

import com.anubhav.Backend.service.ChunkEmbeddingDatabaseService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChunkEmbeddingDatabaseTestController {

    private final ChunkEmbeddingDatabaseService databaseService;

    public ChunkEmbeddingDatabaseTestController(
            ChunkEmbeddingDatabaseService databaseService
    ) {
        this.databaseService = databaseService;
    }

    @PostMapping("/api/chunk-embedding/save")
    public String saveEmbedding(
            @RequestParam Long chunkId
    ) {

        databaseService.generateAndSaveEmbedding(chunkId);

        return "Embedding generated and saved successfully";
    }

    @PostMapping("/api/chunk-embedding/save-all")
    public String saveAllEmbeddings() {

        int processedCount =
                databaseService.generateEmbeddingsForAllChunks();

        return "Embeddings generated and saved for "
                + processedCount
                + " chunks";
    }
}