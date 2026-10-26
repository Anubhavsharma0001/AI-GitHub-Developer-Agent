package com.anubhav.Backend.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class GitCommitService {

    public CommitResult commit(
            Path repositoryPath,
            String commitMessage
    ) {

        if (repositoryPath == null) {
            return new CommitResult(
                    false,
                    "Repository path is null.",
                    null
            );
        }

        if (commitMessage == null
                || commitMessage.isBlank()) {

            return new CommitResult(
                    false,
                    "Commit message cannot be empty.",
                    null
            );
        }

        try {

            CommandResult statusResult =
                    runGitCommand(
                            repositoryPath,
                            "status",
                            "--porcelain"
                    );

            if (!statusResult.success()) {
                return new CommitResult(
                        false,
                        "Could not check repository status.\n"
                                + statusResult.output(),
                        null
                );
            }

            if (statusResult.output().isBlank()) {
                return new CommitResult(
                        false,
                        "There are no changes to commit.",
                        null
                );
            }

            CommandResult addResult =
                    runGitCommand(
                            repositoryPath,
                            "add",
                            "."
                    );

            if (!addResult.success()) {
                return new CommitResult(
                        false,
                        "Git add failed.\n"
                                + addResult.output(),
                        null
                );
            }

            CommandResult commitResult =
                    runGitCommand(
                            repositoryPath,
                            "commit",
                            "-m",
                            commitMessage.trim()
                    );

            if (!commitResult.success()) {
                return new CommitResult(
                        false,
                        "Git commit failed.\n"
                                + commitResult.output(),
                        null
                );
            }

            CommandResult hashResult =
                    runGitCommand(
                            repositoryPath,
                            "rev-parse",
                            "HEAD"
                    );

            String commitHash =
                    hashResult.success()
                            ? hashResult.output().trim()
                            : null;

            return new CommitResult(
                    true,
                    "Git commit created successfully.",
                    commitHash
            );

        } catch (Exception exception) {

            return new CommitResult(
                    false,
                    "Commit failed unexpectedly: "
                            + exception.getMessage(),
                    null
            );
        }
    }

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

    public record CommitResult(
            boolean success,
            String message,
            String commitHash
    ) {
    }

    private record CommandResult(
            boolean success,
            String output
    ) {
    }
}