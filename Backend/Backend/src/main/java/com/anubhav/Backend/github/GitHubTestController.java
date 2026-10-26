
package com.anubhav.Backend.github;

import com.anubhav.Backend.dto.GitHubFileContentResponse;
import com.anubhav.Backend.dto.GitHubFileResponse;
import com.anubhav.Backend.dto.GitHubRepositoryResponse;
import com.anubhav.Backend.dto.GitHubSourceFile;
import com.anubhav.Backend.entity.GitHubRepository;
import com.anubhav.Backend.entity.GitHubSourceChunk;
import com.anubhav.Backend.entity.GitHubSourceFileEntity;
import com.anubhav.Backend.repository.GitHubRepositoryRepository;
import com.anubhav.Backend.service.GitHubChunkDatabaseService;
import com.anubhav.Backend.service.GitHubRepositoryDatabaseService;
import com.anubhav.Backend.service.GitHubSourceFileDatabaseService;
import com.anubhav.Backend.service.OllamaChatService;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/github")
public class GitHubTestController {

    private final GitHubJwtService gitHubJwtService;
    private final GitHubInstallationService gitHubInstallationService;
    private final GitHubRepositoryService gitHubRepositoryService;
    private final GitHubFileFilterService gitHubFileFilterService;
    private final GitHubRepositoryDatabaseService gitHubRepositoryDatabaseService;
    private final GitHubSourceFileDatabaseService gitHubSourceFileDatabaseService;
    private final GitHubChunkDatabaseService gitHubChunkDatabaseService;
    private final GitHubRepositoryRepository gitHubRepositoryRepository;
    private final VectorStore vectorStore;
    private final OllamaChatService ollamaChatService;

    public GitHubTestController(
            GitHubJwtService gitHubJwtService,
            GitHubInstallationService gitHubInstallationService,
            GitHubRepositoryService gitHubRepositoryService,
            GitHubFileFilterService gitHubFileFilterService,
            GitHubRepositoryDatabaseService gitHubRepositoryDatabaseService,
            GitHubSourceFileDatabaseService gitHubSourceFileDatabaseService,
            GitHubChunkDatabaseService gitHubChunkDatabaseService,
            GitHubRepositoryRepository gitHubRepositoryRepository,
            VectorStore vectorStore,
            OllamaChatService ollamaChatService
    ) {
        this.gitHubJwtService = gitHubJwtService;
        this.gitHubInstallationService = gitHubInstallationService;
        this.gitHubRepositoryService = gitHubRepositoryService;
        this.gitHubFileFilterService = gitHubFileFilterService;
        this.gitHubRepositoryDatabaseService = gitHubRepositoryDatabaseService;
        this.gitHubSourceFileDatabaseService = gitHubSourceFileDatabaseService;
        this.gitHubChunkDatabaseService = gitHubChunkDatabaseService;
        this.gitHubRepositoryRepository = gitHubRepositoryRepository;
        this.vectorStore = vectorStore;
        this.ollamaChatService = ollamaChatService;
    }

    @GetMapping("/token")
    public String generateToken() throws Exception {
        return gitHubJwtService.generateToken();
    }

    @GetMapping("/callback")
    public String callback(
            @RequestParam("installation_id") Long installationId,
            @RequestParam(value = "setup_action", required = false)
            String setupAction
    ) {
        return "GitHub App installed successfully. Installation ID: "
                + installationId + ", Setup Action: " + setupAction;
    }

    @GetMapping("/installation-token")
    public String generateInstallationToken(
            @RequestParam("installation_id") Long installationId
    ) throws Exception {
        return gitHubInstallationService
                .generateInstallationToken(installationId);
    }

    @GetMapping("/repository")
    public GitHubRepositoryResponse getRepository(
            @RequestParam("installation_id") Long installationId,
            @RequestParam("owner") String owner,
            @RequestParam("repository") String repository
    ) throws Exception {
        return gitHubRepositoryService.getRepository(
                installationId, owner, repository);
    }

    @GetMapping("/files")
    public List<GitHubFileResponse> getFiles(
            @RequestParam("installation_id") Long installationId,
            @RequestParam("owner") String owner,
            @RequestParam("repository") String repository,
            @RequestParam(value = "path", required = false) String path
    ) throws Exception {
        return gitHubRepositoryService.getFiles(
                installationId, owner, repository, path);
    }

    @GetMapping("/file")
    public GitHubFileContentResponse getFileContent(
            @RequestParam("installation_id") Long installationId,
            @RequestParam("owner") String owner,
            @RequestParam("repository") String repository,
            @RequestParam("path") String path
    ) throws Exception {
        return gitHubRepositoryService.getFileContent(
                installationId, owner, repository, path);
    }

    @GetMapping("/all-files")
    public List<GitHubFileResponse> getAllFiles(
            @RequestParam("installation_id") Long installationId,
            @RequestParam("owner") String owner,
            @RequestParam("repository") String repository
    ) throws Exception {
        return gitHubRepositoryService.getAllFiles(
                installationId, owner, repository);
    }

