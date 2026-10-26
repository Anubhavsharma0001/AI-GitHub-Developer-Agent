package com.anubhav.Backend.service;

import com.anubhav.Backend.dto.GitHubRepositoryResponse;
import com.anubhav.Backend.entity.GitHubRepository;
import com.anubhav.Backend.repository.GitHubRepositoryRepository;
import org.springframework.stereotype.Service;

@Service
public class GitHubRepositoryDatabaseService {

    private final GitHubRepositoryRepository
            gitHubRepositoryRepository;

    public GitHubRepositoryDatabaseService(
            GitHubRepositoryRepository gitHubRepositoryRepository
    ) {
        this.gitHubRepositoryRepository =
                gitHubRepositoryRepository;
    }

    public GitHubRepository saveRepository(
            String owner,
            GitHubRepositoryResponse repositoryResponse
    ) {

        String fullName =
                repositoryResponse.getFullName();

        GitHubRepository existingRepository =
                gitHubRepositoryRepository
                        .findByFullName(fullName)
                        .orElse(null);

        if (existingRepository != null) {

            existingRepository.setOwner(owner);

            existingRepository.setRepositoryName(
                    repositoryResponse.getName()
            );

            existingRepository.setDefaultBranch(
                    repositoryResponse.getDefaultBranch()
            );

            existingRepository.setLanguage(
                    repositoryResponse.getLanguage()
            );

            return gitHubRepositoryRepository.save(
                    existingRepository
            );
        }

        GitHubRepository repository =
                new GitHubRepository(
                        owner,
                        repositoryResponse.getName(),
                        repositoryResponse.getFullName(),
                        repositoryResponse.getDefaultBranch(),
                        repositoryResponse.getLanguage()
                );

        return gitHubRepositoryRepository.save(repository);
    }
}