package com.projectforge.dto;

/**
 * Summary metrics of projects owned by the authenticated user.
 */
public record ProjectStatsResponse(
        long totalProjects,
        long draft,
        long readyForAnalysis,
        long analyzing,
        long underReview,
        long requirementsReady,
        long approved,
        long generating,
        long generated,
        long exported
) {
    public static ProjectStatsResponse empty() {
        return new ProjectStatsResponse(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
