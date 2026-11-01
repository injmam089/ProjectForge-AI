package com.projectforge.markdown.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import com.projectforge.markdown.DocumentContentGenerator;
import com.projectforge.markdown.GeneratorUtils;
import org.springframework.stereotype.Component;

/**
 * Generates PROJECT_GOALS.md from the approved project specification.
 */
@Component
public class ProjectGoalsGenerator implements DocumentContentGenerator {

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.PROJECT_GOALS;
    }

    @Override
    public String generateContent(Project project, JsonNode spec) {
        String projectName = GeneratorUtils.getText(spec.path("project"), "name", project.getName());
        String goal = GeneratorUtils.getText(spec.path("project"), "goal", project.getDescription());

        StringBuilder sb = new StringBuilder();
        sb.append("# Strategic Goals & Success Criteria: ").append(projectName).append("\n\n");

        sb.append("## 1. Primary Goal\n\n");
        sb.append(goal).append("\n\n");

        sb.append("## 2. Key Objectives\n\n");
        sb.append("1. **Deliver Core Value**: Implement all required functional capabilities defined in the approved specification.\n");
        sb.append("2. **Architectural Fidelity**: Preserve modular boundaries and approved technology choices without unauthorized deviations.\n");
        sb.append("3. **Operational Stability**: Ensure reliability, low-latency performance, and structured error handling across all subsystems.\n");
        sb.append("4. **Security Integrity**: Enforce secure-by-default access controls, validation, and token authentication.\n\n");

        sb.append("## 3. Expected Outcomes\n\n");
        sb.append("- Fully functional end-to-end software platform serving designated user roles.\n");
        sb.append("- Robust API contracts and data models supporting future scalability.\n");
        sb.append("- High test coverage verifying critical business workflows.\n\n");

        sb.append("## 4. Success Criteria\n\n");
        sb.append("| Criteria | Metric / Target | Verification Method |\n");
        sb.append("| :--- | :--- | :--- |\n");
        sb.append("| Feature Completeness | 100% of approved critical/high features | Automated Acceptance Tests |\n");
        sb.append("| Code Quality | 0 static analysis critical issues | Linter & SonarQube verification |\n");
        sb.append("| Test Coverage | >= 80% line coverage on domain services | JUnit & JaCoCo reports |\n");
        sb.append("| API Reliability | 99.9% successful test run execution | Integration test harness |\n\n");

        sb.append("## 5. Future Goals & Roadmap\n\n");
        sb.append("- Ongoing enhancements and secondary features deferred to post-MVP iterations.\n");
        sb.append("- Continuous performance tuning based on telemetry and production metrics.\n");

        return sb.toString();
    }
}
