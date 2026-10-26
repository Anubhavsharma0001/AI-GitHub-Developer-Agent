
        package com.anubhav.Backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SearchReplaceApplicationService {

    private final JdbcTemplate jdbcTemplate;
    private final Path repositoryBasePath;

    public SearchReplaceApplicationService(
            JdbcTemplate jdbcTemplate,
            @Value("${agent.repository.base-path}") String repositoryBasePath
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.repositoryBasePath = Path.of(repositoryBasePath)
                .toAbsolutePath()
                .normalize();
    }

    public SearchReplaceResult applyChange(
            Long repositoryId,
            boolean approved,
            String generatedChange
    ) {
        if (!approved) {
            return failure("Change has not been approved.");
        }

        if (repositoryId == null || repositoryId <= 0) {
            return failure("Invalid repository ID.");
        }

        if (generatedChange == null || generatedChange.isBlank()) {
            return failure("Generated change is empty.");
        }

        try {
            String fullName = jdbcTemplate.queryForObject(
                    "SELECT full_name FROM github_repositories WHERE id = ?",
                    String.class,
                    repositoryId
            );

            if (fullName == null || fullName.isBlank()) {
                return failure("Repository was not found.");
            }

            Path repository = resolveRepository(fullName);

            if (repository == null) {
                return failure(
                        "Local repository directory was not found for "
                                + fullName
                );
            }

            Path realRepository = repository.toRealPath();
            ParsedChange change = parseChange(generatedChange);

            Path relativePath = Path.of(change.filePath());

            if (relativePath.isAbsolute()) {
                return failure("Absolute paths are not allowed.");
            }

            Path target = repository.resolve(relativePath).normalize();

            if (!target.startsWith(repository)) {
                return failure("Target path escapes the repository.");
            }

            if (!Files.isRegularFile(target)) {
                return failure(
                        "Target file does not exist: " + change.filePath()
                );
            }

            target = target.toRealPath();

            if (!target.startsWith(realRepository)) {
                return failure("Target resolves outside the repository.");
            }

            String original = Files.readString(
                    target,
                    StandardCharsets.UTF_8
            );

            validateChange(
                    change.search(),
                    change.replace(),
                    original
            );

            String updated = replaceUnique(
                    original,
                    change.search(),
                    change.replace()
            );

            if (updated.equals(original)) {
                return failure("No changes were produced.");
            }

            Files.writeString(
                    target,
                    updated,
                    StandardCharsets.UTF_8
            );

            String gitDiff = getGitDiff(repository, change.filePath());

            return new SearchReplaceResult(
                    true,
                    "Change applied successfully.",
                    change.filePath(),
                    List.of(change.filePath()),
                    gitDiff
            );

        } catch (Exception exception) {
            return failure(
                    "Change application failed: "
                            + exception.getMessage()
            );
        }
    }

    private Path resolveRepository(String fullName) throws Exception {
        Path configuredRepository = repositoryBasePath
                .resolve(fullName)
                .normalize();

        if (configuredRepository.startsWith(repositoryBasePath)
                && Files.isDirectory(configuredRepository.resolve(".git"))) {
            return configuredRepository;
        }

        String[] parts = fullName.split("/", 2);

        if (parts.length == 2) {
            Path repositoryNameOnly = repositoryBasePath
                    .resolve(parts[1])
                    .normalize();

            if (repositoryNameOnly.startsWith(repositoryBasePath)
                    && Files.isDirectory(
                    repositoryNameOnly.resolve(".git"))) {
                return repositoryNameOnly;
            }
        }

        return null;
    }

    private ParsedChange parseChange(String input) {
        String text = input.trim();

        text = text.replaceFirst(
                "(?s)^```(?:text|java|plaintext)?\\s*",
                ""
        );

        text = text.replaceFirst("(?s)\\s*```$", "").trim();

        Pattern pattern = Pattern.compile(
                "(?s)FILE:\\s*(.*?)\\s*"
                        + "SEARCH\\s*\\R(.*?)\\R\\s*END SEARCH\\s*"
                        + "REPLACE\\s*\\R(.*?)\\R\\s*END REPLACE"
        );

        Matcher matcher = pattern.matcher(text);

        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Invalid format. Expected FILE, SEARCH, and REPLACE blocks."
            );
        }

        String filePath = matcher.group(1).trim();
        String search = matcher.group(2);
        String replace = matcher.group(3);

        if (filePath.isBlank() || search.isBlank() || replace.isBlank()) {
            throw new IllegalArgumentException(
                    "FILE, SEARCH, and REPLACE cannot be empty."
            );
        }

        return new ParsedChange(filePath, search, replace);
    }

    private void validateChange(
            String search,
            String replace,
            String currentContent
    ) {
        String normalizedSearch = normalize(search).strip();
        String normalizedReplace = normalize(replace).strip();
        String normalizedCurrent = normalize(currentContent);

        if (normalizedSearch.length() < 12) {
            throw new IllegalArgumentException(
                    "SEARCH must contain a specific code block."
            );
        }

        if (normalizedSearch.equals(normalizedReplace)) {
            throw new IllegalArgumentException(
                    "Replacement is identical to the original code."
            );
        }

        if (isUnsafeSearch(normalizedSearch)) {
            throw new IllegalArgumentException(
                    "Unsafe SEARCH block. Select specific code, not a method or class declaration."
            );
        }

        int occurrences = countOccurrences(
                normalizedCurrent,
                normalizedSearch
        );

        if (occurrences != 1) {
            throw new IllegalArgumentException(
                    "SEARCH must match exactly once. Found: " + occurrences
            );
        }

        String comparableSearch =
                normalizeForComparison(normalizedSearch);

        String comparableReplace =
                normalizeForComparison(normalizedReplace);

        if (!comparableReplace.contains(comparableSearch)) {
            throw new IllegalArgumentException(
                    "REPLACE must preserve the original SEARCH code."
            );
        }

        int searchLines = countLines(normalizedSearch);
        int replaceLines = countLines(normalizedReplace);

        if (replaceLines > searchLines * 3 + 10) {
            throw new IllegalArgumentException(
                    "Replacement is too large for the selected code block."
            );
        }
    }

    private boolean isUnsafeSearch(String search) {
        String text = search.strip();

        if (text.matches("(?s)[{};\\s]+")) {
            return true;
        }

        if (text.matches(
                "(?s)(public|protected|private)?\\s*"
                        + "(static\\s+)?void\\s+main\\s*\\(.*"
        )) {
            return true;
        }

        return text.matches(
                "(?s)(public|protected|private)?\\s*"
                        + "(abstract\\s+|final\\s+)?"
                        + "(class|interface|enum|record)\\s+\\w+.*"
        );
    }

    private String replaceUnique(
            String original,
            String search,
            String replacement
    ) {
        String normalizedOriginal = normalize(original);
        String normalizedSearch = normalize(search).strip();
        String normalizedReplacement = normalize(replacement).strip();

        int index = normalizedOriginal.indexOf(normalizedSearch);

        if (index < 0) {
            throw new IllegalArgumentException(
                    "SEARCH block was not found in the file."
            );
        }

        if (normalizedOriginal.indexOf(
                normalizedSearch,
                index + normalizedSearch.length()
        ) >= 0) {
            throw new IllegalArgumentException(
                    "SEARCH matches multiple locations."
            );
        }

        String lineSeparator = original.contains("\r\n") ? "\r\n" : "\n";

        normalizedReplacement = normalizedReplacement.replace(
                "\n",
                lineSeparator
        );

        return normalizedOriginal.substring(0, index)
                + normalizedReplacement
                + normalizedOriginal.substring(
                index + normalizedSearch.length()
        );
    }

    private String getGitDiff(Path repository, String filePath)
            throws Exception {
        ProcessBuilder builder = new ProcessBuilder(
                "git", "diff", "--", filePath
        );

        builder.directory(repository.toFile());
        builder.redirectErrorStream(true);

        Process process = builder.start();

        String output = new String(
                process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            return "Unable to retrieve Git diff: " + output;
        }

        return output;
    }

    private String normalize(String text) {
        return text.replace("\r\n", "\n")
                .replace('\r', '\n');
    }

    private String normalizeForComparison(String text) {
        return normalize(text)
                .lines()
                .map(String::strip)
                .reduce((first, second) -> first + "\n" + second)
                .orElse("");
    }

    private int countOccurrences(String text, String target) {
        if (target.isEmpty()) {
            return 0;
        }

        int count = 0;
        int index = 0;

        while ((index = text.indexOf(target, index)) >= 0) {
            count++;
            index += target.length();
        }

        return count;
    }

    private int countLines(String text) {
        return text.split("\\R", -1).length;
    }

    private SearchReplaceResult failure(String message) {
        return new SearchReplaceResult(
                false,
                message,
                null,
                List.of(),
                ""
        );
    }

    private record ParsedChange(
            String filePath,
            String search,
            String replace
    ) {
    }

    public record SearchReplaceResult(
            boolean success,
            String message,
            String filePath,
            List<String> changedFiles,
            String gitDiff
    ) {
    }
}
