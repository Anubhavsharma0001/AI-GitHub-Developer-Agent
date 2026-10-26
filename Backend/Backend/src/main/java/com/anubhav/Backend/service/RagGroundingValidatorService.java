package com.anubhav.Backend.service;

import com.anubhav.Backend.dto.RagValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class RagGroundingValidatorService {

    private final OllamaChatService ollamaChatService;
    private final ObjectMapper objectMapper;

    public RagGroundingValidatorService(
            OllamaChatService ollamaChatService,
            ObjectMapper objectMapper
    ) {
        this.ollamaChatService = ollamaChatService;
        this.objectMapper = objectMapper;
    }

    public RagValidationResult validate(
            String answer,
            String repositoryContext
    ) {

        String prompt = """
                You are a strict repository-grounding validator.

                Your task is to check whether the factual claims
                in the ANSWER are explicitly supported by the
                REPOSITORY CONTEXT.

                IMPORTANT RULES:

                1. The repository context is the ONLY source of truth.

                2. Do not use general Java or programming knowledge.

                3. Check every important factual claim independently.

                4. A claim is supported ONLY when the repository
                   context explicitly demonstrates or states it.

                5. A claim is unsupported when the repository
                   context does not provide evidence for it.

                6. Do not assume a standard Java fact is supported
                   merely because it is generally true.

                7. Put supported claims in supportedClaims.

                8. Put unsupported claims in unsupportedClaims.

                9. grounded must be true ONLY when there are no
                   unsupported claims.

                10. Return ONLY valid JSON.
                    Do not return markdown.
                    Do not return ```json.
                    Do not add explanations outside the JSON.

                REQUIRED JSON FORMAT:

                {
                  "grounded": true,
                  "supportedClaims": [
                    "claim 1"
                  ],
                  "unsupportedClaims": []
                }

                REPOSITORY CONTEXT:
                %s

                ANSWER:
                %s

                JSON:
                """.formatted(
                repositoryContext,
                answer
        );

        String rawResult =
                ollamaChatService.ask(prompt);

        if (rawResult == null || rawResult.isBlank()) {

            return new RagValidationResult(
                    false,
                    java.util.List.of(),
                    java.util.List.of(
                            "Validator returned an empty response."
                    )
            );
        }

        try {

            String cleanedResult =
                    cleanJsonResponse(rawResult);

            return objectMapper.readValue(
                    cleanedResult,
                    RagValidationResult.class
            );

        } catch (Exception exception) {

            return new RagValidationResult(
                    false,
                    java.util.List.of(),
                    java.util.List.of(
                            "Validator returned invalid JSON."
                    )
            );
        }
    }

    private String cleanJsonResponse(String response) {

        String cleaned =
                response.trim();

        if (cleaned.startsWith("```json")) {

            cleaned =
                    cleaned.substring(7);
        }

        if (cleaned.startsWith("```")) {

            cleaned =
                    cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {

            cleaned =
                    cleaned.substring(
                            0,
                            cleaned.length() - 3
                    );
        }

        return cleaned.trim();
    }
}