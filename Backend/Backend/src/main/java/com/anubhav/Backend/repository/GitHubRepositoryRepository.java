package com.anubhav.Backend.repository;

import com.anubhav.Backend.entity.GitHubRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GitHubRepositoryRepository
        extends JpaRepository<GitHubRepository, Long> {

    Optional<GitHubRepository> findByFullName(String fullName);
}