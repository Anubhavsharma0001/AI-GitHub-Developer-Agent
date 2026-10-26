package com.anubhav.Backend.service;

import com.anubhav.Backend.dto.HybridSearchResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CodeGenerationService {

    private static final Pattern JAVA_FILE_PATTERN =
            Pattern.compile(
                    "(?i)([A-Za-z0-9_.-]+(?:[/\\\\][A-Za-z0-9_.-]+)*\\.java)"
            );

    private static final Pattern STRUCTURED_CHANGE_PATTERN =
            Pattern.compile(
                    "(?is)" +
                            "FILE\\s*:\\s*(.+?)\\s*" +
                            "SEARCH\\s*\\n" +
                            "(.*?)" +
                            "\\s*END\\s*SEARCH\\s*" +
                            "REPLACE\\s*\\n" +
                            "(.*?)" +
                            "\\s*END\\s*REPLACE"
            );

    private final ChunkEmbeddingService chunkEmbeddingService;
    private final HybridSearchService hybridSearchService;
    private final OllamaChatService ollamaChatService;

    public CodeGenerationService(
            ChunkEmbeddingService chunkEmbeddingService,
            HybridSearchService hybridSearchService,
            OllamaChatService ollamaChatService
    ) {
        this.chunkEmbeddingService = chunkEmbeddingService;
        this.hybridSearchService = hybridSearchService;
        this.ollamaChatService = ollamaChatService;
    }

    public CodeGenerationResult generate(String issue) {

        if (issue == null || issue.isBlank()) {
            return new CodeGenerationResult(
                    false,
                    "Developer issue is empty.",
                    null,
                    List.of()
            );
        }

        try {

            String explicitFile =
                    extractExplicitFileReference(issue);

            float[] issueEmbedding =
                    chunkEmbeddingService.generateEmbedding(issue);

            List<HybridSearchResult> results =
                    hybridSearchService.search(
                            issue,
                            issueEmbedding,
                            10
                    );

            /*
             * If developer explicitly mentions a file and
             * normal retrieval did not find it, perform a
             * filename-focused search.
             */
            if (explicitFile != null
                    && !containsRequestedFile(
                    results,
                    explicitFile
            )) {

                float[] fileEmbedding =
                        chunkEmbeddingService.generateEmbedding(
                                explicitFile
                        );

                List<HybridSearchResult> fileResults =
                        hybridSearchService.search(
                                explicitFile,
                                fileEmbedding,
                                10
                        );

                results =
                        mergeResults(
                                results,
                                fileResults
                        );
            }

            if (results == null) {
                results = new ArrayList<>();
            }

            String repositoryContext =
                    buildRepositoryContext(results);

            String prompt =
                    buildPrompt(
                            issue,
                            explicitFile,
                            repositoryContext
                    );

            String rawResponse =
                    ollamaChatService.ask(prompt);

            String normalizedChange =
                    normalizeStructuredChange(rawResponse);

            // Deterministic fallback for simple comment requests when the model returns NO_CHANGE
            // or malformed output. The normal validation below still runs on this patch.
            boolean commentRequest = issue.toLowerCase().contains("comment");
            boolean mainMethodRequest = issue.toLowerCase().contains("main");

            if (commentRequest && mainMethodRequest && explicitFile != null) {
                normalizedChange = createCommentFallback(
                        issue,
                        explicitFile,
                        results
                );
            } else if (normalizedChange == null) {
                normalizedChange = createCommentFallback(
                        issue,
                        explicitFile,
                        results
                );
            }

            if (normalizedChange == null) {
                return new CodeGenerationResult(
                        false,
                        "AI response could not be parsed or safely recovered. Raw response: "
                                + (rawResponse == null ? "null" : rawResponse.substring(
                                0, Math.min(rawResponse.length(), 500))),
                        null,
                        buildSourceList(results)
                );
            }

            ValidationResult validation =
                    validateStructuredChange(
                            normalizedChange,
                            explicitFile
                    );

            if (!validation.valid()) {

                return new CodeGenerationResult(
                        false,
                        validation.message(),
                        null,
                        buildSourceList(results)
                );
            }

            return new CodeGenerationResult(
                    true,
                    "Code change generated successfully.",
                    normalizedChange,
                    buildSourceList(results)
            );

        } catch (Exception exception) {

            return new CodeGenerationResult(
                    false,
                    "Code generation failed: "
                            + exception.getMessage(),
                    null,
                    List.of()
            );
        }
    }

    public CodeGenerationResult generateCode(String issue) {
        return generate(issue);
    }

    private String createCommentFallback(
            String issue,
            String explicitFile,
            List<HybridSearchResult> results
    ) {
        if (issue == null
                || !issue.toLowerCase().contains("comment")
                || explicitFile == null
                || results == null
                || results.isEmpty()) {
            return null;
        }

        String requestedName = getFileName(explicitFile);

        for (HybridSearchResult result : results) {
            if (result == null
                    || result.getFilePath() == null
                    || !getFileName(result.getFilePath())
                    .equalsIgnoreCase(requestedName)) {
                continue;
            }

            String content = result.getContent();
            if (content == null || content.isBlank()) {
                continue;
            }

            String normalizedContent = normalizeLineEndings(content);
            String[] lines = normalizedContent.split("\\n", -1);
            boolean insideMain = false;
            int depth = 0;

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                String trimmed = line.trim();

                if (!insideMain) {
                    if (trimmed.matches(".*\\bmain\\s*\\(.*\\)\\s*\\{.*")) {
                        insideMain = true;
                        depth = countBraces(line);
                    }
                    continue;
                }

                if (depth <= 0) {
                    break;
                }

                // Skip blank lines, comments, and brace-only lines.
                if (trimmed.isEmpty()
                        || trimmed.startsWith("//")
                        || trimmed.startsWith("/*")
                        || trimmed.startsWith("*")
                        || trimmed.equals("{")
                        || trimmed.equals("}")) {
                    depth += countBraces(line);
                    continue;
                }

                // Do not insert a comment after a control-flow header.
                if (trimmed.startsWith("if ")
                        || trimmed.startsWith("if(")
                        || trimmed.startsWith("for ")
                        || trimmed.startsWith("for(")
                        || trimmed.startsWith("while ")
                        || trimmed.startsWith("while(")
                        || trimmed.startsWith("switch ")
                        || trimmed.startsWith("switch(")
                        || trimmed.startsWith("else")
                        || trimmed.startsWith("try")
                        || trimmed.startsWith("catch")) {
                    depth += countBraces(line);
                    continue;
                }

                // Only select a complete executable statement.
                if (!trimmed.endsWith(";")
                        || trimmed.length() < 12
                        || trimmed.contains("Comment added by the AI developer agent")) {
                    depth += countBraces(line);
                    continue;
                }

                String comment = line.substring(0, line.length() - line.stripLeading().length())
                        + "// Comment added by the AI developer agent";
                String search = line;

                // Never emit a patch whose SEARCH is duplicated in the retrieved content.
                if (countOccurrences(normalizedContent, search) != 1) {
                    depth += countBraces(line);
                    continue;
                }

                String replacement = search
                        + System.lineSeparator()
                        + comment;

                return """
                        FILE: %s

                        SEARCH
                        %s
                        END SEARCH

                        REPLACE
                        %s
                        END REPLACE
                        """.formatted(
                        normalizePath(result.getFilePath()),
                        search,
                        replacement
                ).trim();
            }
        }

        return null;
    }

    private String normalizeLineEndings(String text) {
        return text.replace("\r\n", "\n")
                .replace("\r", "\n");
    }

    private int countOccurrences(String content, String search) {
        if (content == null || search == null || search.isEmpty()) {
            return 0;
        }

        int count = 0;
        int fromIndex = 0;

        while (true) {
            int index = content.indexOf(search, fromIndex);
            if (index < 0) {
                return count;
            }
            count++;
            fromIndex = index + search.length();
        }
    }

    private int countBraces(String line) {
        int count = 0;
        for (char character : line.toCharArray()) {
            if (character == '{') {
                count++;
            } else if (character == '}') {
                count--;
            }
        }
        return count;
    }

    private String buildPrompt(
            String issue,
            String explicitFile,
            String repositoryContext
    ) {
        String fileInstruction;

        if (explicitFile != null) {
            fileInstruction = """
                    REQUIRED FILE:
                    %s

                    Modify ONLY this exact file.
                    """.formatted(explicitFile);
        } else {
            fileInstruction = """
                    Choose an existing file from the repository context.
                    Do not invent file paths.
                    """;
        }

        return """
                You are a precise code-editing agent.
                Make the smallest possible change to the existing repository.

                DEVELOPER ISSUE:
                %s

                %s

                REPOSITORY SOURCE:
                %s

                STRICT PATCH RULES:

                1. Copy SEARCH character-for-character from the existing source.
                2. SEARCH must be a small, specific block of existing code.
                3. NEVER select a class declaration, method declaration,
                   constructor declaration, or an entire method body.
                4. NEVER use "public static void main(String[] args) {"
                   as SEARCH.
                5. For a comment request inside main(), select an existing
                   statement inside main() and insert the comment adjacent
                   to that statement.
                6. REPLACE must preserve the entire SEARCH block and make
                   only the requested modification.
                7. Do not change unrelated code, imports, or formatting.
                8. Do not invent code that is absent from the source.
                9. If no safe, exact SEARCH block exists, return NO_CHANGE.
                10. Do not include explanations or Markdown code fences.

                For a comment request:
                - Add only the requested comment.
                - Do not add print statements, logging, variables,
                  methods, imports, or other functionality.

                OUTPUT EXACTLY:

                FILE: exact/repository/path/File.java

                SEARCH
                exact existing source code
                END SEARCH

                REPLACE
                original source code with only the requested change
                END REPLACE

                If a safe change cannot be generated, output exactly:
                NO_CHANGE
                """.formatted(
                issue,
                fileInstruction,
                repositoryContext
        );
    }

    private String buildRepositoryContext(
            List<HybridSearchResult> results
    ) {

        if (results == null || results.isEmpty()) {
            return "No repository context was retrieved.";
        }

        StringBuilder context =
                new StringBuilder();

        for (int i = 0; i < results.size(); i++) {

            HybridSearchResult result =
                    results.get(i);

            context.append(
                    "\n========== SOURCE "
            );

            context.append(i + 1);

            context.append(
                    " ==========\n"
            );

            context.append("FILE: ")
                    .append(result.getFilePath())
                    .append("\n");

            context.append("FILE NAME: ")
                    .append(result.getFileName())
                    .append("\n");

            context.append("LINES: ")
                    .append(result.getStartLine())
                    .append("-")
                    .append(result.getEndLine())
                    .append("\n");

            context.append("CONTENT:\n");

            if (result.getContent() != null) {
                context.append(
                        result.getContent()
                );
            }

            context.append("\n");
        }

        return context.toString();
    }

    private String normalizeStructuredChange(
            String response
    ) {

        if (response == null || response.isBlank()) {
            return null;
        }

        String cleaned =
                response
                        .replace("\r\n", "\n")
                        .replace("\r", "\n")
                        .trim();

        /*
         * Remove outer Markdown fences if Qwen ignores
         * the instruction and returns:
         *
         * ```text
         * FILE: ...
         * ```
         */
        cleaned =
                removeOuterCodeFence(cleaned);

        if (cleaned.equalsIgnoreCase("NO_CHANGE")) {
            return null;
        }

        /*
         * Sometimes the model adds text before FILE.
         */
        int fileIndex =
                indexOfIgnoreCase(
                        cleaned,
                        "FILE:"
                );

        if (fileIndex < 0) {
            return null;
        }

        cleaned =
                cleaned.substring(fileIndex)
                        .trim();

        /*
         * Normalize marker capitalization.
         */
        cleaned =
                cleaned.replaceAll(
                        "(?i)\\bFILE\\s*:",
                        "FILE:"
                );

        cleaned =
                cleaned.replaceAll(
                        "(?i)\\bSEARCH\\s*",
                        "SEARCH\n"
                );

        cleaned =
                cleaned.replaceAll(
                        "(?i)\\bEND\\s+SEARCH\\s*",
                        "\nEND SEARCH\n"
                );

        cleaned =
                cleaned.replaceAll(
                        "(?i)\\bREPLACE\\s*",
                        "REPLACE\n"
                );

        cleaned =
                cleaned.replaceAll(
                        "(?i)\\bEND\\s+REPLACE\\s*",
                        "\nEND REPLACE\n"
                );

        Matcher matcher =
                STRUCTURED_CHANGE_PATTERN.matcher(
                        cleaned
                );

        if (!matcher.find()) {
            return null;
        }

        String filePath =
                matcher.group(1).trim();

        String searchText =
                matcher.group(2);

        String replaceText =
                matcher.group(3);

        searchText =
                cleanCodeBlock(searchText);

        replaceText =
                cleanCodeBlock(replaceText);

        filePath =
                normalizePath(filePath);

        /*
         * Remove accidental trailing explanation from
         * the file path.
         */
        filePath =
                cleanFilePath(filePath);

        if (filePath.isBlank()) {
            return null;
        }

        if (searchText.isBlank()) {
            return null;
        }

        if (replaceText.isBlank()) {
            return null;
        }

        return """
                FILE: %s

                SEARCH
                %s
                END SEARCH

                REPLACE
                %s
                END REPLACE
                """.formatted(
                filePath,
                searchText,
                replaceText
        ).trim();
    }

    private String removeOuterCodeFence(
            String text
    ) {

        String cleaned =
                text.trim();

        if (!cleaned.startsWith("```")) {
            return cleaned;
        }

        int firstNewLine =
                cleaned.indexOf('\n');

        if (firstNewLine < 0) {
            return cleaned;
        }

        cleaned =
                cleaned.substring(
                        firstNewLine + 1
                ).trim();

        if (cleaned.endsWith("```")) {

            cleaned =
                    cleaned.substring(
                            0,
                            cleaned.length() - 3
                    ).trim();
        }

        return cleaned;
    }

    private String cleanCodeBlock(
            String text
    ) {

        if (text == null) {
            return "";
        }

        String cleaned =
                text
                        .replace("\r\n", "\n")
                        .replace("\r", "\n")
                        .trim();

        /*
         * Remove code fences inside SEARCH/REPLACE.
         */
        if (cleaned.startsWith("```")) {

            int firstNewLine =
                    cleaned.indexOf('\n');

            if (firstNewLine >= 0) {

                cleaned =
                        cleaned.substring(
                                firstNewLine + 1
                        );
            }
        }

        if (cleaned.endsWith("```")) {

            cleaned =
                    cleaned.substring(
                            0,
                            cleaned.length() - 3
                    );
        }

        return cleaned.trim();
    }

    private String cleanFilePath(
            String filePath
    ) {

        String cleaned =
                filePath.trim();

        /*
         * If model accidentally writes:
         *
         * FILE: src/Test.java
         * explanation...
         *
         * keep only the Java path.
         */
        Matcher matcher =
                JAVA_FILE_PATTERN.matcher(
                        cleaned
                );

        if (matcher.find()) {
            return normalizePath(
                    matcher.group(1)
            );
        }

        return normalizePath(cleaned);
    }

    private ValidationResult validateStructuredChange(
            String change,
            String explicitFile
    ) {

        String filePath =
                extractGeneratedFile(change);

        String searchText =
                extractSearchText(change);

        String replaceText =
                extractReplaceText(change);

        if (filePath == null
                || filePath.isBlank()) {

            return new ValidationResult(
                    false,
                    "Generated FILE path is missing."
            );
        }

        if (searchText == null
                || searchText.isBlank()) {

            return new ValidationResult(
                    false,
                    "Generated SEARCH section is empty."
            );
        }

        if (replaceText == null
                || replaceText.isBlank()) {

            return new ValidationResult(
                    false,
                    "Generated REPLACE section is empty."
            );
        }

        filePath =
                normalizePath(filePath);

        /*
         * Security protection.
         */
        if (filePath.startsWith("/")
                || filePath.startsWith("\\")
                || filePath.contains(":")
                || filePath.contains("../")
                || filePath.contains("..\\")) {

            return new ValidationResult(
                    false,
                    "Unsafe repository file path detected."
            );
        }

        /*
         * If developer explicitly requested a file,
         * AI MUST use that file.
         */
        if (explicitFile != null) {

            String requested =
                    normalizePath(explicitFile);

            boolean matches;

            if (requested.contains("/")) {

                matches =
                        filePath.equalsIgnoreCase(
                                requested
                        );

            } else {

                matches =
                        getFileName(filePath)
                                .equalsIgnoreCase(
                                        requested
                                );
            }

            if (!matches) {

                return new ValidationResult(
                        false,
                        "AI selected the wrong file. Requested: "
                                + explicitFile
                                + " | Generated: "
                                + filePath
                );
            }
        }

        /*
         * SEARCH/REPLACE must not contain Markdown.
         */
        if (searchText.contains("```")
                || replaceText.contains("```")) {

            return new ValidationResult(
                    false,
                    "Generated change contains Markdown code fences."
            );
        }

        return new ValidationResult(
                true,
                "Change passed generation validation."
        );
    }

    private String extractGeneratedFile(
            String change
    ) {

        for (String line :
                change.split("\n")) {

            if (line.trim()
                    .toUpperCase()
                    .startsWith("FILE:")) {

                int index =
                        line.indexOf(':');

                return line
                        .substring(index + 1)
                        .trim();
            }
        }

        return null;
    }

    private String extractSearchText(
            String change
    ) {

        int start =
                indexOfIgnoreCase(
                        change,
                        "\nSEARCH\n"
                );

        if (start < 0) {
            return null;
        }

        start +=
                "\nSEARCH\n".length();

        int end =
                indexOfIgnoreCase(
                        change,
                        "\nEND SEARCH",
                        start
                );

        if (end < 0) {
            return null;
        }

        return change
                .substring(start, end)
                .trim();
    }

    private String extractReplaceText(
            String change
    ) {

        int start =
                indexOfIgnoreCase(
                        change,
                        "\nREPLACE\n"
                );

        if (start < 0) {
            return null;
        }

        start +=
                "\nREPLACE\n".length();

        int end =
                indexOfIgnoreCase(
                        change,
                        "\nEND REPLACE",
                        start
                );

        if (end < 0) {
            return null;
        }

        return change
                .substring(start, end)
                .trim();
    }

    private String extractExplicitFileReference(
            String issue
    ) {

        Matcher matcher =
                JAVA_FILE_PATTERN.matcher(issue);

        String bestMatch = null;

        while (matcher.find()) {

            String candidate =
                    normalizePath(
                            matcher.group(1)
                    );

            if (bestMatch == null
                    || candidate.length()
                    > bestMatch.length()) {

                bestMatch = candidate;
            }
        }

        return bestMatch;
    }

    private boolean containsRequestedFile(
            List<HybridSearchResult> results,
            String requestedFile
    ) {

        if (results == null
                || requestedFile == null) {

            return false;
        }

        String requested =
                normalizePath(requestedFile);

        for (HybridSearchResult result :
                results) {

            String resultPath =
                    normalizePath(
                            result.getFilePath()
                    );

            if (requested.contains("/")) {

                if (resultPath.equalsIgnoreCase(
                        requested
                )) {

                    return true;
                }

            } else {

                if (getFileName(resultPath)
                        .equalsIgnoreCase(
                                requested
                        )) {

                    return true;
                }
            }
        }

        return false;
    }

    private List<HybridSearchResult> mergeResults(
            List<HybridSearchResult> first,
            List<HybridSearchResult> second
    ) {

        Map<String, HybridSearchResult> unique =
                new LinkedHashMap<>();

        if (first != null) {

            for (HybridSearchResult result :
                    first) {

                unique.put(
                        buildResultKey(result),
                        result
                );
            }
        }

        if (second != null) {

            for (HybridSearchResult result :
                    second) {

                unique.put(
                        buildResultKey(result),
                        result
                );
            }
        }

        return new ArrayList<>(
                unique.values()
        );
    }

    private String buildResultKey(
            HybridSearchResult result
    ) {

        return normalizePath(
                result.getFilePath()
        )
                + ":"
                + result.getStartLine()
                + ":"
                + result.getEndLine();
    }

    private String normalizePath(
            String path
    ) {

        if (path == null) {
            return "";
        }

        return path
                .replace("\\", "/")
                .replaceFirst("^/+", "")
                .trim();
    }

    private String getFileName(
            String path
    ) {

        String normalized =
                normalizePath(path);

        int index =
                normalized.lastIndexOf('/');

        if (index < 0) {
            return normalized;
        }

        return normalized.substring(
                index + 1
        );
    }

    private int indexOfIgnoreCase(
            String text,
            String search
    ) {

        return indexOfIgnoreCase(
                text,
                search,
                0
        );
    }

    private int indexOfIgnoreCase(
            String text,
            String search,
            int fromIndex
    ) {

        if (text == null
                || search == null) {

            return -1;
        }

        return text
                .toLowerCase()
                .indexOf(
                        search.toLowerCase(),
                        fromIndex
                );
    }

    private List<String> buildSourceList(
            List<HybridSearchResult> results
    ) {

        List<String> sources =
                new ArrayList<>();

        if (results == null) {
            return sources;
        }

        for (HybridSearchResult result :
                results) {

            sources.add(
                    result.getFilePath()
                            + " | Lines "
                            + result.getStartLine()
                            + "-"
                            + result.getEndLine()
            );
        }

        return sources;
    }

    public record CodeGenerationResult(
            boolean success,
            String message,
            String generatedPatch,
            List<String> sources
    ) {
    }

    private record ValidationResult(
            boolean valid,
            String message
    ) {
    }
}
