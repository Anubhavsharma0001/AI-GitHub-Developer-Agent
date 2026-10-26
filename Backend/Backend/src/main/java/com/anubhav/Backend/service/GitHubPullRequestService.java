package com.anubhav.Backend.service;

import com.anubhav.Backend.github.GitHubInstallationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GitHubPullRequestService {

    private static final Long INSTALLATION_ID =
            167558882L;

    private static final String OWNER =
            "Anubhavsharma0001";

    private static final String REPOSITORY =
            "Basic-java-file";

    private static final String BASE_BRANCH =
            "main";

    private final GitHubInstallationService
            gitHubInstallationService;

    private final ObjectMapper objectMapper;

    private final RestClient restClient;

    public GitHubPullRequestService(
            GitHubInstallationService
                    gitHubInstallationService,
            ObjectMapper objectMapper
    ) {

        this.gitHubInstallationService =
                gitHubInstallationService;

        this.objectMapper =
                objectMapper;

        this.restClient =
                RestClient.builder()
                        .baseUrl("https://api.github.com")
                        .build();
    }

    public PullRequestResult createPullRequest(
            String branchName,
            String title,
            String description
    ) throws Exception {

        if (branchName == null
                || branchName.isBlank()) {

            return new PullRequestResult(
                    false,
                    "Branch name cannot be empty.",
                    null,
                    null
            );
        }

        if (title == null
                || title.isBlank()) {

            return new PullRequestResult(
                    false,
                    "Pull request title cannot be empty.",
                    null,
                    null
            );
        }

        branchName =
                branchName.trim();

        title =
                title.trim();

        description =
                description == null
                        ? ""
                        : description.trim();

        String installationToken =
                gitHubInstallationService
                        .generateInstallationToken(
                                INSTALLATION_ID
                        );

        /*
         * IMPORTANT:
         *
         * Before creating a PR, check whether an open PR
         * already exists for this branch.
         *
         * This prevents GitHub 422 errors when the agent
         * tries to create the same PR twice.
         */
        PullRequestResult existingPullRequest =
                findExistingPullRequest(
                        installationToken,
                        branchName
                );

        if (existingPullRequest != null) {

            return existingPullRequest;
        }

        String requestBody =
                objectMapper.writeValueAsString(
                        new PullRequestRequest(
                                title,
                                description,
                                branchName,
                                BASE_BRANCH
                        )
                );

        try {

            String response =
                    restClient
                            .post()
                            .uri(
                                    "/repos/{owner}/{repository}/pulls",
                                    OWNER,
                                    REPOSITORY
                            )
                            .header(
                                    "Authorization",
                                    "Bearer " + installationToken
                            )
                            .header(
                                    "Accept",
                                    "application/vnd.github+json"
                            )
                            .header(
                                    "X-GitHub-Api-Version",
                                    "2026-03-10"
                            )
                            .header(
                                    "User-Agent",
                                    "AI-GitHub-Developer-Agent"
                            )
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .body(requestBody)
                            .retrieve()
                            .body(String.class);

            JsonNode jsonNode =
                    objectMapper.readTree(response);

            String number =
                    jsonNode.has("number")
                            ? jsonNode
                            .get("number")
                            .asText()
                            : null;

            String url =
                    jsonNode.has("html_url")
                            ? jsonNode
                            .get("html_url")
                            .asText()
                            : null;

            return new PullRequestResult(
                    true,
                    "Pull request created successfully.",
                    number,
                    url
            );

        } catch (Exception exception) {

            /*
             * A race condition is possible:
             *
             * Agent A checks -> no PR
             * Agent B checks -> no PR
             * Agent A creates PR
             * Agent B tries -> GitHub 422
             *
             * Check once more before returning failure.
             */
            PullRequestResult retryLookup =
                    findExistingPullRequest(
                            installationToken,
                            branchName
                    );

            if (retryLookup != null) {
                return retryLookup;
            }

            return new PullRequestResult(
                    false,
                    "Pull request creation failed: "
                            + exception.getMessage(),
                    null,
                    null
            );
        }
    }

    private PullRequestResult findExistingPullRequest(
            String installationToken,
            String branchName
    ) {

        try {

            String response =
                    restClient
                            .get()
                            .uri(
                                    uriBuilder ->
                                            uriBuilder
                                                    .path(
                                                            "/repos/{owner}/{repository}/pulls"
                                                    )
                                                    .queryParam(
                                                            "state",
                                                            "open"
                                                    )
                                                    .queryParam(
                                                            "head",
                                                            OWNER
                                                                    + ":"
                                                                    + branchName
                                                    )
                                                    .queryParam(
                                                            "base",
                                                            BASE_BRANCH
                                                    )
                                                    .build(
                                                            OWNER,
                                                            REPOSITORY
                                                    )
                            )
                            .header(
                                    "Authorization",
                                    "Bearer " + installationToken
                            )
                            .header(
                                    "Accept",
                                    "application/vnd.github+json"
                            )
                            .header(
                                    "X-GitHub-Api-Version",
                                    "2026-03-10"
                            )
                            .header(
                                    "User-Agent",
                                    "AI-GitHub-Developer-Agent"
                            )
                            .retrieve()
                            .body(String.class);

            JsonNode jsonNode =
                    objectMapper.readTree(response);

            if (!jsonNode.isArray()
                    || jsonNode.isEmpty()) {

                return null;
            }

            JsonNode pullRequest =
                    jsonNode.get(0);

            String number =
                    pullRequest.has("number")
                            ? pullRequest
                            .get("number")
                            .asText()
                            : null;

            String url =
                    pullRequest.has("html_url")
                            ? pullRequest
                            .get("html_url")
                            .asText()
                            : null;

            return new PullRequestResult(
                    true,
                    "An open pull request already exists for this branch.",
                    number,
                    url
            );

        } catch (Exception ignored) {

            /*
             * If the lookup itself fails, return null.
             *
             * The caller will attempt normal PR creation.
             */
            return null;
        }
    }

    private record PullRequestRequest(
            String title,
            String body,
            String head,
            String base
    ) {
    }

    public record PullRequestResult(
            boolean success,
            String message,
            String pullRequestNumber,
            String pullRequestUrl
    ) {
    }
}