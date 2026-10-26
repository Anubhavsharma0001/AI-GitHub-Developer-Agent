package com.anubhav.Backend.controller;

import com.anubhav.Backend.service.AgentWorkflowService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent/workflow")
public class AgentWorkflowExecuteController {

    private final AgentWorkflowService agentWorkflowService;

    public AgentWorkflowExecuteController(
            AgentWorkflowService agentWorkflowService
    ) {
        this.agentWorkflowService =
                agentWorkflowService;
    }

    @PostMapping("/execute")
    public AgentWorkflowService.WorkflowResult execute(
            @RequestParam Long repositoryId,
            @RequestParam String issue,
            @RequestParam String branchName,
            @RequestParam(required = false)
            String commitMessage,
            @RequestParam(required = false)
            String pullRequestTitle,
            @RequestParam(required = false)
            String pullRequestDescription,
            @RequestParam(defaultValue = "true")
            boolean approved
    ) {

        return agentWorkflowService.execute(
                repositoryId,
                issue,
                branchName,
                commitMessage,
                pullRequestTitle,
                pullRequestDescription,
                approved
        );
    }
}