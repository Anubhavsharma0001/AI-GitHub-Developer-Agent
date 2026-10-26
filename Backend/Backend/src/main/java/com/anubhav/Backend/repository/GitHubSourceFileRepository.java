package com.anubhav.Backend.repository;

import com.anubhav.Backend.entity.GitHubSourceFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GitHubSourceFileRepository
        extends JpaRepository<GitHubSourceFileEntity, Long> {

    List<GitHubSourceFileEntity> findByRepositoryId(Long repositoryId);
}