package com.anubhav.Backend.service;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Service
public class OllamaEmbeddingService {

    private final EmbeddingModel embeddingModel;

    public OllamaEmbeddingService(
            EmbeddingModel embeddingModel
    ) {
        this.embeddingModel = embeddingModel;
    }

    public float[] generateEmbedding(String text) {

        return embeddingModel
                .embed(text);
    }
}