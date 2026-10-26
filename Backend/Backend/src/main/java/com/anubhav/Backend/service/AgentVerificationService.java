package com.anubhav.Backend.service;

import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class AgentVerificationService {

    private final CompileTestService compileTestService;
    private final TestExecutionService testExecutionService;

    public AgentVerificationService(
            CompileTestService compileTestService,
            TestExecutionService testExecutionService
    ) {
        this.compileTestService =
                compileTestService;

        this.testExecutionService =
                testExecutionService;
    }

    public VerificationResult verify(
            Path repositoryPath,
            Path testFile
    ) {

        // =====================================================
        // STEP 1: COMPILE
        // =====================================================

        CompileTestService.CompileResult compileResult =
                compileTestService.compile(
                        repositoryPath
                );

        if (!compileResult.success()) {

            return new VerificationResult(
                    false,
                    "COMPILE_FAILED",
                    compileResult.output(),
                    null
            );
        }

        // =====================================================
        // STEP 2: RUN TARGETED TEST
        // =====================================================

        TestExecutionService.TestExecutionResult testResult =
                testExecutionService.runTest(
                        repositoryPath,
                        testFile
                );

        // =====================================================
        // STEP 3: INTERPRET RESULT
        // =====================================================

        if ("PASS".equals(testResult.status())) {

            return new VerificationResult(
                    true,
                    "VERIFIED",
                    testResult.output(),
                    testResult
            );
        }

        if ("NOT_RUN".equals(testResult.status())) {

            return new VerificationResult(
                    false,
                    "TEST_NOT_RUN",
                    testResult.message(),
                    testResult
            );
        }

        if ("TIMEOUT".equals(testResult.status())) {

            return new VerificationResult(
                    false,
                    "TEST_TIMEOUT",
                    testResult.message(),
                    testResult
            );
        }

        return new VerificationResult(
                false,
                "TEST_FAILED",
                testResult.output(),
                testResult
        );
    }

    public record VerificationResult(
            boolean verified,
            String status,
            String output,
            TestExecutionService.TestExecutionResult testResult
    ) {
    }
}