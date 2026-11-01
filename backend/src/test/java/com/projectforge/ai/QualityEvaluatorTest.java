package com.projectforge.ai;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projectforge.dto.QualityScoreDto;
import com.projectforge.testutil.TestSpecificationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QualityEvaluatorTest {

    private QualityEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new QualityEvaluator();
    }

    @Test
    @DisplayName("Evaluates complete specification across all six dimensions and stamps score into node")
    void evaluatesCompleteSpecification() {
        ObjectNode spec = (ObjectNode) TestSpecificationFactory.createValidSpecificationNode();

        QualityScoreDto result = evaluator.evaluate(spec);

        assertThat(result.overallScore()).isGreaterThanOrEqualTo(80);
        assertThat(result.completeness()).isGreaterThanOrEqualTo(20);
        assertThat(result.clarity()).isGreaterThanOrEqualTo(10);
        assertThat(result.consistency()).isGreaterThanOrEqualTo(10);
        assertThat(result.technicalFeasibility()).isGreaterThanOrEqualTo(10);
        assertThat(result.securityCoverage()).isGreaterThanOrEqualTo(10);
        assertThat(result.testability()).isGreaterThanOrEqualTo(10);
        assertThat(result.summary()).contains("Specification quality evaluated at");
        assertThat(result.strengths()).isNotEmpty();

        // Check that qualityScore and qualityBreakdown are stamped into the JSON node
        assertThat(spec.path("qualityScore").asInt()).isEqualTo(result.overallScore());
        assertThat(spec.path("qualityBreakdown").path("completeness").asInt()).isEqualTo(result.completeness());
        assertThat(spec.path("qualityBreakdown").path("testability").asInt()).isEqualTo(result.testability());
    }

    @Test
    @DisplayName("Deducts quality score when critical or high open questions are unresolved")
    void deductsScoreForCriticalQuestions() {
        ObjectNode spec = (ObjectNode) TestSpecificationFactory.createValidSpecificationNode();
        ArrayNode questions = spec.putArray("openQuestions");
        ObjectNode q1 = questions.addObject();
        q1.put("id", "Q1");
        q1.put("question", "What is the core data retention policy?");
        q1.put("importance", "CRITICAL");
        q1.put("reason", "Mandated by GDPR compliance");

        ObjectNode q2 = questions.addObject();
        q2.put("id", "Q2");
        q2.put("question", "What external payment gateway will be used?");
        q2.put("importance", "CRITICAL");

        QualityScoreDto resultWithCritical = evaluator.evaluate(spec);

        // Clear questions
        spec.putArray("openQuestions");
        QualityScoreDto resultWithoutQuestions = evaluator.evaluate(spec);

        assertThat(resultWithoutQuestions.overallScore()).isGreaterThan(resultWithCritical.overallScore());
    }
}
