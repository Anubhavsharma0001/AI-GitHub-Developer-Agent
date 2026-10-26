package com.anubhav.Backend.controller;

import com.anubhav.Backend.service.TestExecutionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/agent")
public class TestExecutionController {

    private final TestExecutionService testExecutionService;
    private final JdbcTemplate jdbcTemplate;
    private final String repositoryBasePath;

    public TestExecutionController(
            TestExecutionService testExecutionService,
            JdbcTemplate jdbcTemplate,
            @Value("${agent.repository.base-path}")
            String repositoryBasePath
    ) {
        this.testExecutionService =
                testExecutionService;

        this.jdbcTemplate =
                jdbcTemplate;

        this.repositoryBasePath =
                repositoryBasePath;
    }

    // =========================================================
    // RUN TEST
    // =========================================================

    @PostMapping("/test")
    public TestExecutionService.TestExecutionResult runTest(
            @RequestParam Long repositoryId,
            @RequestParam String filePath
    ) {

        // -----------------------------------------------------
        // FIND REPOSITORY
        // -----------------------------------------------------

        String repositoryName =
                findRepositoryName(repositoryId);

        if (repositoryName == null
                || repositoryName.isBlank()) {

            return new TestExecutionService.TestExecutionResult(
                    "NOT_RUN",
                    "Repository was not found.",
                    "",
                    -1,
                    null
            );
        }

        // -----------------------------------------------------
        // BUILD LOCAL REPOSITORY PATH
        // -----------------------------------------------------

        Path repositoryPath =
                Paths.get(repositoryBasePath)
                        .resolve(repositoryName)
                        .normalize();

        if (!Files.isDirectory(repositoryPath)) {

            return new TestExecutionService.TestExecutionResult(
                    "NOT_RUN",
                    "Local repository does not exist: "
                            + repositoryPath,
                    "",
                    -1,
                    null
            );
        }

        // -----------------------------------------------------
        // BUILD JAVA FILE PATH
        // -----------------------------------------------------

        Path javaFile =
                repositoryPath
                        .resolve(filePath)
                        .normalize();

        // -----------------------------------------------------
        // SECURITY CHECK
        // -----------------------------------------------------

        if (!javaFile.startsWith(repositoryPath)) {

            return new TestExecutionService.TestExecutionResult(
                    "NOT_RUN",
                    "Invalid file path.",
                    "",
                    -1,
                    null
            );
        }

        if (!Files.isRegularFile(javaFile)) {

            return new TestExecutionService.TestExecutionResult(
                    "NOT_RUN",
                    "Java file does not exist: "
                            + filePath,
                    "",
                    -1,
                    null
            );
        }

        // -----------------------------------------------------
        // EXECUTE TEST
        // -----------------------------------------------------

        return testExecutionService.runTest(
                repositoryPath,
                javaFile
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