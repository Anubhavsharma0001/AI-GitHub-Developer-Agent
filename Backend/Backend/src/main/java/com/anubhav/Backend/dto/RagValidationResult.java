package com.anubhav.Backend.dto;

import java.util.List;

public class RagValidationResult {

    private boolean grounded;
    private List<String> supportedClaims;
    private List<String> unsupportedClaims;

    public RagValidationResult() {
    }

    public RagValidationResult(
            boolean grounded,
            List<String> supportedClaims,
            List<String> unsupportedClaims
    ) {
        this.grounded = grounded;
        this.supportedClaims = supportedClaims;
        this.unsupportedClaims = unsupportedClaims;
    }

    public boolean isGrounded() {
        return grounded;
    }

    public List<String> getSupportedClaims() {
        return supportedClaims;
    }

    public List<String> getUnsupportedClaims() {
        return unsupportedClaims;
    }

    public void setGrounded(boolean grounded) {
        this.grounded = grounded;
    }

    public void setSupportedClaims(
            List<String> supportedClaims
    ) {
        this.supportedClaims = supportedClaims;
    }

    public void setUnsupportedClaims(
            List<String> unsupportedClaims
    ) {
        this.unsupportedClaims = unsupportedClaims;
    }
}