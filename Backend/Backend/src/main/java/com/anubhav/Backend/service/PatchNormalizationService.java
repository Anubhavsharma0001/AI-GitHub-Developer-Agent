package com.anubhav.Backend.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PatchNormalizationService {

    public NormalizationResult normalize(
            String patch,
            String repositoryRelativePath
    ) {

        if (patch == null || patch.isBlank()) {
            return new NormalizationResult(
                    false,
                    "Patch is empty.",
                    null
            );
        }

        String normalized =
                patch
                        .replace("\r\n", "\n")
                        .replace("\r", "\n")
                        .trim();

        // =====================================================
        // STEP 1: FIND THE ACTUAL DIFF
        // =====================================================

        int diffIndex =
                findDiffStart(normalized);

        if (diffIndex < 0) {

            return new NormalizationResult(
                    false,
                    "No unified Git diff was found.",
                    null
            );
        }

        normalized =
                normalized.substring(diffIndex);

        // =====================================================
        // STEP 2: REMOVE MARKDOWN CODE FENCES
        // =====================================================

        normalized =
                removeCodeFences(normalized);

        // =====================================================
        // STEP 3: FIX COMMON AI TYPO
        // =====================================================

        normalized =
                fixDiffHeader(normalized);

        // =====================================================
        // STEP 4: NORMALIZE FILE PATHS
        // =====================================================

        normalized =
                normalizeFilePaths(
                        normalized,
                        repositoryRelativePath
                );

        // =====================================================
        // STEP 5: REMOVE TRAILING AI TEXT
        // =====================================================

        normalized =
                removeTrailingSections(normalized);

        normalized =
                normalized.trim();

        // =====================================================
        // STEP 6: VALIDATE BASIC DIFF STRUCTURE
        // =====================================================

        ValidationResult validation =
                validatePatch(normalized);

        if (!validation.valid()) {

            return new NormalizationResult(
                    false,
                    validation.message(),
                    null
            );
        }

        return new NormalizationResult(
                true,
                "Patch normalized successfully.",
                normalized
        );
    }

    // =========================================================
    // FIND DIFF START
    // =========================================================

    private int findDiffStart(
            String patch
    ) {

        int normalIndex =
                patch.indexOf("diff --git ");

        if (normalIndex >= 0) {
            return normalIndex;
        }

        /*
         * Some local models occasionally produce:
         *
         * iff --git
         *
         * instead of:
         *
         * diff --git
         */

        int malformedIndex =
                patch.indexOf("iff --git ");

        if (malformedIndex >= 0) {
            return malformedIndex;
        }

        return -1;
    }

    // =========================================================
    // REMOVE CODE FENCES
    // =========================================================

    private String removeCodeFences(
            String patch
    ) {

        patch =
                patch.replace(
                        "```diff",
                        ""
                );

        patch =
                patch.replace(
                        "```",
                        ""
                );

        return patch.trim();
    }

    // =========================================================
    // FIX DIFF HEADER
    // =========================================================

    private String fixDiffHeader(
            String patch
    ) {

        String[] lines =
                patch.split("\n", -1);

        StringBuilder result =
                new StringBuilder();

        for (String line : lines) {

            if (line.startsWith("iff --git ")) {

                line =
                        "d" + line;
            }

            result.append(line)
                    .append("\n");
        }

        return result
                .toString()
                .trim();
    }

    // =========================================================
    // NORMALIZE FILE PATHS
    // =========================================================

    private String normalizeFilePaths(
            String patch,
            String repositoryRelativePath
    ) {

        if (repositoryRelativePath == null
                || repositoryRelativePath.isBlank()) {

            return patch;
        }

        String normalizedPath =
                repositoryRelativePath
                        .replace("\\", "/")
                        .replaceFirst("^/+", "");

        String[] lines =
                patch.split("\n", -1);

        StringBuilder result =
                new StringBuilder();

        for (String line : lines) {

            // -------------------------------------------------
            // diff --git line
            // -------------------------------------------------

            if (line.startsWith("diff --git ")) {

                line =
                        normalizeDiffGitLine(
                                line,
                                normalizedPath
                        );
            }

            // -------------------------------------------------
            // --- a/file
            // -------------------------------------------------

            else if (line.startsWith("--- a/")) {

                line =
                        "--- a/"
                                + normalizedPath;
            }

            // -------------------------------------------------
            // +++ b/file
            // -------------------------------------------------

            else if (line.startsWith("+++ b/")) {

                line =
                        "+++ b/"
                                + normalizedPath;
            }

            result.append(line)
                    .append("\n");
        }

        return result
                .toString()
                .trim();
    }

    // =========================================================
    // NORMALIZE diff --git LINE
    // =========================================================

    private String normalizeDiffGitLine(
            String line,
            String repositoryRelativePath
    ) {

        return "diff --git a/"
                + repositoryRelativePath
                + " b/"
                + repositoryRelativePath;
    }

    // =========================================================
    // REMOVE TRAILING AI SECTIONS
    // =========================================================

    private String removeTrailingSections(
            String patch
    ) {

        String[] lines =
                patch.split("\n", -1);

        StringBuilder result =
                new StringBuilder();

        for (String line : lines) {

            String trimmed =
                    line.trim();

            /*
             * AI sometimes continues with:
             *
             * TESTS
             * CHANGE SUMMARY
             * FILES CHANGED
             *
             * after the diff.
             */

            if (trimmed.equals("TESTS")
                    || trimmed.equals("TESTS:")
                    || trimmed.equals("CHANGE SUMMARY")
                    || trimmed.equals("CHANGE SUMMARY:")
                    || trimmed.equals("FILES CHANGED")
                    || trimmed.equals("FILES CHANGED:")) {

                break;
            }

            result.append(line)
                    .append("\n");
        }

        return result
                .toString()
                .trim();
    }

    // =========================================================
    // VALIDATE PATCH
    // =========================================================

    private ValidationResult validatePatch(
            String patch
    ) {

        if (patch == null || patch.isBlank()) {

            return new ValidationResult(
                    false,
                    "Normalized patch is empty."
            );
        }

        if (!patch.startsWith("diff --git ")) {

            return new ValidationResult(
                    false,
                    "Normalized patch does not start with 'diff --git'."
            );
        }

        if (!patch.contains("\n--- a/")) {

            return new ValidationResult(
                    false,
                    "Normalized patch does not contain a valid '--- a/' header."
            );
        }

        if (!patch.contains("\n+++ b/")) {

            return new ValidationResult(
                    false,
                    "Normalized patch does not contain a valid '+++ b/' header."
            );
        }

        if (!patch.contains("\n@@")) {

            return new ValidationResult(
                    false,
                    "Normalized patch does not contain a valid hunk header."
            );
        }

        /*
         * Every actual diff line inside a hunk should normally
         * begin with:
         *
         * ' '  context
         * '+'  addition
         * '-'  deletion
         *
         * We don't enforce this globally because Git headers,
         * metadata and special lines are also valid.
         */

        return new ValidationResult(
                true,
                "Patch structure is valid."
        );
    }

    // =========================================================
    // RESULT
    // =========================================================

    public record NormalizationResult(
            boolean success,
            String message,
            String normalizedPatch
    ) {
    }

    private record ValidationResult(
            boolean valid,
            String message
    ) {
    }
}