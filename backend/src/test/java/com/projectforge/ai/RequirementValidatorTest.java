package com.projectforge.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projectforge.exception.AIValidationException;
import com.projectforge.testutil.TestSpecificationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequirementValidatorTest {

    private RequirementValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        validator = new RequirementValidator();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Valid specification passes structural validation")
    void validSpecificationPasses() {
        JsonNode validSpec = TestSpecificationFactory.createValidSpecificationNode();
        assertThatCode(() -> validator.validate(validSpec)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Throws AIValidationException when root is not a JSON object")
    void nonObjectRootThrows() {
        JsonNode arrayNode = objectMapper.createArrayNode();
        assertThatThrownBy(() -> validator.validate(arrayNode))
                .isInstanceOf(AIValidationException.class)
                .hasMessageContaining("root payload must be a JSON object");
    }

    @Test
    @DisplayName("Throws AIValidationException when required project section is missing")
    void missingProjectSectionThrows() {
        ObjectNode spec = (ObjectNode) TestSpecificationFactory.createValidSpecificationNode();
        spec.remove("project");

        assertThatThrownBy(() -> validator.validate(spec))
                .isInstanceOf(AIValidationException.class)
                .hasMessageContaining("Missing required 'project' section");
    }

    @Test
    @DisplayName("Throws AIValidationException when features array is empty")
    void emptyFeaturesArrayThrows() {
        ObjectNode spec = (ObjectNode) TestSpecificationFactory.createValidSpecificationNode();
        spec.putArray("features");

        assertThatThrownBy(() -> validator.validate(spec))
                .isInstanceOf(AIValidationException.class)
                .hasMessageContaining("features");
    }

    @Test
    @DisplayName("Throws AIValidationException when modules array is empty")
    void emptyModulesArrayThrows() {
        ObjectNode spec = (ObjectNode) TestSpecificationFactory.createValidSpecificationNode();
        spec.putArray("modules");

        assertThatThrownBy(() -> validator.validate(spec))
                .isInstanceOf(AIValidationException.class)
                .hasMessageContaining("modules");
    }

    @Test
    @DisplayName("Throws AIValidationException when technology section is missing")
    void missingTechnologySectionThrows() {
        ObjectNode spec = (ObjectNode) TestSpecificationFactory.createValidSpecificationNode();
        spec.remove("technology");

        assertThatThrownBy(() -> validator.validate(spec))
                .isInstanceOf(AIValidationException.class)
                .hasMessageContaining("Missing required 'technology' section");
    }
}
