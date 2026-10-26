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
public class CodeGenerationController {

    private final ChunkEmbeddingService chunkEmbeddingService;
    private final HybridSearchService hybridSearchService;
    private final OllamaChatService ollamaChatService;

    public CodeGenerationController(
            ChunkEmbeddingService chunkEmbeddingService,
            HybridSearchService hybridSearchService,
            OllamaChatService ollamaChatService
    ) {
        this.chunkEmbeddingService = chunkEmbeddingService;
        this.hybridSearchService = hybridSearchService;
        this.ollamaChatService = ollamaChatService;
    }

    // =====================================================
    // GENERATE PROPOSED CODE CHANGES
    // =====================================================

    @GetMapping("/generate")
    public String generateCodeChanges(
            @RequestParam String issue
    ) {

        // =================================================
        // 1. Generate issue embedding
        // =================================================

        float[] issueEmbedding =
                chunkEmbeddingService.generateEmbedding(issue);


        // =================================================
        // 2. Retrieve relevant repository code
        // =================================================

        List<HybridSearchResult> results =
                hybridSearchService.search(
                        issue,
                        issueEmbedding,
                        10
                );


        if (results.isEmpty()) {

            return """
                    CODE GENERATION FAILED

                    No relevant repository code was found.

                    The agent will not generate code without
                    repository evidence.
                    """;
        }


        // =================================================
        // 3. Build repository context
        // =================================================

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


        // =================================================
        // 4. Ask local LLM to generate a patch
        // =================================================

        String prompt = """
                You are an AI software development agent.

                Your task is to propose code changes for the
                development issue using ONLY the provided
                repository source code.

                IMPORTANT:

                You are generating a PROPOSED PATCH only.

                Do NOT modify files directly.

                STRICT RULES:

                1. Use ONLY the provided repository context.

                2. Do not invent files, classes, methods,
                   variables, packages, APIs, dependencies,
                   or existing functionality.

                3. Prefer modifying existing files when the
                   required functionality already exists.

                4. Do not create a new file when an existing
                   repository file is clearly the correct place
                   for the change.

                5. Preserve existing code unless the issue
                   requires changing it.

                6. Do not rewrite entire files unnecessarily.

                7. Do not add external dependencies unless
                   the repository context proves that they are
                   already being used or the issue explicitly
                   requires them.

                8. If the repository does not provide enough
                   information to safely generate the change,
                   say:

                   INSUFFICIENT REPOSITORY CONTEXT

                9. Do not explain general programming concepts.

                10. Generate a unified diff.

                OUTPUT FORMAT:

                CHANGE SUMMARY
                <short explanation>

                FILES CHANGED
                - <file>

                FILES CREATED
                - <file>
                OR
                - None

                PATCH

                <unified diff>

                TESTS
                <tests that should be run or added>

                REPOSITORY CONTEXT:

                %s

                DEVELOPMENT ISSUE:

                %s
                """.formatted(
                context,
                issue
        );


        // =================================================
        // 5. Generate patch
        // =================================================

        String generatedPatch =
                ollamaChatService.ask(prompt);


        if (generatedPatch == null
                || generatedPatch.isBlank()) {

            return """
                    CODE GENERATION FAILED

                    The local AI model did not generate
                    a proposed patch.
                    """;
        }


        // =================================================
        // 6. Return generated patch
        // =================================================

        StringBuilder response =
                new StringBuilder();

        response.append(
                "CODE CHANGE PROPOSAL -----\n\n"
        );

        response.append(
                generatedPatch.trim()
        );


        // =================================================
        // 7. Sources
        // =================================================

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