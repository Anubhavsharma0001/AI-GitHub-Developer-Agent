package com.anubhav.Backend.dto;

import java.util.List;

public class RagClaimEvidenceResult {

    private boolean supported;
    private double evidenceScore;
    private List<String> matchedEvidence;

    public RagClaimEvidenceResult() {
    }

    public RagClaimEvidenceResult(
            boolean supported,
            double evidenceScore,
            List<String> matchedEvidence
    ) {
        this.supported = supported;
        this.evidenceScore = evidenceScore;
        this.matchedEvidence = matchedEvidence;
    }

    public boolean isSupported() {
        return supported;
    }

    public double getEvidenceScore() {
        return evidenceScore;
    }

    public List<String> getMatchedEvidence() {
        return matchedEvidence;
    }

    public void setSupported(boolean supported) {
        this.supported = supported;
    }

    public void setEvidenceScore(double evidenceScore) {
        this.evidenceScore = evidenceScore;
    }

    public void setMatchedEvidence(
            List<String> matchedEvidence
    ) {
        this.matchedEvidence = matchedEvidence;
    }
}