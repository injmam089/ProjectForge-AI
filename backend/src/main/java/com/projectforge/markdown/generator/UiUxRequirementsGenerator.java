package com.projectforge.markdown.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import com.projectforge.markdown.DocumentContentGenerator;
import com.projectforge.markdown.GeneratorUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Generates UI_UX_REQUIREMENTS.md from the approved project specification.
 */
@Component
public class UiUxRequirementsGenerator implements DocumentContentGenerator {

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.UI_UX_REQUIREMENTS;
    }

    @Override
    public String generateContent(Project project, JsonNode spec) {
        String projectName = GeneratorUtils.getText(spec.path("project"), "name", project.getName());
        List<String> frontendTech = GeneratorUtils.getStringList(spec.path("technology"), "frontend");

        StringBuilder sb = new StringBuilder();
        sb.append("# UI/UX Requirements: ").append(projectName).append("\n\n");

        sb.append("## 1. Design System & Frontend Baseline\n\n");
        sb.append("- **Frontend Stack**: `").append(GeneratorUtils.joinOrNone(frontendTech)).append("`\n");
        sb.append("- **Visual Language**: Clean, high-contrast, modern interface with consistent spacing, typography, and dark/light mode compatibility.\n\n");

        sb.append("## 2. Navigation & User Journeys\n\n");
        sb.append("- **Intuitive Layout**: Header navigation bar with user profile, breadcrumbs on detail views, and clear calls to action.\n");
        sb.append("- **State Feedback**: Active navigation tabs and explicit visual state indicators across all workflows.\n\n");

        sb.append("## 3. UI States Specification\n\n");
        sb.append("### Loading States\n");
        sb.append("- Asynchronous actions (API calls, data queries) must display localized spinners or skeleton loading placeholders.\n");
        sb.append("- Submit buttons must disable during network transit and display loading text.\n\n");

        sb.append("### Empty States\n");
        sb.append("- When no data is available (empty lists, uninitialized configurations), show helpful contextual empty states with actionable guidance.\n\n");

        sb.append("### Error States\n");
        sb.append("- Form fields with invalid input must highlight with clear inline error messages.\n");
        sb.append("- Global network or server errors must display dismissible error alerts without disrupting completed input.\n\n");

        sb.append("## 4. Accessibility & Responsiveness\n\n");
        sb.append("- **WCAG 2.1 AA Compliance**: Accessible contrast ratios, keyboard navigation support, and ARIA attributes on interactive modals.\n");
        sb.append("- **Responsive Design**: Fluid grid layouts providing full usability across desktop (1440px+), tablet (768px-1024px), and mobile (320px-480px).\n");

        return sb.toString();
    }
}
