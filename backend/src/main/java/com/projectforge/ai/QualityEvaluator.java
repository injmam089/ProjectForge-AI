package com.projectforge.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projectforge.dto.QualityScoreDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic quality evaluation engine assessing specification across the six
 * canonical dimensions:
 * 1. Completeness (25 points)
 * 2. Clarity (15 points)
 * 3. Consistency (15 points)
 * 4. Technical Feasibility (15 points)
 * 5. Security Coverage (15 points)
 * 6. Testability (15 points)
 * Total: 100 points
 */
@Component
public class QualityEvaluator {

    /**
     * Evaluates a Project Specification JSON across all six dimensions, stamps the
     * overall score into the JSON specification node, and returns a QualityScoreDto.
     *
     * @param spec the mutable specification JSON node
     * @return the evaluated QualityScoreDto
     */
    public QualityScoreDto evaluate(JsonNode spec) {
        int completeness = 0;
        int clarity = 0;
        int consistency = 0;
        int technicalFeasibility = 0;
        int securityCoverage = 0;
        int testability = 0;

        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();

        if (spec == null || !spec.isObject()) {
            return new QualityScoreDto(0, 0, 0, 0, 0, 0, 0, "Empty specification", strengths, improvements);
        }

        // 1. Completeness (up to 25 pts)
        JsonNode features = spec.path("features");
        if (features.isArray() && features.size() >= 3) {
            completeness += 8;
            strengths.add("Robust feature breakdown with multiple prioritized items");
        } else if (features.isArray() && !features.isEmpty()) {
            completeness += 4;
            improvements.add("Consider expanding feature scope beyond basic capabilities");
        }

        JsonNode modules = spec.path("modules");
        if (modules.isArray() && modules.size() >= 2) {
            completeness += 5;
            strengths.add("Well-separated modular domain architecture");
        } else if (modules.isArray() && !modules.isEmpty()) {
            completeness += 2;
        }

        JsonNode db = spec.path("database");
        JsonNode entities = db.isObject() ? db.path("entities") : db;
        if (entities.isArray() && entities.size() >= 2) {
            completeness += 6;
            strengths.add("Detailed relational database entity specifications");
        } else if (entities.isArray() && !entities.isEmpty()) {
            completeness += 3;
        }

        JsonNode apis = spec.path("apis");
        if (apis.isArray() && apis.size() >= 2) {
            completeness += 6;
            strengths.add("Clear REST API contract definitions");
        } else if (apis.isArray() && !apis.isEmpty()) {
            completeness += 3;
        }

        // 2. Clarity (up to 15 pts)
        JsonNode project = spec.path("project");
        if (project.isObject()) {
            if (project.path("goal").asText("").length() > 20) {
                clarity += 4;
            }
            if (project.path("problem").asText("").length() > 20) {
                clarity += 3;
            }
        }

        JsonNode users = spec.path("users");
        if (users.isArray() && !users.isEmpty()) {
            clarity += 8;
            strengths.add("Clear user persona definitions with explicit responsibilities and permissions");
        } else {
            improvements.add("Define user personas and access roles more explicitly");
        }

        // 3. Consistency (up to 15 pts)
        JsonNode tech = spec.path("technology");
        if (tech.isObject() && !tech.path("backend").isMissingNode() && apis.isArray() && !apis.isEmpty()) {
            consistency += 5;
            strengths.add("Consistent alignment between backend technology and API contract design");
        }
        if (entities.isArray() && !entities.isEmpty()) {
            boolean hasKeys = true;
            for (JsonNode e : entities) {
                if (e.path("primaryKey").asText("").isBlank()) {
                    hasKeys = false;
                    break;
                }
            }
            if (hasKeys) {
                consistency += 5;
            }
        }
        JsonNode arch = spec.path("architecture");
        if (arch.isObject() && !arch.path("style").asText("").isBlank()) {
            consistency += 5;
        }

        // 4. Technical Feasibility (up to 15 pts)
        if (tech.isObject()) {
            if (!tech.path("database").isMissingNode() && !tech.path("backend").isMissingNode()) {
                technicalFeasibility += 5;
            }
        }
        if (arch.isObject() && arch.path("components").isArray() && !arch.path("components").isEmpty()) {
            technicalFeasibility += 5;
        }
        JsonNode deployment = spec.path("deployment");
        if (deployment.isObject() && !deployment.path("environment").asText("").isBlank()) {
            technicalFeasibility += 5;
            strengths.add("Actionable deployment environments and prerequisite definitions");
        } else {
            improvements.add("Specify deployment target environments and infrastructure prerequisites");
        }

        // 5. Security Coverage (up to 15 pts)
        JsonNode sec = spec.path("security");
        if (sec.isObject()) {
            if (!sec.path("authentication").asText("").isBlank()) {
                securityCoverage += 5;
            }
            if (!sec.path("authorization").asText("").isBlank()) {
                securityCoverage += 5;
            }
            if (sec.path("securityRequirements").isArray() && !sec.path("securityRequirements").isEmpty()) {
                securityCoverage += 5;
                strengths.add("Domain-tailored security policies and requirements");
            } else {
                improvements.add("Enhance domain-specific security and compliance specifications");
            }
        }

        // 6. Testability (up to 15 pts)
        JsonNode testing = spec.path("testing");
        if (testing.isObject()) {
            if (testing.path("unitTesting").isArray() && !testing.path("unitTesting").isEmpty()) {
                testability += 5;
            }
            if (testing.path("integrationTesting").isArray() && !testing.path("integrationTesting").isEmpty()) {
                testability += 5;
            }
            if (testing.path("securityTesting").isArray() && !testing.path("securityTesting").isEmpty()) {
                testability += 5;
                strengths.add("Comprehensive testing strategy covering unit, integration, and security");
            } else {
                improvements.add("Define automated security and vulnerability testing strategy");
            }
        }

        // Deductions for unresolved critical or high questions
        int deductions = 0;
        JsonNode openQuestions = spec.path("openQuestions");
        if (openQuestions.isArray()) {
            for (JsonNode q : openQuestions) {
                String importance = q.path("importance").asText("MEDIUM").toUpperCase();
                if ("CRITICAL".equals(importance)) {
                    deductions += 5;
                } else if ("HIGH".equals(importance)) {
                    deductions += 3;
                }
            }
            if (openQuestions.isEmpty()) {
                strengths.add("All critical architectural questions and requirements have been resolved");
            } else {
                improvements.add("Answer open clarification questions to lock down ambiguous requirements");
            }
        }

        // Clamp dimensions to their maximum values
        completeness = Math.min(25, Math.max(0, completeness));
        clarity = Math.min(15, Math.max(0, clarity));
        consistency = Math.min(15, Math.max(0, consistency));
        technicalFeasibility = Math.min(15, Math.max(0, technicalFeasibility));
        securityCoverage = Math.min(15, Math.max(0, securityCoverage));
        testability = Math.min(15, Math.max(0, testability));

        int rawScore = completeness + clarity + consistency + technicalFeasibility + securityCoverage + testability - deductions;
        int overallScore = Math.max(10, Math.min(100, rawScore));

        String summary = String.format(
                "Specification quality evaluated at %d/100 (Completeness: %d/25, Clarity: %d/15, Consistency: %d/15, Feasibility: %d/15, Security: %d/15, Testability: %d/15).",
                overallScore, completeness, clarity, consistency, technicalFeasibility, securityCoverage, testability
        );

        // Stamp quality score into the JSON specification
        if (spec instanceof ObjectNode objNode) {
            objNode.put("qualityScore", overallScore);
            ObjectNode breakdown = objNode.putObject("qualityBreakdown");
            breakdown.put("completeness", completeness);
            breakdown.put("clarity", clarity);
            breakdown.put("consistency", consistency);
            breakdown.put("technicalFeasibility", technicalFeasibility);
            breakdown.put("securityCoverage", securityCoverage);
            breakdown.put("testability", testability);
        }

        return new QualityScoreDto(
                overallScore,
                completeness,
                clarity,
                consistency,
                technicalFeasibility,
                securityCoverage,
                testability,
                summary,
                strengths,
                improvements
        );
    }
}
