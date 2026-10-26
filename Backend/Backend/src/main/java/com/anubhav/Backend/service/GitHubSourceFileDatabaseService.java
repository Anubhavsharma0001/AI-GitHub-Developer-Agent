package com.anubhav.Backend.service;

import com.anubhav.Backend.dto.GitHubSourceFile;
import com.anubhav.Backend.entity.GitHubRepository;
import com.anubhav.Backend.entity.GitHubSourceFileEntity;
import com.anubhav.Backend.repository.GitHubSourceFileRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GitHubSourceFileDatabaseService {

    private final GitHubSourceFileRepository
            gitHubSourceFileRepository;

    public GitHubSourceFileDatabaseService(
            GitHubSourceFileRepository gitHubSourceFileRepository
    ) {
        this.gitHubSourceFileRepository =
                gitHubSourceFileRepository;
    }

    public GitHubSourceFileEntity saveSourceFile(
            GitHubRepository repository,
            GitHubSourceFile sourceFile
    ) {

        GitHubSourceFileEntity entity =
                new GitHubSourceFileEntity(
                        repository,
                        sourceFile.getName(),
                        sourceFile.getPath(),
                        sourceFile.getContent()
                );

        return gitHubSourceFileRepository.save(entity);
    }

    public List<GitHubSourceFileEntity> saveSourceFiles(
            GitHubRepository repository,
            List<GitHubSourceFile> sourceFiles
    ) {

        List<GitHubSourceFileEntity> entities =
                new ArrayList<>();

        for (GitHubSourceFile sourceFile : sourceFiles) {

            GitHubSourceFileEntity entity =
                    new GitHubSourceFileEntity(
                            repository,
                            sourceFile.getName(),
                            sourceFile.getPath(),
                            sourceFile.getContent()
                    );

            entities.add(entity);
        }

        return gitHubSourceFileRepository.saveAll(entities);
    }
}