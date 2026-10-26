package com.anubhav.Backend.dto;

public class VectorSearchResult {

    private Long chunkId;

    private String fileName;
    private String filePath;

    private Integer startLine;
    private Integer endLine;

    private String content;
    private Double distance;

    public VectorSearchResult(
            Long chunkId,
            String fileName,
            String filePath,
            Integer startLine,
            Integer endLine,
            String content,
            Double distance
    ) {
        this.chunkId = chunkId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.startLine = startLine;
        this.endLine = endLine;
        this.content = content;
        this.distance = distance;
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

    public Double getDistance() {
        return distance;
    }
}