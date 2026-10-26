package com.anubhav.Backend.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TestExecutionService {

    private static final long TIMEOUT_SECONDS = 10;

    private static final Pattern PACKAGE_PATTERN =
            Pattern.compile(
                    "(?m)^\\s*package\\s+([a-zA-Z0-9_.]+)\\s*;"
            );

    private static final Pattern CLASS_PATTERN =
            Pattern.compile(
                    "\\b(?:public\\s+)?class\\s+([A-Za-z_$][A-Za-z0-9_$]*)"
            );

    private static final Pattern MAIN_METHOD_PATTERN =
            Pattern.compile(
                    "\\bpublic\\s+static\\s+void\\s+main\\s*\\(\\s*String\\s*\\[\\]\\s+[A-Za-z_$][A-Za-z0-9_$]*\\s*\\)"
            );

    public TestExecutionResult runTest(
            Path repositoryPath,
            Path javaFile
    ) {

        if (repositoryPath == null
                || !Files.isDirectory(repositoryPath)) {

            return new TestExecutionResult(
                    "NOT_RUN",
                    "Repository directory does not exist.",
                    "",
                    -1,
                    null
            );
        }

        if (javaFile == null
                || !Files.isRegularFile(javaFile)) {

            return new TestExecutionResult(
                    "NOT_RUN",
                    "Java file does not exist.",
                    "",
                    -1,
                    null
            );
        }

        try {

            // =================================================
            // READ SOURCE FILE
            // =================================================

            String source =
                    Files.readString(
                            javaFile,
                            StandardCharsets.UTF_8
                    );

            // =================================================
            // CHECK MAIN METHOD
            // =================================================

            if (!hasRunnableMainMethod(source)) {

                return new TestExecutionResult(
                        "NOT_RUN",
                        "Class does not contain "
                                + "public static void main(String[] args).",
                        "",
                        -1,
                        null
                );
            }

            // =================================================
            // FIND FULLY QUALIFIED CLASS NAME
            // =================================================

            String className =
                    buildFullyQualifiedClassName(
                            source
                    );

            if (className == null) {

                return new TestExecutionResult(
                        "NOT_RUN",
                        "Could not determine the fully qualified "
                                + "class name.",
                        "",
                        -1,
                        null
                );
            }

            // =================================================
            // FIND BUILD DIRECTORY
            // =================================================

            Path buildDirectory =
                    repositoryPath.resolve(
                            ".agent-build"
                    );

            if (!Files.isDirectory(buildDirectory)) {

                return new TestExecutionResult(
                        "NOT_RUN",
                        "Compilation output directory "
                                + "does not exist. Compile first.",
                        "",
                        -1,
                        null
                );
            }

            // =================================================
            // BUILD JAVA COMMAND
            // =================================================

            List<String> command =
                    new ArrayList<>();

            command.add("java");

            command.add("-cp");

            command.add(
                    buildDirectory
                            .toAbsolutePath()
                            .toString()
            );

            command.add(className);

            // =================================================
            // START PROCESS
            // =================================================

            ProcessBuilder processBuilder =
                    new ProcessBuilder(command);

            processBuilder
                    .directory(repositoryPath.toFile());

            processBuilder
                    .redirectErrorStream(true);

            Process process =
                    processBuilder.start();

            // =================================================
            // READ OUTPUT IN SEPARATE THREAD
            // =================================================

            StringBuilder output =
                    new StringBuilder();

            Thread outputReader =
                    new Thread(() -> {

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

                            while (
                                    (line = reader.readLine())
                                            != null
                            ) {

                                synchronized (output) {
                                    output.append(line)
                                            .append(
                                                    System.lineSeparator()
                                            );
                                }
                            }

                        } catch (Exception exception) {

                            synchronized (output) {
                                output.append(
                                        "Error reading process output: "
                                                + exception.getMessage()
                                                + System.lineSeparator()
                                );
                            }
                        }
                    });

            outputReader.start();

            // =================================================
            // TIMEOUT PROTECTION
            // =================================================

            boolean finished =
                    process.waitFor(
                            TIMEOUT_SECONDS,
                            TimeUnit.SECONDS
                    );

            if (!finished) {

                process.destroyForcibly();

                outputReader.join(
                        Duration.ofSeconds(2).toMillis()
                );

                return new TestExecutionResult(
                        "TIMEOUT",
                        "Test exceeded "
                                + TIMEOUT_SECONDS
                                + " seconds and was terminated.",
                        output.toString().trim(),
                        -1,
                        className
                );
            }

            outputReader.join(
                    Duration.ofSeconds(2).toMillis()
            );

            int exitCode =
                    process.exitValue();

            String processOutput =
                    output.toString().trim();

            // =================================================
            // RESULT
            // =================================================

            if (exitCode == 0) {

                return new TestExecutionResult(
                        "PASS",
                        "Test executed successfully.",
                        processOutput,
                        exitCode,
                        className
                );
            }

            return new TestExecutionResult(
                    "FAIL",
                    "Test process exited with code "
                            + exitCode
                            + ".",
                    processOutput,
                    exitCode,
                    className
            );

        } catch (Exception exception) {

            return new TestExecutionResult(
                    "FAIL",
                    "Test execution failed unexpectedly: "
                            + exception.getMessage(),
                    "",
                    -1,
                    null
            );
        }
    }

    // =========================================================
    // MAIN METHOD DETECTION
    // =========================================================

    private boolean hasRunnableMainMethod(
            String source
    ) {

        return MAIN_METHOD_PATTERN
                .matcher(source)
                .find();
    }

    // =========================================================
    // CLASS NAME
    // =========================================================

    private String buildFullyQualifiedClassName(
            String source
    ) {

        Matcher packageMatcher =
                PACKAGE_PATTERN.matcher(source);

        Matcher classMatcher =
                CLASS_PATTERN.matcher(source);

        if (!classMatcher.find()) {
            return null;
        }

        String className =
                classMatcher.group(1);

        if (packageMatcher.find()) {

            String packageName =
                    packageMatcher.group(1);

            return packageName
                    + "."
                    + className;
        }

        return className;
    }

    // =========================================================
    // RESULT
    // =========================================================

    public record TestExecutionResult(
            String status,
            String message,
            String output,
            int exitCode,
            String className
    ) {
    }
}