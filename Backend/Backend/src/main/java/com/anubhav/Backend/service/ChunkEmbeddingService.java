
package com.anubhav.Backend.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChunkEmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;

    public ChunkEmbeddingService(
            EmbeddingModel embeddingModel,
            VectorStore vectorStore
    ) {
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
    }

    public float[] generateEmbedding(String content) {
        return embeddingModel.embed(content);
    }

    public int storeTextAsChunks(String content, String source) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(
                    "Content must not be empty"
            );
        }

        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException(
                    "Source must not be empty"
            );
        }

        int chunkSize = 1000;
        int overlap = 150;
        int start = 0;
        int chunkIndex = 0;

        List<Document> documents = new ArrayList<>();

        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());
            String chunk = content.substring(start, end);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", source);
            metadata.put("chunk_index", chunkIndex);

            documents.add(new Document(chunk, metadata));

            chunkIndex++;

            if (end == content.length()) {
                break;
            }

            start = end - overlap;
        }

        // Find existing chunks for this source.
        List<Document> existing = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(source)
                        .topK(1000)
                        .build()
        );

        // Delete only matching-source chunks that were retrieved.
        List<String> idsToDelete = existing.stream()
                .filter(document ->
                        source.equals(
                                document.getMetadata().get("source")
                        )
                )
                .map(Document::getId)
                .toList();

        if (!idsToDelete.isEmpty()) {
            vectorStore.delete(idsToDelete);
        }

        vectorStore.add(documents);

        return documents.size();
    }
}
