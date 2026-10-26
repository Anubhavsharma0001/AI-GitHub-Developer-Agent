
package com.anubhav.Backend.dto;

public class CodeReviewRequest {

    private Long repositoryId;

    public CodeReviewRequest() {
    }

    public CodeReviewRequest(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }
}
