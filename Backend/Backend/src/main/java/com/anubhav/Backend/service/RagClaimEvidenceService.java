package com.anubhav.Backend.service;

import com.anubhav.Backend.dto.RagClaimEvidenceResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RagClaimEvidenceService {

    /*
     * Minimum score required for a claim
     * to be considered supported.
     */
    private static final double SUPPORT_THRESHOLD = 0.55;

    private static final Pattern JAVA_IDENTIFIER_PATTERN =
            Pattern.compile(
                    "\\b[A-Za-z_$][A-Za-z0-9_$]*\\b"
            );

    private static final Pattern METHOD_CALL_PATTERN =
            Pattern.compile(
                    "\\b[A-Za-z_$][A-Za-z0-9_$]*\\s*\\("
            );

    public RagClaimEvidenceResult checkClaim(
            String claim,
            String repositoryContext
    ) {

        if (claim == null
                || claim.isBlank()
                || repositoryContext == null
                || repositoryContext.isBlank()) {

            return unsupportedResult();
        }

        List<String> clauses =
                splitIntoClauses(claim);

        if (clauses.isEmpty()) {
            return unsupportedResult();
        }

        List<String> matchedEvidence =
                new ArrayList<>();

        double totalScore = 0.0;

        int supportedClauses = 0;

        for (String clause : clauses) {

            ClauseEvidence evidence =
                    evaluateClause(
                            clause,
                            repositoryContext
                    );

            totalScore += evidence.score();

            matchedEvidence.addAll(
                    evidence.matchedEvidence()
            );

            if (evidence.supported()) {
                supportedClauses++;
            }
        }

        double finalScore =
                totalScore / clauses.size();

        /*
         * Every meaningful clause must be supported.
         */
        boolean supported =
                supportedClauses == clauses.size()
                        && finalScore >= SUPPORT_THRESHOLD;

        return new RagClaimEvidenceResult(
                supported,
                finalScore,
                new ArrayList<>(
                        new HashSet<>(matchedEvidence)
                )
        );
    }

    // =====================================================
    // CLAUSE EVALUATION
    // =====================================================

    private ClauseEvidence evaluateClause(
            String clause,
            String repositoryContext
    ) {

        String normalizedClaim =
                normalize(clause);

        String normalizedContext =
                normalize(repositoryContext);

        if (normalizedClaim.isBlank()) {

            return new ClauseEvidence(
                    false,
                    0.0,
                    List.of()
            );
        }

        /*
         * 1. Exact phrase match
         *
         * This is the strongest evidence.
         */
        boolean exactPhrase =
                normalizedClaim.length() >= 15
                        && normalizedContext.contains(
                        normalizedClaim
                );

        double phraseScore =
                exactPhrase ? 1.0 : 0.0;

        /*
         * 2. Important word overlap
         */
        Set<String> claimTerms =
                extractImportantTerms(
                        normalizedClaim
                );

        Set<String> contextTerms =
                extractImportantTerms(
                        normalizedContext
                );

        double termScore =
                calculateOverlap(
                        claimTerms,
                        contextTerms
                );

        /*
         * 3. Java identifier overlap
         */
        Set<String> claimIdentifiers =
                extractJavaIdentifiers(clause);

        Set<String> contextIdentifiers =
                extractJavaIdentifiers(
                        repositoryContext
                );

        double identifierScore =
                calculateOverlap(
                        claimIdentifiers,
                        contextIdentifiers
                );

        /*
         * 4. Method overlap
         */
        Set<String> claimMethods =
                extractMethodNames(clause);

        Set<String> contextMethods =
                extractMethodNames(
                        repositoryContext
                );

        double methodScore =
                calculateOverlap(
                        claimMethods,
                        contextMethods
                );

        /*
         * 5. Calculate final evidence score.
         */
        double evidenceScore;

        if (phraseScore == 1.0) {

            evidenceScore = 1.0;

        } else if (!claimMethods.isEmpty()) {

            evidenceScore =
                    (termScore * 0.30)
                            + (identifierScore * 0.20)
                            + (methodScore * 0.50);

        } else if (!claimIdentifiers.isEmpty()) {

            evidenceScore =
                    (termScore * 0.40)
                            + (identifierScore * 0.60);

        } else {

            evidenceScore =
                    termScore;
        }

        /*
         * Prevent generic word overlap from becoming
         * strong evidence for long claims.
         */
        if (claimTerms.size() >= 3) {

            int matchedTerms = 0;

            for (String term : claimTerms) {

                if (contextTerms.contains(term)) {
                    matchedTerms++;
                }
            }

            if (matchedTerms < 2
                    && evidenceScore > 0.40) {

                evidenceScore = 0.40;
            }
        }

        /*
         * Collect the actual matching evidence.
         */
        Set<String> matched =
                new HashSet<>();

        for (String term : claimTerms) {

            if (contextTerms.contains(term)) {
                matched.add(term);
            }
        }

        for (String identifier : claimIdentifiers) {

            if (contextIdentifiers.contains(identifier)) {
                matched.add(identifier);
            }
        }

        for (String method : claimMethods) {

            if (contextMethods.contains(method)) {

                matched.add(
                        method + "()"
                );
            }
        }

        if (exactPhrase) {

            matched.add(
                    "EXACT_PHRASE_MATCH"
            );
        }

        boolean supported =
                evidenceScore >= SUPPORT_THRESHOLD;

        return new ClauseEvidence(
                supported,
                evidenceScore,
                new ArrayList<>(matched)
        );
    }

    // =====================================================
    // CLAUSE SPLITTING
    // =====================================================

    private List<String> splitIntoClauses(
            String claim
    ) {

        String cleaned =
                claim
                        .replaceAll(
                                "\\s+",
                                " "
                        )
                        .trim();

        if (cleaned.isBlank()) {
            return List.of();
        }

        String[] parts =
                cleaned.split(
                        "(?i)\\s+(?:and|but|while|whereas)\\s+"
                );

        List<String> clauses =
                new ArrayList<>();

        for (String part : parts) {

            String clause =
                    part.trim();

            clause =
                    clause.replaceFirst(
                            "(?i)^(also|additionally|however)\\s+",
                            ""
                    );

            if (clause.length() >= 5) {
                clauses.add(clause);
            }
        }

        return clauses;
    }

    // =====================================================
    // NORMALIZATION
    // =====================================================

    private String normalize(
            String text
    ) {

        return text
                .toLowerCase()
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    // =====================================================
    // IMPORTANT TERMS
    // =====================================================

    private Set<String> extractImportantTerms(
            String text
    ) {

        String normalized =
                text.toLowerCase()
                        .replaceAll(
                                "[^a-zA-Z0-9_$]",
                                " "
                        );

        Set<String> stopWords =
                Set.of(
                        "the",
                        "a",
                        "an",
                        "is",
                        "are",
                        "was",
                        "were",
                        "be",
                        "to",
                        "of",
                        "and",
                        "or",
                        "in",
                        "on",
                        "for",
                        "with",
                        "as",
                        "by",
                        "this",
                        "that",
                        "it",
                        "they",
                        "them",
                        "can",
                        "could",
                        "will",
                        "would",
                        "should",
                        "used",
                        "use",
                        "using",
                        "provide",
                        "provides",
                        "provided",
                        "allow",
                        "allows",
                        "work",
                        "works",
                        "does",
                        "do",
                        "how",
                        "what",
                        "why",
                        "where",
                        "which",
                        "from",
                        "into",
                        "their",
                        "these",
                        "those",
                        "like"
                );

        Set<String> terms =
                new HashSet<>();

        Arrays.stream(
                        normalized.split("\\s+")
                )
                .map(String::trim)
                .filter(
                        term ->
                                !term.isBlank()
                )
                .filter(
                        term ->
                                term.length() >= 3
                )
                .filter(
                        term ->
                                !stopWords.contains(term)
                )
                .forEach(terms::add);

        return terms;
    }

    // =====================================================
    // JAVA IDENTIFIERS
    // =====================================================

    private Set<String> extractJavaIdentifiers(
            String text
    ) {

        Set<String> identifiers =
                new HashSet<>();

        Matcher matcher =
                JAVA_IDENTIFIER_PATTERN.matcher(
                        text
                );

        Set<String> ignoredWords =
                Set.of(
                        "the",
                        "this",
                        "that",
                        "with",
                        "from",
                        "into",
                        "using",
                        "class",
                        "method",
                        "methods",
                        "object",
                        "objects",
                        "primitive",
                        "primitives",
                        "data",
                        "type",
                        "types",
                        "value",
                        "values",
                        "java",
                        "wrapper",
                        "wrappers",
                        "features",
                        "feature"
                );

        while (matcher.find()) {

            String identifier =
                    matcher.group();

            if (identifier.length() < 3) {
                continue;
            }

            if (ignoredWords.contains(
                    identifier.toLowerCase()
            )) {
                continue;
            }

            /*
             * Strong Java identifiers usually look like:
             *
             * Integer
             * String
             * System
             * Scanner
             * MAX_VALUE
             *
             * We avoid treating every English word
             * as a Java identifier.
             */
            boolean looksLikeIdentifier =
                    Character.isUpperCase(
                            identifier.charAt(0)
                    )
                            || identifier.contains("_")
                            || identifier.contains("$");

            if (looksLikeIdentifier) {
                identifiers.add(identifier);
            }
        }

        return identifiers;
    }

    // =====================================================
    // METHOD NAMES
    // =====================================================

    private Set<String> extractMethodNames(
            String text
    ) {

        Set<String> methods =
                new HashSet<>();

        Matcher matcher =
                METHOD_CALL_PATTERN.matcher(
                        text
                );

        while (matcher.find()) {

            String method =
                    matcher.group()
                            .replace(
                                    "(",
                                    ""
                            )
                            .trim();

            if (method.length() >= 2) {
                methods.add(method);
            }
        }

        return methods;
    }

    // =====================================================
    // OVERLAP
    // =====================================================

    private double calculateOverlap(
            Set<String> claimTerms,
            Set<String> contextTerms
    ) {

        if (claimTerms.isEmpty()) {
            return 0.0;
        }

        int matches = 0;

        for (String term : claimTerms) {

            if (contextTerms.contains(term)) {
                matches++;
            }
        }

        return (double) matches
                / claimTerms.size();
    }

    // =====================================================
    // UNSUPPORTED RESULT
    // =====================================================

    private RagClaimEvidenceResult unsupportedResult() {

        return new RagClaimEvidenceResult(
                false,
                0.0,
                List.of()
        );
    }

    // =====================================================
    // INTERNAL RECORD
    // =====================================================

    private record ClauseEvidence(
            boolean supported,
            double score,
            List<String> matchedEvidence
    ) {
    }
}