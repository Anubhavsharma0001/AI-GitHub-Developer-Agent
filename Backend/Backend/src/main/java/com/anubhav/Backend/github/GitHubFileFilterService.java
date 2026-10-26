package com.anubhav.Backend.github;

import com.anubhav.Backend.dto.GitHubFileResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GitHubFileFilterService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".java",
            ".py",
            ".js",
            ".ts",
            ".jsx",
            ".tsx",
            ".cpp",
            ".c",
            ".h",
            ".hpp",
            ".cs",
            ".go",
            ".rs",
            ".php",
            ".rb",
            ".kt",
            ".swift",
            ".html",
            ".css",
            ".scss",
            ".xml",
            ".json",
            ".yml",
            ".yaml",
            ".md",
            ".txt",
            ".sql"
    );

    public List<GitHubFileResponse> filterFiles(
            List<GitHubFileResponse> files
    ) {

        return files.stream()
                .filter(file -> isAllowedFile(file.getPath()))
                .collect(Collectors.toList());
    }

    private boolean isAllowedFile(String path) {

        if (path == null || path.isBlank()) {
            return false;
        }

        String lowerCasePath =
                path.toLowerCase();

        for (String extension : ALLOWED_EXTENSIONS) {

            if (lowerCasePath.endsWith(extension)) {
                return true;
            }
        }

        return false;
    }
}