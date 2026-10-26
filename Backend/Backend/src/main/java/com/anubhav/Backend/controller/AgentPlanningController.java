package com.anubhav.Backend.controller;

import com.anubhav.Backend.dto.HybridSearchResult;
import com.anubhav.Backend.service.ChunkEmbeddingService;
import com.anubhav.Backend.service.HybridSearchService;
import com.anubhav.Backend.service.OllamaChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/agent")
public class AgentPlanningController {

    private final ChunkEmbeddingService chunkEmbeddingService;
    private final HybridSearchService hybridSearchService;
    private final OllamaChatService ollamaChatService;

    public AgentPlanningController(
            ChunkEmbeddingService chunkEmbeddingService,
            HybridSearchService hybridSearchService,
            OllamaChatService ollamaChatService
    ) {
        this.chunkEmbeddingService = chunkEmbeddingService;
        this.hybridSearchService = hybridSearchService;
        this.ollamaChatService = ollamaChatService;
    }

    @GetMapping("/plan")
    public String createPlan(
            @RequestParam String issue
    ) {

        // =====================================================
        // 1. Convert issue into an embedding
        // =====================================================

        float[] issueEmbedding =
                chunkEmbeddingService.generateEmbedding(issue);


        // =====================================================
        // 2. Search the repository
        // =====================================================

        List<HybridSearchResult> results =
                hybridSearchService.search(
                        issue,
                        issueEmbedding,
                        8
                );


        if (results.isEmpty()) {

            return """
                    ISSUE ANALYSIS

                    No relevant repository code was found.

                    The agent cannot safely create an
                    implementation plan without repository
                    evidence.
                    """;
        }


        // =====================================================
        // 3. Build repository context
        // =====================================================

        StringBuilder context =
                new StringBuilder();

        Set<String> sources =
                new LinkedHashSet<>();


        for (HybridSearchResult result : results) {

            String source =
                    result.getFileName()
                            + " | "
                            + result.getFilePath()
                            + " | Lines "
                            + result.getStartLine()
                            + "-"
                            + result.getEndLine();

            sources.add(source);


            context.append(
                    "=== REPOSITORY SOURCE ===\n"
            );

            context.append("File: ")
                    .append(result.getFileName())
                    .append("\n");

            context.append("Path: ")
                    .append(result.getFilePath())
                    .append("\n");

            context.append("Lines: ")
                    .append(result.getStartLine())
                    .append("-")
                    .append(result.getEndLine())
                    .append("\n");

            context.append("CODE:\n");

            context.append(result.getContent())
                    .append("\n\n");
        }


        // =====================================================
        // 4. Ask the local LLM to analyze the issue
        // =====================================================

        String prompt = """
                You are an AI software development planner.

                You are given:

                1. A development issue.
                2. Relevant source code retrieved from a
                   repository.

                Your job is to analyze the issue and create a
                practical implementation plan.

                STRICT RULES:

                1. Use ONLY the provided repository context.

                2. Do not invent files, classes, methods,
                   variables, APIs, dependencies, or behavior.

                3. If the repository does not provide enough
                   information, clearly state that.

                4. Do not write implementation code yet.

                5. Identify existing files/classes that are
                   relevant.

                6. Explain what needs to change.

                7. Give a numbered implementation plan.

                8. Mention tests that should be added or updated
                   only when supported by the repository context.

                Use this format:

                ISSUE
                <short description>

                RELEVANT FILES
                - file
                - file

                CURRENT IMPLEMENTATION
                <what the relevant code currently does>

                REQUIRED CHANGES
                1. ...
                2. ...
                3. ...

                TEST PLAN
                1. ...
                2. ...

                RISKS
                - ...

                REPOSITORY CONTEXT:

                %s

                DEVELOPMENT ISSUE:

                %s
                """.formatted(
                context,
                issue
        );


        // =====================================================
        // 5. Generate implementation plan
        // =====================================================

        String plan =
                ollamaChatService.ask(prompt);


        if (plan == null || plan.isBlank()) {

            return """
                    ISSUE ANALYSIS

                    The local AI model did not generate
                    an implementation plan.
                    """;
        }


        // =====================================================
        // 6. Return plan + repository sources
        // =====================================================

        StringBuilder response =
                new StringBuilder();

        response.append(
                "ISSUE ANALYSIS -----\n\n"
        );

        response.append(plan.trim());


        response.append(
                "\n\nREPOSITORY SOURCES -----\n"
        );


        int sourceNumber = 1;

        for (String source : sources) {

            response.append(sourceNumber)
                    .append(". ")
                    .append(source)
                    .append("\n");

            sourceNumber++;
        }


        return response.toString();
    }
}