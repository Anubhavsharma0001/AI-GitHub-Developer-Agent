package com.anubhav.Backend.controller;

import com.anubhav.Backend.service.GitHubPullRequestService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
public class GitHubPullRequestController {

    private final GitHubPullRequestService
            gitHubPullRequestService;

    public GitHubPullRequestController(
            GitHubPullRequestService
                    gitHubPullRequestService
    ) {
        this.gitHubPullRequestService =
                gitHubPullRequestService;
    }

    @PostMapping("/pull-request")
    public GitHubPullRequestService.PullRequestResult
    createPullRequest(
            @RequestParam String branchName,
            @RequestParam String title,
            @RequestBody(required = false)
            String description
    ) {

        try {

            return gitHubPullRequestService
                    .createPullRequest(
                            branchName,
                            title,
                            description
                    );

        } catch (Exception exception) {

            return new GitHubPullRequestService.PullRequestResult(
                    false,
                    "Pull request creation failed: "
                            + exception.getMessage(),
                    null,
                    null
            );
        }
    }
}