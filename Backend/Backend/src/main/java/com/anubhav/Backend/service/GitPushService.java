package com.anubhav.Backend.service;

import com.anubhav.Backend.github.GitHubInstallationService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class GitPushService {

    private static final Long INSTALLATION_ID =
            167558882L;

    private static final String REMOTE_URL =
            "https://github.com/Anubhavsharma0001/Basic-java-file.git";

    private final GitHubInstallationService
            gitHubInstallationService;

    public GitPushService(
            GitHubInstallationService gitHubInstallationService
    ) {
        this.gitHubInstallationService =
                gitHubInstallationService;
    }

    public PushResult push(
            Path repositoryPath,
            String branchName
    ) {

        if (repositoryPath == null) {
            return new PushResult(
                    false,
                    "Repository path is null.",
                    null
            );
        }

        if (branchName == null
                || branchName.isBlank()) {

            return new PushResult(
                    false,
                    "Branch name cannot be empty.",
                    null
            );
        }

        try {

            String installationToken =
                    gitHubInstallationService
                            .generateInstallationToken(
                                    INSTALLATION_ID
                            );

            CommandResult remoteResult =
                    runGitCommand(
                            repositoryPath,
                            "remote",
                            "get-url",
                            "origin"
                    );

            if (!remoteResult.success()) {
                return new PushResult(
                        false,
                        "Could not read Git remote.\n"
                                + remoteResult.output(),
                        null
                );
            }

            String authenticatedRemote =
                    buildAuthenticatedRemote(
                            installationToken
                    );

            CommandResult pushResult =
                    runGitCommand(
                            repositoryPath,
                            "push",
                            authenticatedRemote,
                            "HEAD:refs/heads/" + branchName
                    );

            if (!pushResult.success()) {
                return new PushResult(
                        false,
                        "Git push failed.\n"
                                + sanitizeOutput(
                                pushResult.output()
                        ),
                        null
                );
            }

            return new PushResult(
                    true,
                    "Branch pushed successfully to GitHub.",
                    branchName
            );

        } catch (Exception exception) {

            return new PushResult(
                    false,
                    "Push failed unexpectedly: "
                            + exception.getMessage(),
                    null
            );
        }
    }

    private String buildAuthenticatedRemote(
            String installationToken
    ) {

        return "https://x-access-token:"
                + installationToken
                + "@github.com/Anubhavsharma0001/"
                + "Basic-java-file.git";
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

    private String sanitizeOutput(
            String output
    ) {

        if (output == null) {
            return "";
        }

        return output.replaceAll(
                "https://x-access-token:[^@\\s]+@",
                "https://github.com/"
        );
    }

    public record PushResult(
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