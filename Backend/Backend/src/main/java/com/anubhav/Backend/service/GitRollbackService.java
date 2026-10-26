package com.anubhav.Backend.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@Service
public class GitRollbackService {

    public RollbackResult rollback(
            Path repositoryPath
    ) {

        if (repositoryPath == null) {

            return new RollbackResult(
                    false,
                    "Repository path is null."
            );
        }

        try {

            // =================================================
            // DISCARD MODIFIED TRACKED FILES
            // =================================================

            CommandResult restoreResult =
                    runGitCommand(
                            repositoryPath,
                            "restore",
                            "."
                    );

            if (!restoreResult.success()) {

                return new RollbackResult(
                        false,
                        "Git restore failed.\n"
                                + restoreResult.output()
                );
            }

            // =================================================
            // REMOVE UNTRACKED FILES
            // =================================================

            CommandResult cleanResult =
                    runGitCommand(
                            repositoryPath,
                            "clean",
                            "-fd"
                    );

            if (!cleanResult.success()) {

                return new RollbackResult(
                        false,
                        "Git clean failed.\n"
                                + cleanResult.output()
                );
            }

            // =================================================
            // RESULT
            // =================================================

            return new RollbackResult(
                    true,
                    "Repository rolled back successfully."
            );

        } catch (Exception exception) {

            return new RollbackResult(
                    false,
                    "Rollback failed unexpectedly: "
                            + exception.getMessage()
            );
        }
    }

    // =========================================================
    // GIT COMMAND
    // =========================================================

    private CommandResult runGitCommand(
            Path repositoryPath,
            String... arguments
    ) throws Exception {

        java.util.List<String> command =
                new java.util.ArrayList<>();

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
    // RESULT
    // =========================================================

    public record RollbackResult(
            boolean success,
            String message
    ) {
    }

    private record CommandResult(
            boolean success,
            String output
    ) {
    }
}