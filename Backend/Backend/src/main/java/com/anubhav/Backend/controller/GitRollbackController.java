package com.anubhav.Backend.controller;

import com.anubhav.Backend.service.GitRollbackService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/agent")
public class GitRollbackController {

    private final GitRollbackService gitRollbackService;
    private final JdbcTemplate jdbcTemplate;
    private final String repositoryBasePath;

    public GitRollbackController(
            GitRollbackService gitRollbackService,
            JdbcTemplate jdbcTemplate,
            @Value("${agent.repository.base-path}")
            String repositoryBasePath
    ) {
        this.gitRollbackService =
                gitRollbackService;

        this.jdbcTemplate =
                jdbcTemplate;

        this.repositoryBasePath =
                repositoryBasePath;
    }

    @PostMapping("/rollback")
    public GitRollbackService.RollbackResult rollback(
            @RequestParam Long repositoryId
    ) {

        // =====================================================
        // FIND REPOSITORY
        // =====================================================

        String repositoryName =
                findRepositoryName(repositoryId);

        if (repositoryName == null
                || repositoryName.isBlank()) {

            return new GitRollbackService.RollbackResult(
                    false,
                    "Repository was not found."
            );
        }

        // =====================================================
        // LOCAL REPOSITORY
        // =====================================================

        Path repositoryPath =
                Paths.get(repositoryBasePath)
                        .resolve(repositoryName)
                        .normalize();

        if (!Files.isDirectory(repositoryPath)) {

            return new GitRollbackService.RollbackResult(
                    false,
                    "Local repository does not exist: "
                            + repositoryPath
            );
        }

        // =====================================================
        // ROLLBACK
        // =====================================================

        return gitRollbackService.rollback(
                repositoryPath
        );
    }

    // =========================================================
    // FIND REPOSITORY
    // =========================================================

    private String findRepositoryName(
            Long repositoryId
    ) {

        List<String> names =
                jdbcTemplate.query(
                        """
                        SELECT repository_name
                        FROM github_repositories
                        WHERE id = ?
                        """,
                        (resultSet, rowNum) ->
                                resultSet.getString(
                                        "repository_name"
                                ),
                        repositoryId
                );

        if (names.isEmpty()) {
            return null;
        }

        return names.get(0);
    }
}