    @GetMapping("/filtered-files")
    public List<GitHubFileResponse> getFilteredFiles(
            @RequestParam("installation_id") Long installationId,
            @RequestParam("owner") String owner,
            @RequestParam("repository") String repository
    ) throws Exception {
        List<GitHubFileResponse> files =
                gitHubRepositoryService.getAllFiles(
                        installationId, owner, repository);

        return gitHubFileFilterService.filterFiles(files);
    }

    @GetMapping("/source-file")
    public GitHubSourceFile getSourceFile(
            @RequestParam("installation_id") Long installationId,
            @RequestParam("owner") String owner,
            @RequestParam("repository") String repository,
            @RequestParam("path") String path
    ) throws Exception {
        return gitHubRepositoryService.getSourceFile(
                installationId, owner, repository, path);
    }

    @GetMapping("/source-files")
    public List<GitHubSourceFile> getAllSourceFiles(
            @RequestParam("installation_id") Long installationId,
            @RequestParam("owner") String owner,
            @RequestParam("repository") String repository
    ) throws Exception {
        return gitHubRepositoryService.getAllSourceFiles(
                installationId, owner, repository);
    }

    @PostMapping("/save-repository")
    public GitHubRepository saveRepository(
            @RequestParam("installation_id") Long installationId,
            @RequestParam("owner") String owner,
            @RequestParam("repository") String repository
    ) throws Exception {
        GitHubRepositoryResponse response =
                gitHubRepositoryService.getRepository(
                        installationId, owner, repository);

        return gitHubRepositoryDatabaseService.saveRepository(
                owner, response);
    }

    @PostMapping("/save-source-files")
    public List<GitHubSourceFileEntity> saveSourceFiles(
            @RequestParam("installation_id") Long installationId,
            @RequestParam("owner") String owner,
            @RequestParam("repository") String repository
    ) throws Exception {
        GitHubRepositoryResponse response =
                gitHubRepositoryService.getRepository(
                        installationId, owner, repository);

        GitHubRepository savedRepository =
                gitHubRepositoryDatabaseService.saveRepository(
                        owner, response);

        List<GitHubSourceFile> files =
                gitHubRepositoryService.getAllSourceFiles(
                        installationId, owner, repository);

        return gitHubSourceFileDatabaseService.saveSourceFiles(
                savedRepository, files);
    }

    @PostMapping("/create-chunks")
    public List<GitHubSourceChunk> createChunks(
            @RequestParam("repository_id") Long repositoryId
    ) {
        validateRepository(repositoryId);

        return gitHubChunkDatabaseService
                .createChunksForRepository(repositoryId);
    }

    @GetMapping("/search-code")
    public List<Map<String, Object>> searchCode(
            @RequestParam("query") String query,
            @RequestParam("repository_id") Long repositoryId
    ) {
        validateQuestion(query);
        validateRepository(repositoryId);

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(5)
                .filterExpression("repository_id == " + repositoryId)
                .build();

        List<Document> documents =
                vectorStore.similaritySearch(request);

        return documents.stream().map(document -> {
            Map<String, Object> result = new HashMap<>();

            result.put("content",
                    document.getText() == null
                            ? "" : document.getText());
            result.put("metadata", document.getMetadata());

            return result;
        }).toList();
    }

    @PostMapping("/ask-repository")
    public Map<String, Object> askRepository(
            @RequestParam("repository_id") Long repositoryId,
            @RequestParam("question") String question
    ) {
        validateQuestion(question);
        validateRepository(repositoryId);

        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(5)
                .filterExpression("repository_id == " + repositoryId)
                .build();

        List<Document> documents =
                vectorStore.similaritySearch(request);

        if (documents == null || documents.isEmpty()) {
            throw new IllegalArgumentException(
                    "No relevant code found for this repository. "
                            + "Ensure its source files have been indexed."
            );
        }

        StringBuilder context = new StringBuilder();
        List<Map<String, Object>> sources = new ArrayList<>();

        for (Document document : documents) {
            Map<String, Object> metadata = document.getMetadata();

            Object path = metadata.get("path");
            Object startLine = metadata.get("start_line");
            Object endLine = metadata.get("end_line");

            context.append("\n--- File: ")
                    .append(path == null ? "Unknown file" : path);

            if (startLine != null || endLine != null) {
                context.append(" (lines ")
                        .append(startLine == null ? "?" : startLine)
                        .append("-")
                        .append(endLine == null ? "?" : endLine)
                        .append(")");
            }

            context.append(" ---\n")
                    .append(document.getText() == null
                            ? "" : document.getText())
                    .append("\n");

            Map<String, Object> source = new HashMap<>();
            source.put("file",
                    path == null ? "Unknown file" : path);
            source.put("start_line", startLine);
            source.put("end_line", endLine);

            sources.add(source);
        }

        String answer = ollamaChatService.askWithContext(
                question,
                context.toString()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("repository_id", repositoryId);
        response.put("question", question);
        response.put("answer", answer);
        response.put("retrieved_chunks", documents.size());
        response.put("sources", sources);

        return response;
    }

    private void validateQuestion(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "Question must not be empty");
        }
    }

    private void validateRepository(Long repositoryId) {
        if (repositoryId == null || repositoryId <= 0) {
            throw new IllegalArgumentException(
                    "A valid repository_id is required");
        }

        gitHubRepositoryRepository.findById(repositoryId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Repository not found: " + repositoryId));
    }
}
