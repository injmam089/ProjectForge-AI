package com.projectforge.dto;

import java.util.List;

/**
 * Breakdown of a project specification's quality evaluation score across six core dimensions.
 * Evaluates:
 * 1. Completeness (25 points)
 * 2. Clarity (15 points)
 * 3. Consistency (15 points)
 * 4. Technical Feasibility (15 points)
 * 5. Security Coverage (15 points)
 * 6. Testability (15 points)
 * Total: 100 points
 */
public record QualityScoreDto(
        int overallScore,
        int completeness,
        int clarity,
        int consistency,
        int technicalFeasibility,
        int securityCoverage,
        int testability,
        String summary,
        List<String> strengths,
        List<String> improvements
) {}
