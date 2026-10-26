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
public class FileImpactController {

    private final ChunkEmbeddingService chunkEmbeddingService;
    private final HybridSearchService hybridSearchService;
    private final OllamaChatService ollamaChatService;

    public FileImpactController(
            ChunkEmbeddingService chunkEmbeddingService,
            HybridSearchService hybridSearchService,
            OllamaChatService ollamaChatService
    ) {
        this.chunkEmbeddingService = chunkEmbeddingService;
        this.hybridSearchService = hybridSearchService;
        this.ollamaChatService = ollamaChatService;
    }

    @GetMapping("/impact")
    public String analyzeImpact(
            @RequestParam String issue
    ) {

        // =====================================================
        // 1. Generate issue embedding
        // =====================================================

        float[] issueEmbedding =
                chunkEmbeddingService.generateEmbedding(issue);


        // =====================================================
        // 2. Retrieve relevant repository code
        // =====================================================

        List<HybridSearchResult> results =
                hybridSearchService.search(
                        issue,
                        issueEmbedding,
                        10
                );


        if (results.isEmpty()) {

            return """
                    IMPACT ANALYSIS -----

                    No relevant repository code was found.

                    The agent cannot determine which files
                    should be changed safely.
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
        // 4. Ask LLM for file impact analysis
        // =====================================================

        String prompt = """
                You are an AI software repository impact analyzer.

                Analyze the development issue using ONLY the
                provided repository code.

                Your job is NOT to write code.

                Your job is to determine exactly which existing
                files are affected by the issue.

                STRICT RULES:

                1. Use ONLY repository evidence.

                2. Do not invent files or classes.

                3. Do not assume functionality exists unless
                   it is visible in the provided code.

                4. Do not suggest creating a file if an existing
                   file already provides the required functionality.

                5. Prefer modifying an existing file when the
                   required functionality already exists there.

                6. Only put a file under CREATE if the repository
                   evidence shows that the required component
                   does not already exist.

                7. Clearly distinguish existing functionality
                   from missing functionality.

                8. If the repository context is insufficient,
                   say so instead of guessing.

                Use EXACTLY this structure:

                ISSUE
                <issue>

                RELEVANT EXISTING FILES
                1. <file> - <why relevant>
                2. <file> - <why relevant>

                EXISTING FUNCTIONALITY
                <describe what the repository already contains>

                FILES TO MODIFY
                1. <file> - <exact reason>
                2. <file> - <exact reason>

                FILES TO CREATE
                1. <file> - <reason>
                OR
                None

                FILES TO LEAVE UNCHANGED
                1. <file> - <reason>

                IMPLEMENTATION IMPACT
                <short explanation of how the issue affects
                the existing code>

                RISKS
                1. <risk>
                2. <risk>

                REPOSITORY CONTEXT:

                %s

                DEVELOPMENT ISSUE:

                %s
                """.formatted(
                context,
                issue
        );


        // =====================================================
        // 5. Generate impact analysis
        // =====================================================

        String analysis =
                ollamaChatService.ask(prompt);


        if (analysis == null
                || analysis.isBlank()) {

            return """
                    IMPACT ANALYSIS -----

                    The local AI model did not generate
                    an impact analysis.
                    """;
        }


        // =====================================================
        // 6. Build response
        // =====================================================

        StringBuilder response =
                new StringBuilder();

        response.append(
                "IMPACT ANALYSIS -----\n\n"
        );

        response.append(
                analysis.trim()
        );


        // =====================================================
        // 7. Sources
        // =====================================================

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