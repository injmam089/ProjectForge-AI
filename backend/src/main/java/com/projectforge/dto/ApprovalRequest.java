package com.projectforge.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for human-in-the-loop approval of project requirements.
 * Approval must be explicit; confirmation cannot be false or null.
 */
public record ApprovalRequest(
        @NotNull(message = "Approval confirmation must be provided")
        @AssertTrue(message = "Approval confirmation must be explicitly set to true")
        Boolean confirmation
) {}
