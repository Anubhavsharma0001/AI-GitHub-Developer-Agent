package com.anubhav.Backend.controller;

import com.anubhav.Backend.service.CompileTestService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/agent")
public class CompileTestController {

    private final CompileTestService compileTestService;
    private final JdbcTemplate jdbcTemplate;
    private final String repositoryBasePath;

    public CompileTestController(
            CompileTestService compileTestService,
            JdbcTemplate jdbcTemplate,
            @Value("${agent.repository.base-path}")
            String repositoryBasePath
    ) {
        this.compileTestService = compileTestService;
        this.jdbcTemplate = jdbcTemplate;
        this.repositoryBasePath = repositoryBasePath;
    }

    @PostMapping("/compile")
    public CompileTestService.CompileResult compile(
            @RequestParam Long repositoryId
    ) {

        String repositoryName =
                findRepositoryName(repositoryId);

        if (repositoryName == null
                || repositoryName.isBlank()) {

            return new CompileTestService.CompileResult(
                    false,
                    "Repository was not found.",
                    java.util.List.of()
            );
        }

        Path repositoryPath =
                Paths.get(repositoryBasePath)
                        .resolve(repositoryName)
                        .normalize();

        if (!Files.exists(repositoryPath)
                || !Files.isDirectory(repositoryPath)) {

            return new CompileTestService.CompileResult(
                    false,
                    "Local repository does not exist: "
                            + repositoryPath,
                    java.util.List.of()
            );
        }

        return compileTestService.compile(
                repositoryPath
        );
    }

    private String findRepositoryName(
            Long repositoryId
    ) {

        java.util.List<String> names =
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