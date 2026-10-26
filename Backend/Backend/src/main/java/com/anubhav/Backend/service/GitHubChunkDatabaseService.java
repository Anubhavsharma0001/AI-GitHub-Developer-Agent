
package com.anubhav.Backend.service;

import com.anubhav.Backend.entity.GitHubSourceChunk;
import com.anubhav.Backend.entity.GitHubSourceFileEntity;
import com.anubhav.Backend.repository.GitHubSourceFileRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GitHubChunkDatabaseService {

    private final GitHubSourceFileRepository gitHubSourceFileRepository;
    private final GitHubChunkingService gitHubChunkingService;
    private final VectorStore vectorStore;

    public GitHubChunkDatabaseService(
            GitHubSourceFileRepository gitHubSourceFileRepository,
            GitHubChunkingService gitHubChunkingService,
            VectorStore vectorStore
    ) {
        this.gitHubSourceFileRepository = gitHubSourceFileRepository;
        this.gitHubChunkingService = gitHubChunkingService;
        this.vectorStore = vectorStore;
    }

    @Transactional
    public List<GitHubSourceChunk> createChunksForRepository(
            Long repositoryId
    ) {
        List<GitHubSourceFileEntity> sourceFiles =
                gitHubSourceFileRepository.findByRepositoryId(repositoryId);

        List<GitHubSourceChunk> allChunks = new ArrayList<>();

        for (GitHubSourceFileEntity sourceFile : sourceFiles) {

            List<GitHubSourceChunk> chunks =
                    gitHubChunkingService.createChunks(sourceFile);

            allChunks.addAll(chunks);

            List<Document> documents = new ArrayList<>();

            for (GitHubSourceChunk chunk : chunks) {
                Map<String, Object> metadata = new HashMap<>();

                metadata.put("repository_id", repositoryId);
                metadata.put("source_file_id", sourceFile.getId());
                metadata.put("chunk_id", chunk.getId());
                metadata.put("path", sourceFile.getPath());
                metadata.put("start_line", chunk.getStartLine());
                metadata.put("end_line", chunk.getEndLine());

                documents.add(new Document(chunk.getContent(), metadata));
            }

            if (!documents.isEmpty()) {
                vectorStore.add(documents);
            }
        }

        return allChunks;
    }
}
