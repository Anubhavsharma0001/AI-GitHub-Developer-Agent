
package com.anubhav.Backend.controller;

import com.anubhav.Backend.dto.CodeReviewRequest;
import com.anubhav.Backend.service.CodeReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/code-review")
public class CodeReviewController {

    private final CodeReviewService codeReviewService;

    public CodeReviewController(
            CodeReviewService codeReviewService
    ) {
        this.codeReviewService = codeReviewService;
    }

    @PostMapping("/repository")
    public Map<String, Object> reviewRepository(
            @RequestBody CodeReviewRequest request
    ) {
        String report =
                codeReviewService.reviewRepository(request);

        return Map.of(
                "repository_id", request.getRepositoryId(),
                "status", "completed",
                "review_report", report
        );
    }
}
