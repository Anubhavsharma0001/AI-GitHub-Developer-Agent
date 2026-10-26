package com.anubhav.Backend.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class CompileTestService {

    private static final String JAVA_RELEASE = "24";

    public CompileResult compile(
            Path repositoryPath
    ) {

        if (repositoryPath == null
                || !Files.isDirectory(repositoryPath)) {

            return new CompileResult(
                    false,
                    "Repository directory does not exist.",
                    List.of()
            );
        }

        try {

            // =================================================
            // FIND JAVA FILES
            // =================================================

            List<Path> javaFiles =
                    Files.walk(repositoryPath)
                            .filter(Files::isRegularFile)
                            .filter(path ->
                                    path.toString()
                                            .endsWith(".java"))
                            .filter(path ->
                                    !isExcludedFile(
                                            repositoryPath,
                                            path
                                    ))
                            .toList();

            if (javaFiles.isEmpty()) {

                return new CompileResult(
                        false,
                        "No Java files were found.",
                        List.of()
                );
            }

            // =================================================
            // BUILD OUTPUT DIRECTORY
            // =================================================

            Path buildDirectory =
                    repositoryPath.resolve(
                            ".agent-build"
                    );

            Files.createDirectories(
                    buildDirectory
            );

            // =================================================
            // BUILD JAVAC COMMAND
            // =================================================

            List<String> command =
                    new ArrayList<>();

            command.add("javac");

            // Force Java 24 compatible bytecode.
            command.add("--release");
            command.add(JAVA_RELEASE);

            command.add("-d");

            command.add(
                    buildDirectory.toAbsolutePath()
                            .toString()
            );

            for (Path javaFile : javaFiles) {

                command.add(
                        javaFile.toAbsolutePath()
                                .toString()
                );
            }

            // =================================================
            // RUN JAVAC
            // =================================================

            ProcessBuilder processBuilder =
                    new ProcessBuilder(command);

            processBuilder
                    .directory(repositoryPath.toFile());

            processBuilder
                    .redirectErrorStream(true);

            Process process =
                    processBuilder.start();

            String output =
                    readProcessOutput(process);

            int exitCode =
                    process.waitFor();

            // =================================================
            // RESULT
            // =================================================

            if (exitCode != 0) {

                return new CompileResult(
                        false,
                        output,
                        javaFiles.stream()
                                .map(Path::toString)
                                .toList()
                );
            }

            return new CompileResult(
                    true,
                    output.isBlank()
                            ? "Compilation successful using Java release "
                            + JAVA_RELEASE
                            : output,
                    javaFiles.stream()
                            .map(Path::toString)
                            .toList()
            );

        } catch (Exception exception) {

            return new CompileResult(
                    false,
                    "Compilation failed unexpectedly: "
                            + exception.getMessage(),
                    List.of()
            );
        }
    }

    // =========================================================
    // EXCLUDED FILES
    // =========================================================

    private boolean isExcludedFile(
            Path repositoryPath,
            Path javaFile
    ) {

        Path relativePath =
                repositoryPath.relativize(
                        javaFile
                );

        /*
         * IntelliJ's default starter file.
         *
         * src/Main.java uses Java's newer
         * implicitly-declared class syntax,
         * which is not compatible with our
         * Java 24 compilation target.
         *
         * We exclude only this exact file.
         */

        return relativePath.toString()
                .equalsIgnoreCase(
                        "src" + java.io.File.separator + "Main.java"
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

    public record CompileResult(
            boolean success,
            String output,
            List<String> compiledFiles
    ) {
    }
}