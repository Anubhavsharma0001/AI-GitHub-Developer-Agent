package com.anubhav.Backend.controller;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class PatchApprovalController {

    // =====================================================
    // APPROVE PATCH
    // =====================================================

    @PostMapping("/approve-patch")
    public Map<String, Object> approvePatch(
            @RequestParam boolean approved,
            @RequestBody String patch
    ) {

        if (patch == null || patch.isBlank()) {

            return Map.of(
                    "approved", false,
                    "status", "REJECTED",
                    "message", "Patch is empty."
            );
        }

        if (!approved) {

            return Map.of(
                    "approved", false,
                    "status", "REJECTED",
                    "message", "Patch was rejected by the user."
            );
        }

        return Map.of(
                "approved", true,
                "status", "APPROVED",
                "message",
                "Patch approved successfully. "
                        + "It is ready for the apply stage.",
                "patch", patch
        );
    }
}