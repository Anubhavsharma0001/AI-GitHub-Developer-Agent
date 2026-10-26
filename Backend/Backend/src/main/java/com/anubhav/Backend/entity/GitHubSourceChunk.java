package com.anubhav.Backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "github_source_chunks")
public class GitHubSourceChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "source_file_id", nullable = false)
    private GitHubSourceFileEntity sourceFile;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Integer startLine;

    private Integer endLine;

    public GitHubSourceChunk() {
    }

    public GitHubSourceChunk(
            GitHubSourceFileEntity sourceFile,
            String content,
            Integer startLine,
            Integer endLine
    ) {
        this.sourceFile = sourceFile;
        this.content = content;
        this.startLine = startLine;
        this.endLine = endLine;
    }

    public Long getId() {
        return id;
    }

    public GitHubSourceFileEntity getSourceFile() {
        return sourceFile;
    }

    public String getContent() {
        return content;
    }

    public Integer getStartLine() {
        return startLine;
    }

    public Integer getEndLine() {
        return endLine;
    }

    public void setSourceFile(
            GitHubSourceFileEntity sourceFile
    ) {
        this.sourceFile = sourceFile;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setStartLine(Integer startLine) {
        this.startLine = startLine;
    }

    public void setEndLine(Integer endLine) {
        this.endLine = endLine;
    }
}