package com.anubhav.Backend.github;

import com.anubhav.Backend.dto.GitHubFileContentResponse;
import com.anubhav.Backend.dto.GitHubFileResponse;
import com.anubhav.Backend.dto.GitHubRepositoryResponse;
import com.anubhav.Backend.dto.GitHubSourceFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class GitHubRepositoryService {

    private final GitHubInstallationService gitHubInstallationService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GitHubRepositoryService(
            GitHubInstallationService gitHubInstallationService,
            ObjectMapper objectMapper
    ) {
        this.gitHubInstallationService = gitHubInstallationService;
        this.objectMapper = objectMapper;

        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .build();
    }

    public GitHubRepositoryResponse getRepository(
            Long installationId,
            String owner,
            String repository
    ) throws Exception {

        String installationToken =
                gitHubInstallationService
                        .generateInstallationToken(installationId);

        String response =
                restClient
                        .get()
                        .uri(
                                "/repos/{owner}/{repository}",
                                owner,
                                repository
                        )
                        .header(
                                "Authorization",
                                "Bearer " + installationToken
                        )
                        .header(
                                "Accept",
                                "application/vnd.github+json"
                        )
                        .header(
                                "X-GitHub-Api-Version",
                                "2026-03-10"
                        )
                        .header(
                                "User-Agent",
                                "AI-GitHub-Developer-Agent"
                        )
                        .retrieve()
                        .body(String.class);

        JsonNode jsonNode =
                objectMapper.readTree(response);

        return new GitHubRepositoryResponse(
                jsonNode.get("name").asText(),
                jsonNode.get("full_name").asText(),
                jsonNode.get("description").isNull()
                        ? null
                        : jsonNode.get("description").asText(),
                jsonNode.get("private").asBoolean(),
                jsonNode.get("default_branch").asText(),
                jsonNode.get("language").isNull()
                        ? null
                        : jsonNode.get("language").asText()
        );
    }

    public List<GitHubFileResponse> getFiles(
            Long installationId,
            String owner,
            String repository,
            String path
    ) throws Exception {

        String installationToken =
                gitHubInstallationService
                        .generateInstallationToken(installationId);

        String response;

        if (path == null || path.isBlank()) {

            response =
                    restClient
                            .get()
                            .uri(
                                    "/repos/{owner}/{repository}/contents",
                                    owner,
                                    repository
                            )
                            .header(
                                    "Authorization",
                                    "Bearer " + installationToken
                            )
                            .header(
                                    "Accept",
                                    "application/vnd.github+json"
                            )
                            .header(
                                    "X-GitHub-Api-Version",
                                    "2026-03-10"
                            )
                            .header(
                                    "User-Agent",
                                    "AI-GitHub-Developer-Agent"
                            )
                            .retrieve()
                            .body(String.class);

        } else {

            response =
                    restClient
                            .get()
                            .uri(
                                    "/repos/{owner}/{repository}/contents/{path}",
                                    owner,
                                    repository,
                                    path
                            )
                            .header(
                                    "Authorization",
                                    "Bearer " + installationToken
                            )
                            .header(
                                    "Accept",
                                    "application/vnd.github+json"
                            )
                            .header(
                                    "X-GitHub-Api-Version",
                                    "2026-03-10"
                            )
                            .header(
                                    "User-Agent",
                                    "AI-GitHub-Developer-Agent"
                            )
                            .retrieve()
                            .body(String.class);
        }

        JsonNode jsonArray =
                objectMapper.readTree(response);

        List<GitHubFileResponse> files =
                new ArrayList<>();

        for (JsonNode item : jsonArray) {

            files.add(
                    new GitHubFileResponse(
                            item.get("name").asText(),
                            item.get("path").asText(),
                            item.get("type").asText()
                    )
            );
        }

        return files;
    }

    public GitHubFileContentResponse getFileContent(
            Long installationId,
            String owner,
            String repository,
            String path
    ) throws Exception {

        String installationToken =
                gitHubInstallationService
                        .generateInstallationToken(installationId);

        String response =
                restClient
                        .get()
                        .uri(
                                "/repos/{owner}/{repository}/contents/{path}",
                                owner,
                                repository,
                                path
                        )
                        .header(
                                "Authorization",
                                "Bearer " + installationToken
                        )
                        .header(
                                "Accept",
                                "application/vnd.github+json"
                        )
                        .header(
                                "X-GitHub-Api-Version",
                                "2026-03-10"
                        )
                        .header(
                                "User-Agent",
                                "AI-GitHub-Developer-Agent"
                        )
                        .retrieve()
                        .body(String.class);

        JsonNode jsonNode =
                objectMapper.readTree(response);

        String encodedContent =
                jsonNode.get("content").asText();

        encodedContent =
                encodedContent.replaceAll("\\s", "");

        byte[] decodedBytes =
                Base64.getDecoder().decode(encodedContent);

        String content =
                new String(
                        decodedBytes,
                        StandardCharsets.UTF_8
                );

        return new GitHubFileContentResponse(
                jsonNode.get("name").asText(),
                jsonNode.get("path").asText(),
                content
        );
    }

    public List<GitHubFileResponse> getAllFiles(
            Long installationId,
            String owner,
            String repository
    ) throws Exception {

        List<GitHubFileResponse> allFiles =
                new ArrayList<>();

        exploreDirectory(
                installationId,
                owner,
                repository,
                "",
                allFiles
        );

        return allFiles;
    }

    private void exploreDirectory(
            Long installationId,
            String owner,
            String repository,
            String path,
            List<GitHubFileResponse> allFiles
    ) throws Exception {

        List<GitHubFileResponse> currentItems =
                getFiles(
                        installationId,
                        owner,
                        repository,
                        path
                );

        for (GitHubFileResponse item : currentItems) {

            if ("file".equals(item.getType())) {

                allFiles.add(item);

            } else if ("dir".equals(item.getType())) {

                exploreDirectory(
                        installationId,
                        owner,
                        repository,
                        item.getPath(),
                        allFiles
                );
            }
        }
    }

    public GitHubSourceFile getSourceFile(
            Long installationId,
            String owner,
            String repository,
            String path
    ) throws Exception {

        GitHubFileContentResponse fileContent =
                getFileContent(
                        installationId,
                        owner,
                        repository,
                        path
                );

        return new GitHubSourceFile(
                fileContent.getName(),
                fileContent.getPath(),
                fileContent.getContent()
        );
    }

    public List<GitHubSourceFile> getAllSourceFiles(
            Long installationId,
            String owner,
            String repository
    ) throws Exception {

        List<GitHubFileResponse> allFiles =
                getAllFiles(
                        installationId,
                        owner,
                        repository
                );

        List<GitHubFileResponse> filteredFiles =
                allFiles.stream()
                        .filter(file ->
                                file.getPath()
                                        .toLowerCase()
                                        .matches(
                                                ".*\\.(java|py|js|ts|jsx|tsx|cpp|c|h|hpp|cs|go|rs|php|rb|kt|swift|html|css|scss|xml|json|yml|yaml|md|txt|sql)$"
                                        )
                        )
                        .toList();

        List<GitHubSourceFile> sourceFiles =
                new ArrayList<>();

        for (GitHubFileResponse file : filteredFiles) {

            GitHubSourceFile sourceFile =
                    getSourceFile(
                            installationId,
                            owner,
                            repository,
                            file.getPath()
                    );

            sourceFiles.add(sourceFile);
        }

        return sourceFiles;
    }
}