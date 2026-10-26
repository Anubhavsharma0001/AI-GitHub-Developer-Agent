package com.anubhav.Backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "github_repositories")
public class GitHubRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String owner;

    private String repositoryName;

    private String fullName;

    private String defaultBranch;

    private String language;

    public GitHubRepository() {
    }

    public GitHubRepository(
            String owner,
            String repositoryName,
            String fullName,
            String defaultBranch,
            String language
    ) {
        this.owner = owner;
        this.repositoryName = repositoryName;
        this.fullName = fullName;
        this.defaultBranch = defaultBranch;
        this.language = language;
    }

    public Long getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public String getLanguage() {
        return language;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}