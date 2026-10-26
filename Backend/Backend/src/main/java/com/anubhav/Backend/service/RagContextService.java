package com.anubhav.Backend.service;

import com.anubhav.Backend.dto.VectorSearchResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagContextService {

    public String buildContext(
            List<VectorSearchResult> results
    ) {

        StringBuilder context = new StringBuilder();

        for (VectorSearchResult result : results) {

            context.append("=== Code Chunk ")
                    .append(result.getChunkId())
                    .append(" ===\n");

            context.append("Lines ")
                    .append(result.getStartLine())
                    .append("-")
                    .append(result.getEndLine())
                    .append("\n\n");

            context.append(result.getContent())
                    .append("\n\n");
        }

        return context.toString();
    }
}