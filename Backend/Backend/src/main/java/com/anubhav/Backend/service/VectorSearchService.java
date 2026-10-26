package com.anubhav.Backend.service;

import com.anubhav.Backend.dto.VectorSearchResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VectorSearchService {

    private final JdbcTemplate jdbcTemplate;

    public VectorSearchService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<VectorSearchResult> search(
            float[] queryEmbedding,
            int limit
    ) {

        StringBuilder vector =
                new StringBuilder("[");

        for (int i = 0; i < queryEmbedding.length; i++) {

            if (i > 0) {
                vector.append(",");
            }

            vector.append(queryEmbedding[i]);
        }

        vector.append("]");

        return jdbcTemplate.query(
                """
                SELECT
                    c.id,
                    f.name AS file_name,
                    f.path AS file_path,
                    c.start_line,
                    c.end_line,
                    c.content,
                    c.embedding <=> ?::vector AS distance
                FROM github_source_chunks c
                JOIN github_source_files f
                    ON c.source_file_id = f.id
                WHERE c.embedding IS NOT NULL
                ORDER BY distance
                LIMIT ?
                """,

                (resultSet, rowNum) ->
                        new VectorSearchResult(
                                resultSet.getLong("id"),
                                resultSet.getString("file_name"),
                                resultSet.getString("file_path"),
                                resultSet.getInt("start_line"),
                                resultSet.getInt("end_line"),
                                resultSet.getString("content"),
                                resultSet.getDouble("distance")
                        ),

                vector.toString(),
                limit
        );
    }
}