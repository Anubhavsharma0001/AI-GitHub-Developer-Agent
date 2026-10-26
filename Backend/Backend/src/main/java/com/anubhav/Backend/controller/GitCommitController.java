package com.anubhav.Backend.controller;

import com.anubhav.Backend.service.GitCommitService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/agent")
public class GitCommitController {

    private final GitCommitService gitCommitService;
    private final JdbcTemplate jdbcTemplate;
    private final String repositoryBasePath;

    public GitCommitController(
            GitCommitService gitCommitService,
            JdbcTemplate jdbcTemplate,
            @Value("${agent.repository.base-path}")
            String repositoryBasePath
    ) {
        this.gitCommitService =
                gitCommitService;

        this.jdbcTemplate =
                jdbcTemplate;

        this.repositoryBasePath =
                repositoryBasePath;
    }

    @PostMapping("/commit")
    public GitCommitService.CommitResult commit(
            @RequestParam Long repositoryId,
            @RequestParam String message
    ) {

        String repositoryName =
                findRepositoryName(repositoryId);

        if (repositoryName == null
                || repositoryName.isBlank()) {

            return new GitCommitService.CommitResult(
                    false,
                    "Repository was not found.",
                    null
            );
        }

        Path repositoryPath =
                Paths.get(repositoryBasePath)
                        .resolve(repositoryName)
                        .normalize();

        if (!Files.isDirectory(repositoryPath)) {

            return new GitCommitService.CommitResult(
                    false,
                    "Local repository does not exist: "
                            + repositoryPath,
                    null
            );
        }

        return gitCommitService.commit(
                repositoryPath,
                message
        );
    }

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