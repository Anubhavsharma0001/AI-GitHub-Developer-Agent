package com.anubhav.Backend.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class GitBranchService {

    public BranchResult createBranch(
            Path repositoryPath,
            String branchName
    ) {

        if (repositoryPath == null) {
            return new BranchResult(
                    false,
                    "Repository path is null.",
                    null
            );
        }

        if (branchName == null
                || branchName.isBlank()) {

            return new BranchResult(
                    false,
                    "Branch name cannot be empty.",
                    null
            );
        }

        String safeBranchName =
                branchName.trim();

        try {

            CommandResult statusResult =
                    runGitCommand(
                            repositoryPath,
                            "status",
                            "--porcelain"
                    );

            if (!statusResult.success()) {
                return new BranchResult(
                        false,
                        "Could not check repository status.\n"
                                + statusResult.output(),
                        null
                );
            }

            if (!statusResult.output().isBlank()) {
                return new BranchResult(
                        false,
                        "Repository has uncommitted changes. "
                                + "Rollback or commit them before creating "
                                + "an agent branch.\n"
                                + statusResult.output(),
                        null
                );
            }

            CommandResult branchResult =
                    runGitCommand(
                            repositoryPath,
                            "switch",
                            "-c",
                            safeBranchName
                    );

            if (!branchResult.success()) {
                return new BranchResult(
                        false,
                        "Could not create Git branch.\n"
                                + branchResult.output(),
                        null
                );
            }

            return new BranchResult(
                    true,
                    "Git branch created successfully.",
                    safeBranchName
            );

        } catch (Exception exception) {

            return new BranchResult(
                    false,
                    "Branch creation failed unexpectedly: "
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

    public record BranchResult(
            boolean success,
            String message,
            String branchName
    ) {
    }

    private record CommandResult(
            boolean success,
            String output
    ) {
    }
}