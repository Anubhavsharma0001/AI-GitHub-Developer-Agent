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
public class AgentWorkflowService {

    private final GitBranchService gitBranchService;
    private final GitCommitService gitCommitService;
    private final GitPushService gitPushService;
    private final GitRollbackService gitRollbackService;
    private final CodeGenerationService codeGenerationService;
    private final SearchReplaceApplicationService searchReplaceApplicationService;
    private final GitHubPullRequestService gitHubPullRequestService;
    private final JdbcTemplate jdbcTemplate;
    private final String repositoryBasePath;

    public AgentWorkflowService(
            GitBranchService gitBranchService,
            GitCommitService gitCommitService,
            GitPushService gitPushService,
            GitRollbackService gitRollbackService,
            CodeGenerationService codeGenerationService,
            SearchReplaceApplicationService searchReplaceApplicationService,
            GitHubPullRequestService gitHubPullRequestService,
            JdbcTemplate jdbcTemplate,
            @Value("${agent.repository.base-path}")
            String repositoryBasePath
    ) {
        this.gitBranchService = gitBranchService;
        this.gitCommitService = gitCommitService;
        this.gitPushService = gitPushService;
        this.gitRollbackService = gitRollbackService;
        this.codeGenerationService = codeGenerationService;
        this.searchReplaceApplicationService =
                searchReplaceApplicationService;
        this.gitHubPullRequestService =
                gitHubPullRequestService;
        this.jdbcTemplate = jdbcTemplate;
        this.repositoryBasePath = repositoryBasePath;
    }

    public WorkflowResult startWorkflow(
            Path repositoryPath,
            String branchName
    ) {

        WorkflowResult result =
                new WorkflowResult();

        result.status = "STARTED";
        result.branchName = branchName;

        GitBranchService.BranchResult branchResult =
                gitBranchService.createBranch(
                        repositoryPath,
                        branchName
                );

        if (!branchResult.success()) {

            result.status =
                    "BRANCH_CREATION_FAILED";

            result.message =
                    branchResult.message();

            return result;
        }

        result.branchCreated = true;
        result.status = "BRANCH_CREATED";
        result.message =
                "Agent branch created successfully.";

        return result;
    }

    public WorkflowResult generateCode(
            String issue
    ) {

        WorkflowResult result =
                new WorkflowResult();

        result.status =
                "GENERATING_CODE";

        CodeGenerationService.CodeGenerationResult
                generationResult =
                codeGenerationService.generate(issue);

        if (!generationResult.success()) {

            result.status =
                    "CODE_GENERATION_FAILED";

            result.message =
                    generationResult.message();

            result.sources =
                    generationResult.sources();

            return result;
        }

        result.status =
                "CODE_GENERATED";

        result.message =
                generationResult.message();

        result.generatedPatch =
                generationResult.generatedPatch();

        result.sources =
                generationResult.sources();

        return result;
    }

    public WorkflowResult commitAndPush(
            Path repositoryPath,
            String branchName,
            String commitMessage
    ) {

        WorkflowResult result =
                new WorkflowResult();

        result.branchName =
                branchName;

        GitCommitService.CommitResult commitResult =
                gitCommitService.commit(
                        repositoryPath,
                        commitMessage
                );

        if (!commitResult.success()) {

            result.status =
                    "COMMIT_FAILED";

            result.message =
                    commitResult.message();

            return result;
        }

        result.committed = true;

        result.commitHash =
                commitResult.commitHash();

        GitPushService.PushResult pushResult =
                gitPushService.push(
                        repositoryPath,
                        branchName
                );

        if (!pushResult.success()) {

            result.status =
                    "PUSH_FAILED";

            result.message =
                    pushResult.message();

            return result;
        }

        result.pushed = true;

        result.status =
                "PUSHED";

        result.message =
                "Changes committed and pushed successfully.";

        return result;
    }

