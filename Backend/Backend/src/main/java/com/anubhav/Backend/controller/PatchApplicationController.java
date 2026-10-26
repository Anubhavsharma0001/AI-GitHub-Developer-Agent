package com.anubhav.Backend.controller;

import com.anubhav.Backend.service.PatchApplicationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
public class PatchApplicationController {

    private final PatchApplicationService
            patchApplicationService;

    public PatchApplicationController(
            PatchApplicationService patchApplicationService
    ) {
        this.patchApplicationService =
                patchApplicationService;
    }

    @PostMapping("/apply-patch")
    public PatchApplicationService.PatchApplicationResult applyPatch(
            @RequestParam Long repositoryId,
            @RequestParam boolean approved,
            @RequestBody String patch
    ) {

        // =====================================================
        // HUMAN APPROVAL CHECK
        // =====================================================

        if (!approved) {

            return new PatchApplicationService.PatchApplicationResult(
                    false,
                    "Patch was not approved. "
                            + "No changes were made.",
                    java.util.List.of()
            );
        }

        // =====================================================
        // APPLY APPROVED PATCH
        // =====================================================

        return patchApplicationService.applyPatch(
                repositoryId,
                patch
        );
    }
}