
package com.anubhav.Backend.repository;

import com.anubhav.Backend.entity.GitHubSourceChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GitHubSourceChunkRepository
        extends JpaRepository<GitHubSourceChunk, Long> {

    List<GitHubSourceChunk> findBySourceFileId(Long sourceFileId);

    void deleteBySourceFileId(Long sourceFileId);
}