    /*
     * ============================================================
     * COMPLETE AUTOMATED WORKFLOW
     * ============================================================
     */
    public WorkflowResult execute(
            Long repositoryId,
            String issue,
            String branchName,
            String commitMessage,
            String pullRequestTitle,
            String pullRequestDescription,
            boolean approved
    ) {

        WorkflowResult result =
                new WorkflowResult();

        result.branchName =
                branchName;

        if (repositoryId == null) {

            result.status =
                    "INVALID_REQUEST";

            result.message =
                    "Repository ID cannot be null.";

            return result;
        }

        if (issue == null || issue.isBlank()) {

            result.status =
                    "INVALID_REQUEST";

            result.message =
                    "Developer issue cannot be empty.";

            return result;
        }

        if (branchName == null
                || branchName.isBlank()) {

            result.status =
                    "INVALID_REQUEST";

            result.message =
                    "Branch name cannot be empty.";

            return result;
        }

        if (!approved) {

            result.status =
                    "APPROVAL_REQUIRED";

            result.message =
                    "Code change was not approved.";

            return result;
        }

        try {

            /*
             * ----------------------------------------------------
             * 1. Resolve repository
             * ----------------------------------------------------
             */
            String repositoryName =
                    findRepositoryName(repositoryId);

            if (repositoryName == null
                    || repositoryName.isBlank()) {

                result.status =
                        "REPOSITORY_NOT_FOUND";

                result.message =
                        "Repository was not found.";

                return result;
            }

            Path repositoryPath =
                    Paths.get(
                                    repositoryBasePath
                            )
                            .resolve(repositoryName)
                            .normalize();

            if (!Files.exists(repositoryPath)
                    || !Files.isDirectory(repositoryPath)) {

                result.status =
                        "LOCAL_REPOSITORY_NOT_FOUND";

                result.message =
                        "Local repository does not exist: "
                                + repositoryPath;

                return result;
            }

            /*
             * ----------------------------------------------------
             * 2. Create agent branch
             * ----------------------------------------------------
             */
            result.status =
                    "CREATING_BRANCH";

            GitBranchService.BranchResult
                    branchResult =
                    gitBranchService.createBranch(
                            repositoryPath,
                            branchName
                    );

            if (!branchResult.success()) {

                result.status =
                        "BRANCH_CREATION_FAILED";

                result.message =
                        branchResult.message();

                return result;
            }

            result.branchCreated = true;

            /*
             * ----------------------------------------------------
             * 3. Generate code using RAG + Ollama
             * ----------------------------------------------------
             */
            result.status =
                    "GENERATING_CODE";

            CodeGenerationService.CodeGenerationResult
                    generationResult =
                    codeGenerationService.generate(issue);

            result.sources =
                    generationResult.sources();

            if (!generationResult.success()) {

                result.status =
                        "CODE_GENERATION_FAILED";

                result.message =
                        generationResult.message();

                return result;
            }

            result.generatedPatch =
                    generationResult.generatedPatch();

            result.status =
                    "CODE_GENERATED";

            /*
             * ----------------------------------------------------
             * 4. Apply SEARCH/REPLACE
             * ----------------------------------------------------
             */
            result.status =
                    "APPLYING_CHANGE";

            SearchReplaceApplicationService
                    .SearchReplaceResult applyResult =
                    searchReplaceApplicationService.applyChange(
                            repositoryId,
                            true,
                            generationResult.generatedPatch()
                    );

            if (!applyResult.success()) {

                result.status =
                        "CHANGE_APPLICATION_FAILED";

                result.message =
                        applyResult.message();

                return result;
            }

            result.changedFiles =
                    applyResult.changedFiles();

            result.gitDiff =
                    applyResult.gitDiff();

            /*
             * ----------------------------------------------------
             * 5. Git diff validation
             * ----------------------------------------------------
             */
            result.status =
                    "VALIDATING_DIFF";

            CommandResult diffCheck =
                    runCommand(
                            repositoryPath,
                            "git",
                            "diff",
                            "--check"
                    );

            if (!diffCheck.success()) {

                rollbackSilently(repositoryPath);

                result.status =
                        "DIFF_VALIDATION_FAILED";

                result.message =
                        "Git diff validation failed: "
                                + diffCheck.output();

                return result;
            }

            /*
             * ----------------------------------------------------
             * 6. Compile
             * ----------------------------------------------------
             */
            result.status =
                    "COMPILING";

            VerificationResult compileResult =
                    compileChangedJavaFiles(
                            repositoryPath,
                            result.changedFiles
                    );

            result.compileOutput =
                    compileResult.output();

            if (!compileResult.success()) {

                rollbackSilently(repositoryPath);

                result.status =
                        "COMPILATION_FAILED";

                result.message =
                        compileResult.message();

                return result;
            }

            result.compiled = true;

            /*
             * ----------------------------------------------------
             * 7. Runtime test
             * ----------------------------------------------------
             */
            result.status =
                    "TESTING";

            VerificationResult testResult =
                    runMainClassIfPresent(
                            repositoryPath,
                            result.changedFiles
                    );

            result.testOutput =
                    testResult.output();

            if (!testResult.success()) {

                rollbackSilently(repositoryPath);

                result.status =
                        "TEST_FAILED";

                result.message =
                        testResult.message();

                return result;
            }

            result.tested = true;

            /*
             * ----------------------------------------------------
             * 8. Commit
             * ----------------------------------------------------
             */
            result.status =
                    "COMMITTING";

            String finalCommitMessage =
                    commitMessage == null
                            || commitMessage.isBlank()
                            ? "AI agent: apply requested code change"
                            : commitMessage;

            GitCommitService.CommitResult
                    commitResult =
                    gitCommitService.commit(
                            repositoryPath,
                            finalCommitMessage
                    );

            if (!commitResult.success()) {

                result.status =
                        "COMMIT_FAILED";

                result.message =
                        commitResult.message();

                return result;
            }

            result.committed = true;

            result.commitHash =
                    commitResult.commitHash();

            /*
             * ----------------------------------------------------
             * 9. Push
             * ----------------------------------------------------
             */
            result.status =
                    "PUSHING";

            GitPushService.PushResult pushResult =
                    gitPushService.push(
                            repositoryPath,
                            branchName
                    );

            if (!pushResult.success()) {

                result.status =
                        "PUSH_FAILED";

                result.message =
                        pushResult.message();

                return result;
            }

            result.pushed = true;

            /*
             * ----------------------------------------------------
             * 10. Create or find existing PR
             * ----------------------------------------------------
             */
            result.status =
                    "CREATING_PULL_REQUEST";

            String finalTitle =
                    pullRequestTitle == null
                            || pullRequestTitle.isBlank()
                            ? "AI Agent Code Change"
                            : pullRequestTitle;

            String finalDescription =
                    pullRequestDescription == null
                            || pullRequestDescription.isBlank()
                            ? buildDefaultPullRequestDescription(
                            issue,
                            result
                    )
                            : pullRequestDescription;

            GitHubPullRequestService.PullRequestResult
                    pullRequestResult =
                    gitHubPullRequestService
                            .createPullRequest(
                                    branchName,
                                    finalTitle,
                                    finalDescription
                            );

            result.pullRequestNumber =
                    pullRequestResult
                            .pullRequestNumber();

            result.pullRequestUrl =
                    pullRequestResult
                            .pullRequestUrl();

            if (!pullRequestResult.success()) {

                result.status =
                        "PULL_REQUEST_FAILED";

                result.message =
                        pullRequestResult.message();

                return result;
            }

            /*
             * ----------------------------------------------------
             * COMPLETE
             * ----------------------------------------------------
             */
            result.status =
                    "COMPLETED";

            result.message =
                    "AI developer workflow completed successfully.";

            return result;

        } catch (Exception exception) {

            result.status =
                    "WORKFLOW_FAILED";

            result.message =
                    "Workflow failed: "
                            + exception.getMessage();

            return result;
        }
    }

