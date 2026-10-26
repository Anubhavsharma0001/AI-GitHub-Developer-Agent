
package com.anubhav.Backend.controller;

import com.anubhav.Backend.service.OllamaEmbeddingService;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class EmbeddingTestController {

    private final OllamaEmbeddingService embeddingService;
    private final VectorStore vectorStore;

    public EmbeddingTestController(
            OllamaEmbeddingService embeddingService,
            VectorStore vectorStore
    ) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    // Test Ollama embedding generation
    @GetMapping("/api/embedding/test")
    public int testEmbedding(@RequestParam String text) {
        float[] embedding = embeddingService.generateEmbedding(text);
        return embedding.length;
    }

    // Store text and its generated embedding in PGVector
    @GetMapping("/api/embedding/store-test")
    public Map<String, Object> storeTest(
            @RequestParam String text
    ) {
        Document document = new Document(
                text,
                Map.of("source", "manual-test")
        );

        vectorStore.add(List.of(document));

        return Map.of(
                "success", true,
                "message", "Text submitted to PGVector successfully",
                "text", text
        );
    }

    // Search for text similar to the supplied query
    @GetMapping("/api/embedding/search-test")
    public List<Map<String, Object>> searchTest(
            @RequestParam String query
    ) {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(5)
                        .build()
        );

        return results.stream()
                .map(document -> Map.<String, Object>of(
                        "text", document.getText() == null
                                ? "" : document.getText(),
                        "metadata", document.getMetadata()
                ))
                .toList();
    }
}
