package com.anubhav.Backend.controller;

import com.anubhav.Backend.service.AgentWorkflowService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/agent")
public class AgentWorkflowController {

    private final AgentWorkflowService agentWorkflowService;

    private final JdbcTemplate jdbcTemplate;

    private final String repositoryBasePath;

    public AgentWorkflowController(
            AgentWorkflowService agentWorkflowService,
            JdbcTemplate jdbcTemplate,
            @Value("${agent.repository.base-path}")
            String repositoryBasePath
    ) {
        this.agentWorkflowService =
                agentWorkflowService;

        this.jdbcTemplate =
                jdbcTemplate;

        this.repositoryBasePath =
                repositoryBasePath;
    }

    @PostMapping("/workflow/start")
    public AgentWorkflowService.WorkflowResult startWorkflow(
            @RequestParam Long repositoryId,
            @RequestParam String branchName
    ) {

        String repositoryName =
                findRepositoryName(repositoryId);

        if (repositoryName == null
                || repositoryName.isBlank()) {

            return createErrorResult(
                    "REPOSITORY_NOT_FOUND",
                    "Repository was not found."
            );
        }

        Path repositoryPath =
                Paths.get(repositoryBasePath)
                        .resolve(repositoryName)
                        .normalize();

        if (!Files.isDirectory(repositoryPath)) {

            return createErrorResult(
                    "REPOSITORY_NOT_FOUND",
                    "Local repository does not exist: "
                            + repositoryPath
            );
        }

        return agentWorkflowService.startWorkflow(
                repositoryPath,
                branchName
        );
    }

    @PostMapping("/workflow/generate")
    public AgentWorkflowService.WorkflowResult generateCode(
            @RequestParam String issue
    ) {

        if (issue == null || issue.isBlank()) {

            return createErrorResult(
                    "INVALID_ISSUE",
                    "Development issue cannot be empty."
            );
        }

        return agentWorkflowService.generateCode(
                issue
        );
    }

    @PostMapping("/workflow/commit-push")
    public AgentWorkflowService.WorkflowResult commitAndPush(
            @RequestParam Long repositoryId,
            @RequestParam String branchName,
            @RequestParam String commitMessage
    ) {

        String repositoryName =
                findRepositoryName(repositoryId);

        if (repositoryName == null
                || repositoryName.isBlank()) {

            return createErrorResult(
                    "REPOSITORY_NOT_FOUND",
                    "Repository was not found."
            );
        }

        Path repositoryPath =
                Paths.get(repositoryBasePath)
                        .resolve(repositoryName)
                        .normalize();

        if (!Files.isDirectory(repositoryPath)) {

            return createErrorResult(
                    "REPOSITORY_NOT_FOUND",
                    "Local repository does not exist: "
                            + repositoryPath
            );
        }

        return agentWorkflowService.commitAndPush(
                repositoryPath,
                branchName,
                commitMessage
        );
    }

    @PostMapping("/workflow/rollback")
    public AgentWorkflowService.WorkflowResult rollback(
            @RequestParam Long repositoryId
    ) {

        String repositoryName =
                findRepositoryName(repositoryId);

        if (repositoryName == null
                || repositoryName.isBlank()) {

            return createErrorResult(
                    "REPOSITORY_NOT_FOUND",
                    "Repository was not found."
            );
        }

        Path repositoryPath =
                Paths.get(repositoryBasePath)
                        .resolve(repositoryName)
                        .normalize();

        if (!Files.isDirectory(repositoryPath)) {

            return createErrorResult(
                    "REPOSITORY_NOT_FOUND",
                    "Local repository does not exist: "
                            + repositoryPath
            );
        }

        return agentWorkflowService.rollback(
                repositoryPath
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

    private AgentWorkflowService.WorkflowResult
    createErrorResult(
            String status,
            String message
    ) {

        AgentWorkflowService.WorkflowResult result =
                new AgentWorkflowService.WorkflowResult();

        result.setStatus(status);
        result.setMessage(message);

        return result;
    }
}