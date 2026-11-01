package com.projectforge.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectforge.exception.AIValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates the structural integrity and business rule completeness of AI-generated
 * Project Specification JSON documents before persistence.
 */
@Component
public class RequirementValidator {

    private static final Logger log = LoggerFactory.getLogger(RequirementValidator.class);

    /**
     * Validates that the provided JsonNode contains all required sections, non-empty arrays,
     * and valid metadata required to serve as the single source of truth for future phases.
     *
     * @param spec the parsed JSON node representing the project specification
     * @throws AIValidationException if validation rules are violated
     */
    public void validate(JsonNode spec) {
        if (spec == null || !spec.isObject()) {
            throw new AIValidationException("Invalid specification: root payload must be a JSON object.");
        }

        List<String> errors = new ArrayList<>();

        // 1. Project identity
        JsonNode projectNode = spec.path("project");
        if (!projectNode.isObject()) {
            errors.add("Missing required 'project' section in specification.");
        } else {
            if (projectNode.path("name").asText("").trim().isEmpty()) {
                errors.add("Project identity 'name' must not be blank.");
            }
            if (projectNode.path("goal").asText("").trim().isEmpty()) {
                errors.add("Project identity 'goal' must not be blank.");
            }
            if (projectNode.path("problem").asText("").trim().isEmpty()) {
                errors.add("Project identity 'problem' statement must not be blank.");
            }
        }

        // 2. Features
        JsonNode featuresNode = spec.path("features");
        if (!featuresNode.isArray() || featuresNode.isEmpty()) {
            errors.add("Specification must contain at least one feature in the 'features' array.");
        }

        // 3. Modules
        JsonNode modulesNode = spec.path("modules");
        if (!modulesNode.isArray() || modulesNode.isEmpty()) {
            errors.add("Specification must contain at least one module in the 'modules' array.");
        }

        // 4. Technology
        JsonNode techNode = spec.path("technology");
        if (!techNode.isObject()) {
            errors.add("Missing required 'technology' section in specification.");
        }

        // 5. Database
        JsonNode dbNode = spec.path("database");
        if (!dbNode.isObject() && !dbNode.isArray()) {
            errors.add("Missing required 'database' section in specification.");
        }

        // 6. APIs
        JsonNode apisNode = spec.path("apis");
        if (!apisNode.isArray()) {
            errors.add("Missing required 'apis' array in specification.");
        }

        // 7. Security
        JsonNode securityNode = spec.path("security");
        if (!securityNode.isObject()) {
            errors.add("Missing required 'security' section in specification.");
        }

        // 8. Testing
        JsonNode testingNode = spec.path("testing");
        if (!testingNode.isObject()) {
            errors.add("Missing required 'testing' section in specification.");
        }

        if (!errors.isEmpty()) {
            String combinedError = "Requirement specification validation failed: " + String.join("; ", errors);
            log.warn(combinedError);
            throw new AIValidationException(combinedError);
        }

        log.debug("Requirement specification passed all structural validation checks.");
    }
}
