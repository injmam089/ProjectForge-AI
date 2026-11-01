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
}