    /*
     * ============================================================
     * COMPILE CHANGED JAVA FILES
     * ============================================================
     */
    private VerificationResult compileChangedJavaFiles(
            Path repositoryPath,
            List<String> changedFiles
    ) {

        if (changedFiles == null
                || changedFiles.isEmpty()) {

            return new VerificationResult(
                    false,
                    "No changed files were detected.",
                    ""
            );
        }

        List<String> javaFiles =
                changedFiles.stream()
                        .filter(file ->
                                file.toLowerCase()
                                        .endsWith(".java")
                        )
                        .toList();

        if (javaFiles.isEmpty()) {

            return new VerificationResult(
                    true,
                    "No Java files changed. Java compilation skipped.",
                    ""
            );
        }

        try {

            Path buildDirectory =
                    repositoryPath.resolve(
                            ".agent-build"
                    );

            Files.createDirectories(
                    buildDirectory
            );

            List<String> command =
                    new ArrayList<>();

            command.add("javac");
            command.add("-d");
            command.add(
                    buildDirectory.toString()
            );

            for (String javaFile :
                    javaFiles) {

                command.add(javaFile);
            }

            CommandResult result =
                    runCommand(
                            repositoryPath,
                            command.toArray(
                                    new String[0]
                            )
                    );

            if (!result.success()) {

                return new VerificationResult(
                        false,
                        "Java compilation failed.",
                        result.output()
                );
            }

            return new VerificationResult(
                    true,
                    "Java compilation passed.",
                    result.output()
            );

        } catch (Exception exception) {

            return new VerificationResult(
                    false,
                    "Compilation failed: "
                            + exception.getMessage(),
                    ""
            );
        }
    }

