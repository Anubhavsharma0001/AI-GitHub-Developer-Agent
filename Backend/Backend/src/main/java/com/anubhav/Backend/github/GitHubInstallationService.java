package com.anubhav.Backend.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GitHubInstallationService {

    private final GitHubJwtService gitHubJwtService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GitHubInstallationService(
            GitHubJwtService gitHubJwtService,
            ObjectMapper objectMapper
    ) {
        this.gitHubJwtService = gitHubJwtService;
        this.objectMapper = objectMapper;

        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .build();
    }

    public String generateInstallationToken(
            Long installationId
    ) throws Exception {

        String jwt =
                gitHubJwtService.generateToken();

        String response =
                restClient
                        .post()
                        .uri(
                                "/app/installations/{installationId}/access_tokens",
                                installationId
                        )
                        .header(
                                "Authorization",
                                "Bearer " + jwt
                        )
                        .header(
                                "Accept",
                                "application/vnd.github+json"
                        )
                        .header(
                                "X-GitHub-Api-Version",
                                "2026-03-10"
                        )
                        .retrieve()
                        .body(String.class);

        JsonNode jsonNode =
                objectMapper.readTree(response);

        return jsonNode
                .get("token")
                .asText();
    }
}