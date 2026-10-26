package com.anubhav.Backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class PatchApplicationService {

    private final JdbcTemplate jdbcTemplate;

    private final String repositoryBasePath;

    public PatchApplicationService(
            JdbcTemplate jdbcTemplate,
            @Value("${agent.repository.base-path}")
            String repositoryBasePath
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.repositoryBasePath = repositoryBasePath;
    }

    public PatchApplicationResult applyPatch(
            Long repositoryId,
            String patch
    ) {

        if (patch == null || patch.isBlank()) {
            return new PatchApplicationResult(
                    false,
                    "Patch is empty.",
                    List.of()
            );
        }

        String repositoryName =
                findRepositoryName(repositoryId);

        if (repositoryName == null
                || repositoryName.isBlank()) {

            return new PatchApplicationResult(
                    false,
                    "Repository was not found.",
                    List.of()
            );
        }

        Path repositoryPath =
                Paths.get(repositoryBasePath)
                        .resolve(repositoryName)
                        .normalize();

        if (!Files.exists(repositoryPath)
                || !Files.isDirectory(repositoryPath)) {

            return new PatchApplicationResult(
                    false,
                    "Local repository does not exist: "
                            + repositoryPath,
                    List.of()
            );
        }

        try {

            // =================================================
            // STEP 0: CLEAN AI GENERATED PATCH
            // =================================================

            String cleanPatch =
                    extractGitPatch(patch);

            if (cleanPatch.isBlank()) {

                return new PatchApplicationResult(
                        false,
                        "No valid Git diff was found inside the AI generated response.",
                        List.of()
                );
            }

            // =================================================
            // STEP 1: DRY RUN
            // =================================================

            CommandResult checkResult =
                    runGitApply(
                            repositoryPath,
                            cleanPatch,
                            true
                    );

            if (!checkResult.success()) {

                return new PatchApplicationResult(
                        false,
                        "Patch cannot be applied.\n\n"
                                + checkResult.output(),
                        List.of()
                );
            }

            // =================================================
            // STEP 2: ACTUALLY APPLY PATCH
            // =================================================

            CommandResult applyResult =
                    runGitApply(
                            repositoryPath,
                            cleanPatch,
                            false
                    );

            if (!applyResult.success()) {

                return new PatchApplicationResult(
                        false,
                        "Patch application failed.\n\n"
                                + applyResult.output(),
                        List.of()
                );
            }

            // =================================================
            // STEP 3: GET CHANGED FILES
            // =================================================

            CommandResult statusResult =
                    runGitCommand(
                            repositoryPath,
                            "status",
                            "--short"
                    );

            List<String> changedFiles =
                    parseChangedFiles(
                            statusResult.output()
                    );

            return new PatchApplicationResult(
                    true,
                    "Patch applied successfully.",
                    changedFiles
            );

        } catch (Exception exception) {

            return new PatchApplicationResult(
                    false,
                    "Unexpected error while applying patch: "
                            + exception.getMessage(),
                    List.of()
            );
        }
    }

    // =========================================================
    // EXTRACT ACTUAL GIT PATCH FROM AI RESPONSE
    // =========================================================

    private String extractGitPatch(String patch) {

        if (patch == null || patch.isBlank()) {
            return "";
        }

        String cleaned =
                patch
                        .replace("\r\n", "\n")
                        .replace("\r", "\n")
                        .trim();

        // -----------------------------------------------------
        // CASE 1:
        // AI returned a fenced diff:
        //
        // ```diff
        // diff --git ...
        // ...
        // ```
        // -----------------------------------------------------

        int diffFenceStart =
                cleaned.indexOf("```diff");

        if (diffFenceStart >= 0) {

            int contentStart =
                    diffFenceStart + "```diff".length();

            int closingFence =
                    cleaned.indexOf(
                            "```",
                            contentStart
                    );

            if (closingFence >= 0) {

                String fencedPatch =
                        cleaned.substring(
                                contentStart,
                                closingFence
                        ).trim();

                int diffStart =
                        fencedPatch.indexOf("diff --git ");

                if (diffStart >= 0) {
                    return fencedPatch
                            .substring(diffStart)
                            .trim();
                }
            }
        }

        // -----------------------------------------------------
        // CASE 2:
        // AI returned normal text followed by:
        //
        // diff --git ...
        // -----------------------------------------------------

        int diffStart =
                cleaned.indexOf("diff --git ");

        if (diffStart >= 0) {

            String extracted =
                    cleaned.substring(diffStart);

            // Remove closing Markdown fence if present.
            int closingFence =
                    extracted.indexOf("\n```");

            if (closingFence >= 0) {

                extracted =
                        extracted.substring(
                                0,
                                closingFence
                        );
            }

            return extracted.trim();
        }

        return "";
    }

    // =========================================================
    // FIND REPOSITORY
    // =========================================================

    private String findRepositoryName(
            Long repositoryId
    ) {

        List<String> names =
                jdbcTemplate.query(
                        """
                        SELECT repository_name
                        FROM github_repositories
                        WHERE id = ?
                        """,
                        (resultSet, rowNum) ->
                                resultSet.getString(
                                        "repository_name"
                                ),
                        repositoryId
                );

        if (names.isEmpty()) {
            return null;
        }

        return names.get(0);
    }

    // =========================================================
    // GIT APPLY
    // =========================================================

    private CommandResult runGitApply(
            Path repositoryPath,
            String patch,
            boolean checkOnly
    ) throws Exception {

        List<String> command =
                new ArrayList<>();

        command.add("git");
        command.add("apply");

        if (checkOnly) {
            command.add("--check");
        }

        ProcessBuilder processBuilder =
                new ProcessBuilder(command);

        processBuilder.directory(
                repositoryPath.toFile()
        );

        processBuilder.redirectErrorStream(true);

        Process process =
                processBuilder.start();

        // Send clean patch through STDIN
        process.getOutputStream()
                .write(
                        patch.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

        process.getOutputStream().close();

        String output =
                readProcessOutput(process);

        int exitCode =
                process.waitFor();

        return new CommandResult(
                exitCode == 0,
                output
        );
    }

    // =========================================================
    // GIT COMMAND
    // =========================================================

    private CommandResult runGitCommand(
            Path repositoryPath,
            String... arguments
    ) throws Exception {

        List<String> command =
                new ArrayList<>();

        command.add("git");

        for (String argument : arguments) {
            command.add(argument);
        }

        ProcessBuilder processBuilder =
                new ProcessBuilder(command);

        processBuilder.directory(
                repositoryPath.toFile()
        );

        processBuilder.redirectErrorStream(true);

        Process process =
                processBuilder.start();

        String output =
                readProcessOutput(process);

        int exitCode =
                process.waitFor();

        return new CommandResult(
                exitCode == 0,
                output
        );
    }

    // =========================================================
    // PROCESS OUTPUT
    // =========================================================

    private String readProcessOutput(
            Process process
    ) throws Exception {

        StringBuilder output =
                new StringBuilder();

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        process.getInputStream(),
                                        StandardCharsets.UTF_8
                                )
                        )
        ) {

            String line;

            while ((line = reader.readLine()) != null) {

                output.append(line)
                        .append(System.lineSeparator());
            }
        }

        return output.toString().trim();
    }

    // =========================================================
    // PARSE GIT STATUS
    // =========================================================

    private List<String> parseChangedFiles(
            String status
    ) {

        List<String> files =
                new ArrayList<>();

        if (status == null || status.isBlank()) {
            return files;
        }

        String[] lines =
                status.split("\\R");

        for (String line : lines) {

            if (line.length() <= 3) {
                continue;
            }

            String file =
                    line.substring(3).trim();

            if (!file.isBlank()) {
                files.add(file);
            }
        }

        return files;
    }

    // =========================================================
    // RESULT
    // =========================================================

    public record PatchApplicationResult(
            boolean success,
            String message,
            List<String> changedFiles
    ) {
    }

    private record CommandResult(
            boolean success,
            String output
    ) {
    }
}