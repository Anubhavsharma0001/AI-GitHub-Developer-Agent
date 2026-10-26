
package com.anubhav.Backend.service;

import com.anubhav.Backend.entity.GitHubSourceChunk;
import com.anubhav.Backend.entity.GitHubSourceFileEntity;
import com.anubhav.Backend.repository.GitHubSourceChunkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class GitHubChunkingService {

    private static final int MAX_CHUNK_SIZE = 80;

    private final GitHubSourceChunkRepository gitHubSourceChunkRepository;

    public GitHubChunkingService(
            GitHubSourceChunkRepository gitHubSourceChunkRepository
    ) {
        this.gitHubSourceChunkRepository = gitHubSourceChunkRepository;
    }

    @Transactional
    public List<GitHubSourceChunk> createChunks(
            GitHubSourceFileEntity sourceFile
    ) {
        if (sourceFile == null || sourceFile.getId() == null) {
            throw new IllegalArgumentException(
                    "Source file must be saved before creating chunks"
            );
        }

        String content = sourceFile.getContent();

        // Remove old chunks so reprocessing the file does not duplicate them.
        gitHubSourceChunkRepository.deleteBySourceFileId(sourceFile.getId());

        List<GitHubSourceChunk> chunks = new ArrayList<>();

        if (content == null || content.isBlank()) {
            return chunks;
        }

        String[] lines = content.split("\\R", -1);

        int start = 0;

        while (start < lines.length) {
            int end = findChunkEnd(lines, start);
            String chunkContent = buildContent(lines, start, end);

            if (isMeaningful(chunkContent)) {
                GitHubSourceChunk chunk = new GitHubSourceChunk(
                        sourceFile,
                        chunkContent,
                        start + 1,
                        end
                );

                chunks.add(chunk);
            }

            start = end;
        }

        return gitHubSourceChunkRepository.saveAll(chunks);
    }

    private int findChunkEnd(String[] lines, int start) {
        int braceBalance = 0;
        boolean foundCodeBlock = false;
        int end = start;

        for (
                int i = start;
                i < lines.length && i < start + MAX_CHUNK_SIZE;
                i++
        ) {
            String line = lines[i].trim();

            if (line.isEmpty()) {
                end = i + 1;
                continue;
            }

            if (line.contains("{")) {
                foundCodeBlock = true;
            }

            for (char character : line.toCharArray()) {
                if (character == '{') {
                    braceBalance++;
                } else if (character == '}') {
                    braceBalance--;
                }
            }

            end = i + 1;

            if (
                    foundCodeBlock
                            && braceBalance == 0
                            && isStructuralLine(line)
            ) {
                break;
            }
        }

        if (end <= start) {
            end = Math.min(start + MAX_CHUNK_SIZE, lines.length);
        }

        return end;
    }

    private boolean isStructuralLine(String line) {
        return line.endsWith("}")
                || line.startsWith("class ")
                || line.startsWith("public class ")
                || line.startsWith("private class ")
                || line.startsWith("protected class ")
                || line.startsWith("interface ")
                || line.startsWith("public interface ")
                || line.startsWith("enum ")
                || line.startsWith("public enum ");
    }

    private String buildContent(String[] lines, int start, int end) {
        StringBuilder content = new StringBuilder();

        for (int i = start; i < end; i++) {
            content.append(lines[i]).append(System.lineSeparator());
        }

        return content.toString();
    }

    private boolean isMeaningful(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }

        String withoutWhitespace = content.replaceAll("\\s+", "");

        return withoutWhitespace.length() >= 10;
    }
}