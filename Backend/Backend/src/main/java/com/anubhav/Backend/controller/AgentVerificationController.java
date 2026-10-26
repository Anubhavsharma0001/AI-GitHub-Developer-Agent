package com.anubhav.Backend.controller;

import com.anubhav.Backend.service.AgentVerificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/agent")
public class AgentVerificationController {

    private final AgentVerificationService verificationService;
    private final JdbcTemplate jdbcTemplate;
    private final String repositoryBasePath;

    public AgentVerificationController(
            AgentVerificationService verificationService,
            JdbcTemplate jdbcTemplate,
            @Value("${agent.repository.base-path}")
            String repositoryBasePath
    ) {
        this.verificationService =
                verificationService;

        this.jdbcTemplate =
                jdbcTemplate;

        this.repositoryBasePath =
                repositoryBasePath;
    }

    @PostMapping("/verify")
    public AgentVerificationService.VerificationResult verify(
            @RequestParam Long repositoryId,
            @RequestParam String testFile
    ) {

        // =====================================================
        // FIND REPOSITORY
        // =====================================================

        String repositoryName =
                findRepositoryName(repositoryId);

        if (repositoryName == null
                || repositoryName.isBlank()) {

            return new AgentVerificationService.VerificationResult(
                    false,
                    "REPOSITORY_NOT_FOUND",
                    "Repository was not found.",
                    null
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

            return new AgentVerificationService.VerificationResult(
                    false,
                    "REPOSITORY_NOT_FOUND",
                    "Local repository does not exist: "
                            + repositoryPath,
                    null
            );
        }

        // =====================================================
        // TEST FILE
        // =====================================================

        Path testFilePath =
                repositoryPath
                        .resolve(testFile)
                        .normalize();

        // =====================================================
        // SECURITY CHECK
        // =====================================================

        if (!testFilePath.startsWith(repositoryPath)) {

            return new AgentVerificationService.VerificationResult(
                    false,
                    "INVALID_PATH",
                    "Invalid test file path.",
                    null
            );
        }

        if (!Files.isRegularFile(testFilePath)) {

            return new AgentVerificationService.VerificationResult(
                    false,
                    "TEST_FILE_NOT_FOUND",
                    "Test file does not exist: "
                            + testFile,
                    null
            );
        }

        // =====================================================
        // VERIFY
        // =====================================================

        return verificationService.verify(
                repositoryPath,
                testFilePath
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