    /*
     * ============================================================
     * RUN MAIN CLASS
     * ============================================================
     */
    private VerificationResult runMainClassIfPresent(
            Path repositoryPath,
            List<String> changedFiles
    ) {

        if (changedFiles == null
                || changedFiles.isEmpty()) {

            return new VerificationResult(
                    true,
                    "No changed files. Runtime test skipped.",
                    ""
            );
        }

        try {

            Path buildDirectory =
                    repositoryPath.resolve(
                            ".agent-build"
                    );

            for (String file :
                    changedFiles) {

                if (!file.toLowerCase()
                        .endsWith(".java")) {

                    continue;
                }

                Path javaFile =
                        repositoryPath.resolve(
                                file
                        );

                if (!Files.exists(javaFile)) {
                    continue;
                }

                String content =
                        Files.readString(
                                javaFile,
                                StandardCharsets.UTF_8
                        );

                if (!content.contains(
                        "static void main"
                )) {
                    continue;
                }

                String packageName =
                        extractPackageName(
                                content
                        );

                String className =
                        extractClassName(
                                javaFile
                        );

                if (className == null) {
                    continue;
                }

                String fullyQualifiedClassName;

                if (packageName == null
                        || packageName.isBlank()) {

                    fullyQualifiedClassName =
                            className;

                } else {

                    fullyQualifiedClassName =
                            packageName
                                    + "."
                                    + className;
                }

                CommandResult runResult =
                        runCommand(
                                repositoryPath,
                                "java",
                                "-cp",
                                buildDirectory.toString(),
                                fullyQualifiedClassName
                        );

                if (!runResult.success()) {

                    return new VerificationResult(
                            false,
                            "Runtime test failed.",
                            runResult.output()
                    );
                }

                return new VerificationResult(
                        true,
                        "Runtime test passed.",
                        runResult.output()
                );
            }

            return new VerificationResult(
                    true,
                    "No main method found. Runtime test skipped.",
                    ""
            );

        } catch (Exception exception) {

            return new VerificationResult(
                    false,
                    "Runtime test failed: "
                            + exception.getMessage(),
                    ""
            );
        }
    }

    private String extractPackageName(
            String content
    ) {

        for (String line :
                content.split("\\R")) {

            String trimmed =
                    line.trim();

            if (trimmed.startsWith(
                    "package "
            )
                    && trimmed.endsWith(";")) {

                return trimmed
                        .substring(
                                "package ".length(),
                                trimmed.length() - 1
                        )
                        .trim();
            }
        }

        return null;
    }

    private String extractClassName(
            Path javaFile
    ) {

        String fileName =
                javaFile.getFileName()
                        .toString();

        if (!fileName.endsWith(
                ".java"
        )) {

            return null;
        }

        return fileName.substring(
                0,
                fileName.length() - 5
        );
    }

    private CommandResult runCommand(
            Path workingDirectory,
            String... command
    ) {

        try {

            ProcessBuilder builder =
                    new ProcessBuilder(
                            command
                    );

            builder.directory(
                    workingDirectory.toFile()
            );

            builder.redirectErrorStream(true);

            Process process =
                    builder.start();

            String output =
                    readProcessOutput(
                            process
                    );

            int exitCode =
                    process.waitFor();

            return new CommandResult(
                    exitCode == 0,
                    output
            );

        } catch (Exception exception) {

            return new CommandResult(
                    false,
                    exception.getMessage()
            );
        }
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

            while (
                    (line = reader.readLine())
                            != null
            ) {

                output
                        .append(line)
                        .append(System.lineSeparator());
            }
        }

