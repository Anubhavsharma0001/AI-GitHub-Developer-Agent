package com.anubhav.Backend.controller;

import com.anubhav.Backend.dto.HybridSearchResult;
import com.anubhav.Backend.dto.RagClaimEvidenceResult;
import com.anubhav.Backend.service.ChunkEmbeddingService;
import com.anubhav.Backend.service.HybridSearchService;
import com.anubhav.Backend.service.OllamaChatService;
import com.anubhav.Backend.service.RagClaimEvidenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/rag")
public class RagContextTestController {

    private final ChunkEmbeddingService chunkEmbeddingService;
    private final HybridSearchService hybridSearchService;
    private final OllamaChatService ollamaChatService;
    private final RagClaimEvidenceService ragClaimEvidenceService;

    public RagContextTestController(
            ChunkEmbeddingService chunkEmbeddingService,
            HybridSearchService hybridSearchService,
            OllamaChatService ollamaChatService,
            RagClaimEvidenceService ragClaimEvidenceService
    ) {
        this.chunkEmbeddingService = chunkEmbeddingService;
        this.hybridSearchService = hybridSearchService;
        this.ollamaChatService = ollamaChatService;
        this.ragClaimEvidenceService = ragClaimEvidenceService;
    }

    @GetMapping("/ask")
    public String ask(
            @RequestParam String query
    ) {

        // =====================================================
        // 1. Generate query embedding
        // =====================================================

        float[] queryEmbedding =
                chunkEmbeddingService.generateEmbedding(query);


        // =====================================================
        // 2. Retrieve relevant repository chunks
        // =====================================================

        List<HybridSearchResult> results =
                hybridSearchService.search(
                        query,
                        queryEmbedding,
                        5
                );


        if (results.isEmpty()) {

            return """
                    ANSWER -----
                    I couldn't find any relevant code in the repository.
                    """;
        }


        // =====================================================
        // 3. Build repository context
        // =====================================================

        StringBuilder context =
                new StringBuilder();

        Set<String> uniqueSources =
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

            uniqueSources.add(source);


            context.append("=== REPOSITORY SOURCE ===\n");

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

            context.append("Relevance Score: ")
                    .append(result.getFinalScore())
                    .append("\n");

            context.append("Keyword Match: ")
                    .append(result.getKeywordMatch())
                    .append("\n");

            context.append("CODE:\n");

            context.append(result.getContent())
                    .append("\n\n");
        }


        // =====================================================
        // 4. Ask local LLM
        // =====================================================

        String prompt = """
                You are a repository-aware Java programming assistant.

                Answer the user's question using ONLY the provided
                repository code context.

                STRICT RULES:

                1. Do not use outside Java knowledge.
                2. Do not invent classes, methods, variables,
                   behavior, libraries, or features.
                3. Every factual statement must be supported
                   by the repository context.
                4. Keep the answer concise.
                5. Do not add general Java explanations unless
                   they are directly supported by the repository.
                6. Do not mention information that cannot be
                   verified from the repository context.
                7. If the repository does not provide enough
                   information, say so.

                REPOSITORY CODE CONTEXT:

                %s

                USER QUESTION:

                %s
                """.formatted(
                context,
                query
        );


        String generatedAnswer =
                ollamaChatService.ask(prompt);


        if (generatedAnswer == null
                || generatedAnswer.isBlank()) {

            return """
                    ANSWER -----
                    The model did not generate an answer.
                    """;
        }


        // =====================================================
        // 5. Split generated answer into individual claims
        // =====================================================

        List<String> claims =
                splitIntoClaims(generatedAnswer);


        // =====================================================
        // 6. Validate every claim independently
        // =====================================================

        List<String> verifiedClaims =
                new ArrayList<>();

        double totalEvidenceScore = 0.0;

        int verifiedCount = 0;


        for (String claim : claims) {

            if (claim.isBlank()) {
                continue;
            }


            RagClaimEvidenceResult evidence =
                    ragClaimEvidenceService.checkClaim(
                            claim,
                            context.toString()
                    );


            if (evidence.isSupported()) {

                verifiedClaims.add(
                        claim.trim()
                );

                totalEvidenceScore +=
                        evidence.getEvidenceScore();

                verifiedCount++;
            }
        }


        // =====================================================
        // 7. Build final grounded answer
        // =====================================================

        StringBuilder response =
                new StringBuilder();

        response.append("ANSWER -----\n");


        if (verifiedClaims.isEmpty()) {

            response.append(
                    "The provided repository context is not "
                            + "sufficient to answer this question."
            );

        } else {

            for (String claim : verifiedClaims) {

                response.append(claim)
                        .append(" ");
            }
        }


        // =====================================================
        // 8. Grounding information
        // =====================================================

        response.append("\n\nGROUNDING -----\n");

        /*
         * The final answer contains ONLY verified claims.
         *
         * Therefore, if at least one verified claim exists,
         * the answer we actually return to the user is grounded.
         *
         * Unsupported claims generated by the LLM are filtered out.
         */

        boolean grounded =
                !verifiedClaims.isEmpty();


        response.append("Grounded: ")
                .append(grounded)
                .append("\n");

        response.append("Evidence-Verified Sentences: ")
                .append(verifiedCount)
                .append("\n");

        int filteredClaims =
                claims.size() - verifiedCount;

        response.append("Filtered Unsupported Claims: ")
                .append(filteredClaims)
                .append("\n");


        if (verifiedCount > 0) {

            double averageScore =
                    totalEvidenceScore
                            / verifiedCount;

            response.append("Average Evidence Score: ")
                    .append(
                            String.format(
                                    "%.2f",
                                    averageScore
                            )
                    )
                    .append("\n");
        }


        // =====================================================
        // 9. Sources
        // =====================================================

        response.append("\nSOURCES -----\n");

        int sourceNumber = 1;

        for (String source : uniqueSources) {

            response.append(sourceNumber)
                    .append(". ")
                    .append(source)
                    .append("\n");

            sourceNumber++;
        }


        return response.toString();
    }


    // =========================================================
    // CLAIM SPLITTER
    // =========================================================

    private List<String> splitIntoClaims(
            String answer
    ) {

        List<String> claims =
                new ArrayList<>();


        String cleaned =
                answer
                        .replace(
                                "\r",
                                " "
                        )
                        .replace(
                                "\n",
                                " "
                        )
                        .replaceAll(
                                "\\s+",
                                " "
                        )
                        .trim();


        if (cleaned.isBlank()) {
            return claims;
        }


        /*
         * Split normal sentences.
         */

        String[] sentences =
                cleaned.split(
                        "(?<=[.!?])\\s+"
                );


        for (String sentence : sentences) {

            String claim =
                    sentence.trim();


            /*
             * Remove common answer prefixes.
             */

            claim =
                    claim.replaceFirst(
                            "(?i)^answer\\s*[:\\-]*\\s*",
                            ""
                    );


            /*
             * Remove bullet prefixes.
             */

            claim =
                    claim.replaceFirst(
                            "^[-•*]\\s*",
                            ""
                    );


            if (claim.length() >= 5) {

                claims.add(claim);
            }
        }


        return claims;
    }
}