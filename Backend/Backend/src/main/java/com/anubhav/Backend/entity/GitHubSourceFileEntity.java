package com.anubhav.Backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "github_source_files")
public class GitHubSourceFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "repository_id", nullable = false)
    private GitHubRepository repository;

    private String name;

    @Column(length = 1000)
    private String path;

    @Column(columnDefinition = "TEXT")
    private String content;

    public GitHubSourceFileEntity() {
    }

    public GitHubSourceFileEntity(
            GitHubRepository repository,
            String name,
            String path,
            String content
    ) {
        this.repository = repository;
        this.name = name;
        this.path = path;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public GitHubRepository getRepository() {
        return repository;
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

    public void setRepository(GitHubRepository repository) {
        this.repository = repository;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setContent(String content) {
        this.content = content;
    }
}