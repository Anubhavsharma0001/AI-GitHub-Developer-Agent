package com.anubhav.Backend.dto;

public class GitHubRepositoryResponse {

    private String name;
    private String fullName;
    private String description;
    private boolean isPrivate;
    private String defaultBranch;
    private String language;

    public GitHubRepositoryResponse(
            String name,
            String fullName,
            String description,
            boolean isPrivate,
            String defaultBranch,
            String language
    ) {
        this.name = name;
        this.fullName = fullName;
        this.description = description;
        this.isPrivate = isPrivate;
        this.defaultBranch = defaultBranch;
        this.language = language;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public String getLanguage() {
        return language;
    }
}