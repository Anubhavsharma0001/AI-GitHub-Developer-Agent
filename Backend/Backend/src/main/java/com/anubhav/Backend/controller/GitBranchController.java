package com.anubhav.Backend.controller;

import com.anubhav.Backend.service.GitBranchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/agent")
public class GitBranchController {

    private final GitBranchService gitBranchService;
    private final JdbcTemplate jdbcTemplate;
    private final String repositoryBasePath;

    public GitBranchController(
            GitBranchService gitBranchService,
            JdbcTemplate jdbcTemplate,
            @Value("${agent.repository.base-path}")
            String repositoryBasePath
    ) {
        this.gitBranchService =
                gitBranchService;

        this.jdbcTemplate =
                jdbcTemplate;

        this.repositoryBasePath =
                repositoryBasePath;
    }

    @PostMapping("/branch")
    public GitBranchService.BranchResult createBranch(
            @RequestParam Long repositoryId,
            @RequestParam String branchName
    ) {

        String repositoryName =
                findRepositoryName(repositoryId);

        if (repositoryName == null
                || repositoryName.isBlank()) {

            return new GitBranchService.BranchResult(
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

            return new GitBranchService.BranchResult(
                    false,
                    "Local repository does not exist: "
                            + repositoryPath,
                    null
            );
        }

        return gitBranchService.createBranch(
                repositoryPath,
                branchName
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