        return output.toString().trim();
    }

    private void rollbackSilently(
            Path repositoryPath
    ) {

        try {

            gitRollbackService.rollback(
                    repositoryPath
            );

        } catch (Exception ignored) {
        }
    }

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
                        (resultSet, rowNumber) ->
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

    private String buildDefaultPullRequestDescription(
            String issue,
            WorkflowResult result
    ) {

        return """
                ## AI Developer Agent

                ### Developer Issue
                %s

                ### Generated Change
                AI-generated SEARCH/REPLACE change was applied
                to the repository.

                ### Validation
                - Git diff validation: passed
                - Java compilation: passed
                - Runtime test: %s

                ### Commit
                `%s`
                """.formatted(
                issue,
                result.tested
                        ? "passed"
                        : "skipped",
                result.commitHash
        );
    }

    public WorkflowResult rollback(
            Path repositoryPath
    ) {

        WorkflowResult result =
                new WorkflowResult();

        GitRollbackService.RollbackResult rollbackResult =
                gitRollbackService.rollback(
                        repositoryPath
                );

        if (!rollbackResult.success()) {

            result.status =
                    "ROLLBACK_FAILED";

            result.message =
                    rollbackResult.message();

            return result;
        }

        result.status =
                "ROLLED_BACK";

        result.message =
                rollbackResult.message();

        return result;
    }

    private record CommandResult(
            boolean success,
            String output
    ) {
    }

    private record VerificationResult(
            boolean success,
            String message,
            String output
    ) {
    }

    public static class WorkflowResult {

        private String status;
        private String message;
        private String branchName;
        private String commitHash;
        private String generatedPatch;
        private String gitDiff;
        private String compileOutput;
        private String testOutput;
        private String pullRequestNumber;
        private String pullRequestUrl;

        private List<String> sources;
        private List<String> changedFiles;

        private boolean branchCreated;
        private boolean committed;
        private boolean pushed;
        private boolean compiled;
        private boolean tested;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getBranchName() {
            return branchName;
        }

        public void setBranchName(String branchName) {
            this.branchName = branchName;
        }

        public String getCommitHash() {
            return commitHash;
        }

        public void setCommitHash(String commitHash) {
            this.commitHash = commitHash;
        }

        public String getGeneratedPatch() {
            return generatedPatch;
        }

        public void setGeneratedPatch(
                String generatedPatch
        ) {
            this.generatedPatch = generatedPatch;
        }

        public String getGitDiff() {
            return gitDiff;
        }

        public void setGitDiff(String gitDiff) {
            this.gitDiff = gitDiff;
        }

        public String getCompileOutput() {
            return compileOutput;
        }

        public void setCompileOutput(
                String compileOutput
        ) {
            this.compileOutput = compileOutput;
        }

        public String getTestOutput() {
            return testOutput;
        }

        public void setTestOutput(
                String testOutput
        ) {
            this.testOutput = testOutput;
        }

        public String getPullRequestNumber() {
            return pullRequestNumber;
        }

        public void setPullRequestNumber(
                String pullRequestNumber
        ) {
            this.pullRequestNumber =
                    pullRequestNumber;
        }

        public String getPullRequestUrl() {
            return pullRequestUrl;
        }

        public void setPullRequestUrl(
                String pullRequestUrl
        ) {
            this.pullRequestUrl =
                    pullRequestUrl;
        }

        public List<String> getSources() {
            return sources;
        }

        public void setSources(
                List<String> sources
        ) {
            this.sources = sources;
        }

        public List<String> getChangedFiles() {
            return changedFiles;
        }

        public void setChangedFiles(
                List<String> changedFiles
        ) {
            this.changedFiles = changedFiles;
        }

        public boolean isBranchCreated() {
            return branchCreated;
        }

        public void setBranchCreated(
                boolean branchCreated
        ) {
            this.branchCreated =
                    branchCreated;
        }

        public boolean isCommitted() {
            return committed;
        }

        public void setCommitted(boolean committed) {
            this.committed = committed;
        }

        public boolean isPushed() {
            return pushed;
        }

        public void setPushed(boolean pushed) {
            this.pushed = pushed;
        }

        public boolean isCompiled() {
            return compiled;
        }

        public void setCompiled(boolean compiled) {
            this.compiled = compiled;
        }

        public boolean isTested() {
            return tested;
        }

        public void setTested(boolean tested) {
            this.tested = tested;
        }
    }
}