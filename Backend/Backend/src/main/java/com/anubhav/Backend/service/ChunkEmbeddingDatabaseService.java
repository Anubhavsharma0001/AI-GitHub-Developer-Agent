package com.anubhav.Backend.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChunkEmbeddingDatabaseService {

    private final JdbcTemplate jdbcTemplate;
    private final ChunkEmbeddingService chunkEmbeddingService;

    public ChunkEmbeddingDatabaseService(
            JdbcTemplate jdbcTemplate,
            ChunkEmbeddingService chunkEmbeddingService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.chunkEmbeddingService = chunkEmbeddingService;
    }

    public void generateAndSaveEmbedding(Long chunkId) {

        String content = jdbcTemplate.queryForObject(
                """
                SELECT content
                FROM github_source_chunks
                WHERE id = ?
                """,
                String.class,
                chunkId
        );

        if (isMeaningless(content)) {
            throw new RuntimeException(
                    "Chunk " + chunkId + " contains no meaningful content"
            );
        }

        float[] embedding =
                chunkEmbeddingService.generateEmbedding(content);

        saveEmbedding(chunkId, embedding);
    }

    public int generateEmbeddingsForAllChunks() {

        List<Long> chunkIds = jdbcTemplate.query(
                """
                SELECT id
                FROM github_source_chunks
                WHERE embedding IS NULL
                  AND content IS NOT NULL
                  AND content ~ '\\S'
                ORDER BY id
                """,
                (resultSet, rowNum) ->
                        resultSet.getLong("id")
        );

        int processedCount = 0;

        for (Long chunkId : chunkIds) {

            String content = jdbcTemplate.queryForObject(
                    """
                    SELECT content
                    FROM github_source_chunks
                    WHERE id = ?
                    """,
                    String.class,
                    chunkId
            );

            if (isMeaningless(content)) {
                System.out.println(
                        "Skipping meaningless chunk " + chunkId
                );
                continue;
            }

            float[] embedding =
                    chunkEmbeddingService.generateEmbedding(content);

            saveEmbedding(chunkId, embedding);

            processedCount++;

            System.out.println(
                    "Embedded chunk " + chunkId
                            + " (" + processedCount
                            + "/" + chunkIds.size() + ")"
            );
        }

        return processedCount;
    }

    private boolean isMeaningless(String content) {

        if (content == null || content.isBlank()) {
            return true;
        }

        String withoutWhitespace =
                content.replaceAll("\\s+", "");

        return withoutWhitespace.length() < 10;
    }

    private void saveEmbedding(
            Long chunkId,
            float[] embedding
    ) {

        StringBuilder vector = new StringBuilder("[");

        for (int i = 0; i < embedding.length; i++) {

            if (i > 0) {
                vector.append(",");
            }

            vector.append(embedding[i]);
        }

        vector.append("]");

        jdbcTemplate.update(
                """
                UPDATE github_source_chunks
                SET embedding = ?::vector
                WHERE id = ?
                """,
                vector.toString(),
                chunkId
        );
    }
}