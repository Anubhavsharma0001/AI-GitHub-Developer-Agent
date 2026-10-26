package com.anubhav.Backend.dto;

public class GitHubFileContentResponse {

    private String name;
    private String path;
    private String content;

    public GitHubFileContentResponse(
            String name,
            String path,
            String content
    ) {
        this.name = name;
        this.path = path;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getContent() {
        return content;
    }
}