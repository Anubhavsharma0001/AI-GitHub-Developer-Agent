package com.anubhav.Backend.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/agent")
public class PatchValidationController {

    private final JdbcTemplate jdbcTemplate;

    private static final Pattern FILE_PATTERN =
            Pattern.compile(
                    "(?m)^(?:---|\\+\\+\\+)\\s+[ab]/(.+)$"
            );

    public PatchValidationController(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/validate-patch")
    public PatchValidationResponse validatePatch(
            @RequestParam Long repositoryId,
            @RequestBody String patch
    ) {

        if (patch == null || patch.isBlank()) {

            return new PatchValidationResponse(
                    false,
                    "Patch is empty.",
                    List.of()
            );
        }

        List<String> referencedFiles =
                extractReferencedFiles(patch);

        if (referencedFiles.isEmpty()) {

            return new PatchValidationResponse(
                    false,
                    "No valid files were found in the patch.",
                    List.of()
            );
        }

        List<String> missingFiles =
                new ArrayList<>();

        for (String filePath : referencedFiles) {

            Integer count =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT COUNT(*)
                            FROM github_source_files
                            WHERE repository_id = ?
                              AND path = ?
                            """,
                            Integer.class,
                            repositoryId,
                            filePath
                    );

            if (count == null || count == 0) {
                missingFiles.add(filePath);
            }
        }

        if (!missingFiles.isEmpty()) {

            return new PatchValidationResponse(
                    false,
                    "Patch references files that do not exist.",
                    missingFiles
            );
        }

        return new PatchValidationResponse(
                true,
                "Patch references only existing repository files.",
                referencedFiles
        );
    }

    private List<String> extractReferencedFiles(
            String patch
    ) {

        List<String> files =
                new ArrayList<>();

        Matcher matcher =
                FILE_PATTERN.matcher(patch);

        while (matcher.find()) {

            String file =
                    matcher.group(1).trim();

            if (!files.contains(file)) {
                files.add(file);
            }
        }

        return files;
    }

    public record PatchValidationResponse(
            boolean valid,
            String message,
            List<String> referencedFiles
    ) {
    }
}