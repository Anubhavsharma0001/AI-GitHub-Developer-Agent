package com.anubhav.Backend.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/repository")
public class RepositoryExplorerController {

    private final JdbcTemplate jdbcTemplate;

    public RepositoryExplorerController(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // =====================================================
    // GET ALL FILES
    // =====================================================

    @GetMapping("/files")
    public List<RepositoryFileResponse> getFiles(
            @RequestParam Long repositoryId
    ) {

        return jdbcTemplate.query(
                """
                SELECT
                    id,
                    name,
                    path
                FROM github_source_files
                WHERE repository_id = ?
                ORDER BY path
                """,

                (resultSet, rowNum) ->
                        new RepositoryFileResponse(
                                resultSet.getLong("id"),
                                resultSet.getString("name"),
                                resultSet.getString("path")
                        ),

                repositoryId
        );
    }

    // =====================================================
    // GET SINGLE FILE
    // =====================================================

    @GetMapping("/files/{fileId}")
    public RepositoryFileDetailResponse getFile(
            @PathVariable("fileId") Long fileId
    ) {

        return jdbcTemplate.queryForObject(
                """
                SELECT
                    id,
                    name,
                    path,
                    content
                FROM github_source_files
                WHERE id = ?
                """,

                (resultSet, rowNum) ->
                        new RepositoryFileDetailResponse(
                                resultSet.getLong("id"),
                                resultSet.getString("name"),
                                resultSet.getString("path"),
                                resultSet.getString("content")
                        ),

                fileId
        );
    }

    // =====================================================
    // FILE LIST RESPONSE
    // =====================================================

    public record RepositoryFileResponse(
            Long id,
            String name,
            String path
    ) {
    }

    // =====================================================
    // FILE DETAIL RESPONSE
    // =====================================================

    public record RepositoryFileDetailResponse(
            Long id,
            String name,
            String path,
            String content
    ) {
    }
}