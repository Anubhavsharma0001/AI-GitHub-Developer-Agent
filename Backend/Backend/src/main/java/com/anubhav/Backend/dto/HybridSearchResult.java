package com.anubhav.Backend.dto;

public class HybridSearchResult {

    private Long chunkId;

    private String fileName;
    private String filePath;

    private Integer startLine;
    private Integer endLine;

    private String content;

    private Double vectorDistance;
    private Boolean keywordMatch;
    private Double finalScore;

    public HybridSearchResult(
            Long chunkId,
            String fileName,
            String filePath,
            Integer startLine,
            Integer endLine,
            String content,
            Double vectorDistance,
            Boolean keywordMatch,
            Double finalScore
    ) {
        this.chunkId = chunkId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.startLine = startLine;
        this.endLine = endLine;
        this.content = content;
        this.vectorDistance = vectorDistance;
        this.keywordMatch = keywordMatch;
        this.finalScore = finalScore;
    }

    public Long getChunkId() {
        return chunkId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public Integer getStartLine() {
        return startLine;
    }

    public Integer getEndLine() {
        return endLine;
    }

    public String getContent() {
        return content;
    }

    public Double getVectorDistance() {
        return vectorDistance;
    }

    public Boolean getKeywordMatch() {
        return keywordMatch;
    }

    public Double getFinalScore() {
        return finalScore;
    }
}