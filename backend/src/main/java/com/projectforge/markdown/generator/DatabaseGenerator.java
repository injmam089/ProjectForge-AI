package com.projectforge.markdown.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.projectforge.entity.DocumentType;
import com.projectforge.entity.Project;
import com.projectforge.markdown.DocumentContentGenerator;
import com.projectforge.markdown.GeneratorUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Generates DATABASE.md from the approved project specification.
 */
@Component
public class DatabaseGenerator implements DocumentContentGenerator {

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.DATABASE;
    }

    @Override
    public String generateContent(Project project, JsonNode spec) {
        String projectName = GeneratorUtils.getText(spec.path("project"), "name", project.getName());
        List<String> dbTech = GeneratorUtils.getStringList(spec.path("technology"), "database");

        StringBuilder sb = new StringBuilder();
        sb.append("# Database Design & Schema: ").append(projectName).append("\n\n");

        sb.append("## 1. Database Engine\n\n");
        sb.append("- **Approved Storage Technology**: `").append(GeneratorUtils.joinOrNone(dbTech)).append("`\n\n");

        JsonNode entitiesNode = spec.path("database").path("entities");
        if (entitiesNode.isArray() && entitiesNode.size() > 0) {
            sb.append("## 2. Entity Schemas & Tables\n\n");
            for (JsonNode e : entitiesNode) {
                String name = GeneratorUtils.getText(e, "name", "entity");
                String pk = GeneratorUtils.getText(e, "primaryKey", "id");
                List<String> fields = GeneratorUtils.getStringList(e, "fields");
                List<String> fks = GeneratorUtils.getStringList(e, "foreignKeys");
                List<String> constraints = GeneratorUtils.getStringList(e, "constraints");

                sb.append("### Table: `").append(name).append("`\n\n");
                sb.append("- **Primary Key**: `").append(pk).append("`\n");
                if (!fks.isEmpty()) {
                    sb.append("- **Foreign Keys**: `").append(String.join("`, `", fks)).append("`\n");
                }
                if (!constraints.isEmpty()) {
                    sb.append("- **Constraints**: ").append(String.join("; ", constraints)).append("\n");
                }
                sb.append("\n**Columns / Fields**:\n\n");
                sb.append("| Column Specification |\n");
                sb.append("| :--- |\n");
                for (String field : fields) {
                    sb.append("| `").append(field).append("` |\n");
                }
                sb.append("\n");
            }
        } else {
            sb.append("## 2. Entities\n\n");
            sb.append("_No explicit relational entities defined in approved specification._\n\n");
        }

        List<String> rels = GeneratorUtils.getStringList(spec.path("database"), "relationships");
        if (!rels.isEmpty()) {
            sb.append("## 3. Relationships & Cardinality\n\n");
            for (String rel : rels) {
                sb.append("- ").append(rel).append("\n");
            }
            sb.append("\n");
        }

        sb.append("## 4. Data Ownership & Lifecycle\n\n");
        sb.append("- **User Isolation**: All domain records are partitioned by owning user identity.\n");
        sb.append("- **Audit Timestamps**: Entities maintain `created_at` and `updated_at` timestamps.\n");
        sb.append("- **Referential Integrity**: Cascading deletes applied where parent-child entity ownership exists.\n");

        return sb.toString();
    }
}
