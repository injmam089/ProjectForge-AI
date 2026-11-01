package com.projectforge.entity;

/**
 * Controlled project lifecycle status across the ProjectForge AI workflow.
 *
 * Lifecycle:
 * DRAFT -> READY_FOR_ANALYSIS -> ANALYZING -> REQUIREMENTS_READY -> UNDER_REVIEW -> APPROVED -> GENERATING -> GENERATED -> EXPORTED
 */
public enum ProjectStatus {
    DRAFT,
    READY_FOR_ANALYSIS,
    ANALYZING,
    REQUIREMENTS_READY,
    UNDER_REVIEW,
    APPROVED,
    GENERATING,
    GENERATED,
    EXPORTED;

    /**
     * Determines whether project metadata and requirements intake can be safely modified.
     */
    public boolean canBeEdited() {
        return this == DRAFT || this == READY_FOR_ANALYSIS;
    }

    /**
     * Determines whether the project can transition into the analysis queue.
     */
    public boolean canPrepareForAnalysis() {
        return this == DRAFT || this == READY_FOR_ANALYSIS;
    }

    /**
     * Determines whether AI requirement analysis can be triggered on this project.
     */
    public boolean canBeAnalyzed() {
        return this == READY_FOR_ANALYSIS || this == DRAFT || this == REQUIREMENTS_READY || this == UNDER_REVIEW;
    }

    /**
     * Determines whether clarifications can be submitted to refine requirements.
     */
    public boolean canSubmitClarifications() {
        return this == REQUIREMENTS_READY || this == UNDER_REVIEW || this == ANALYZING;
    }

    /**
     * Determines whether project requirements can be reviewed and inspected.
     */
    public boolean canBeReviewed() {
        return this == REQUIREMENTS_READY || this == UNDER_REVIEW || this == APPROVED ||
                this == GENERATING || this == GENERATED || this == EXPORTED;
    }

    /**
     * Determines whether project is in an approval-eligible state.
     */
    public boolean canBeApproved() {
        return this == REQUIREMENTS_READY || this == UNDER_REVIEW;
    }

    /**
     * Determines whether the project specification can be edited during human review.
     */
    public boolean canEditSpecification() {
        return this == DRAFT || this == READY_FOR_ANALYSIS || this == REQUIREMENTS_READY || this == UNDER_REVIEW;
    }

    /**
     * Determines whether the project has already been approved.
     */
    public boolean isApproved() {
        return this == APPROVED;
    }

    /**
     * Determines whether documentation generation can be triggered on this project.
     * Allowed only from APPROVED (initial generation) or GENERATED (re-generation).
     */
    public boolean canGenerateDocuments() {
        return this == APPROVED || this == GENERATED;
    }

    /**
     * Determines whether the project is currently generating documentation.
     */
    public boolean isGenerating() {
        return this == GENERATING;
    }

    /**
     * Determines whether documentation has already been generated.
     */
    public boolean isGenerated() {
        return this == GENERATED;
    }
}
