
package com.anubhav.Backend.controller;

import com.anubhav.Backend.service.ChunkEmbeddingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ChunkEmbeddingTestController {

    private final ChunkEmbeddingService chunkEmbeddingService;

    public ChunkEmbeddingTestController(
            ChunkEmbeddingService chunkEmbeddingService
    ) {
        this.chunkEmbeddingService = chunkEmbeddingService;
    }

    // Test embedding generation
    @GetMapping("/api/chunk-embedding/test")
    public int testChunkEmbedding(
            @RequestParam String text
    ) {
        float[] embedding =
                chunkEmbeddingService.generateEmbedding(text);

        return embedding.length;
    }

    // Split supplied text into chunks and store in PGVector
    @PostMapping("/api/chunk-embedding/store")
    public Map<String, Object> storeChunks(
            @RequestBody Map<String, String> request
    ) {
        String content = request.get("content");
        String source = request.getOrDefault(
                "source",
                "manual-test"
        );

        int chunksStored =
                chunkEmbeddingService.storeTextAsChunks(
                        content,
                        source
                );

        return Map.of(
                "success", true,
                "chunksStored", chunksStored,
                "source", source
        );
    }
}
