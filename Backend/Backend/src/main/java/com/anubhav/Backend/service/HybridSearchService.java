package com.anubhav.Backend.service;

import com.anubhav.Backend.dto.HybridSearchResult;
import com.anubhav.Backend.dto.VectorSearchResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HybridSearchService {

    private static final double MIN_RELEVANCE_SCORE = 0.60;

    private static final double MAX_SCORE_GAP_FROM_TOP = 0.08;

    private final VectorSearchService vectorSearchService;
    private final KeywordSearchService keywordSearchService;

    public HybridSearchService(
            VectorSearchService vectorSearchService,
            KeywordSearchService keywordSearchService
    ) {
        this.vectorSearchService = vectorSearchService;
        this.keywordSearchService = keywordSearchService;
    }

    public List<HybridSearchResult> search(
            String query,
            float[] queryEmbedding,
            int limit
    ) {

        List<VectorSearchResult> vectorResults =
                vectorSearchService.search(
                        queryEmbedding,
                        limit
                );

        List<VectorSearchResult> keywordResults =
                keywordSearchService.search(
                        query,
                        limit
                );

        Map<Long, VectorSearchResult> uniqueResults =
                new LinkedHashMap<>();

        for (VectorSearchResult result : vectorResults) {

            uniqueResults.put(
                    result.getChunkId(),
                    result
            );
        }

        for (VectorSearchResult result : keywordResults) {

            uniqueResults.putIfAbsent(
                    result.getChunkId(),
                    result
            );
        }

        List<HybridSearchResult> results =
                new ArrayList<>();

        for (VectorSearchResult result :
                uniqueResults.values()) {

            boolean keywordMatch = false;

            for (VectorSearchResult keywordResult :
                    keywordResults) {

                if (keywordResult.getChunkId()
                        .equals(result.getChunkId())) {

                    keywordMatch = true;
                    break;
                }
            }

            Double vectorDistance = null;

            for (VectorSearchResult vectorResult :
                    vectorResults) {

                if (vectorResult.getChunkId()
                        .equals(result.getChunkId())) {

                    vectorDistance =
                            vectorResult.getDistance();

                    break;
                }
            }

            double vectorScore = 0.0;

            if (vectorDistance != null) {

                vectorScore =
                        1.0 - vectorDistance;
            }

            double keywordBonus =
                    keywordMatch
                            ? 0.30
                            : 0.0;

            double finalScore;

            if (vectorDistance == null
                    && keywordMatch) {

                finalScore = 0.80;

            } else {

                finalScore =
                        vectorScore + keywordBonus;
            }

            results.add(
                    new HybridSearchResult(
                            result.getChunkId(),
                            result.getFileName(),
                            result.getFilePath(),
                            result.getStartLine(),
                            result.getEndLine(),
                            result.getContent(),
                            vectorDistance,
                            keywordMatch,
                            finalScore
                    )
            );
        }

        /*
         * Highest relevance first.
         */
        results.sort(
                (first, second) ->
                        Double.compare(
                                second.getFinalScore(),
                                first.getFinalScore()
                        )
        );

        if (results.isEmpty()) {
            return results;
        }

        /*
         * The strongest result becomes our reference score.
         */
        double topScore =
                results.get(0).getFinalScore();

        List<HybridSearchResult> filteredResults =
                new ArrayList<>();

        for (HybridSearchResult result : results) {

            double score =
                    result.getFinalScore();

            /*
             * Keep a result only when:
             *
             * 1. It passes the minimum relevance threshold.
             *
             * 2. It is reasonably close to the strongest
             *    retrieved result.
             */
            boolean passesMinimumScore =
                    score >= MIN_RELEVANCE_SCORE;

            boolean closeToTopResult =
                    (topScore - score)
                            <= MAX_SCORE_GAP_FROM_TOP;

            if (passesMinimumScore
                    && closeToTopResult) {

                filteredResults.add(result);
            }
        }

        /*
         * Keep at most the requested number of results.
         */
        if (filteredResults.size() > limit) {

            return filteredResults.subList(
                    0,
                    limit
            );
        }

        return filteredResults;
    }
}