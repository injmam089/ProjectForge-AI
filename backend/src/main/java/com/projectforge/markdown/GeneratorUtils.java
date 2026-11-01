package com.projectforge.markdown;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility functions for deterministic Markdown document generation.
 */
public final class GeneratorUtils {

    private GeneratorUtils() {
    }

    public static String getText(JsonNode node, String field, String defaultValue) {
        if (node == null || !node.has(field) || node.path(field).isNull()) {
            return defaultValue;
        }
        String text = node.path(field).asText();
        return text != null && !text.isBlank() ? text : defaultValue;
    }

    public static List<String> getStringList(JsonNode node, String arrayField) {
        List<String> result = new ArrayList<>();
        if (node == null || !node.has(arrayField)) {
            return result;
        }
        JsonNode arr = node.path(arrayField);
        if (arr.isArray()) {
            for (JsonNode item : arr) {
                if (item.isTextual() && !item.asText().isBlank()) {
                    result.add(item.asText());
                }
            }
        }
        return result;
    }

    public static List<String> getTechCategory(JsonNode spec, String category) {
        return getStringList(spec.path("technology"), category);
    }

    public static String joinOrNone(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "None specified in approved specification";
        }
        return String.join(", ", list);
    }

    public static String toBulletList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "- None specified in approved specification\n";
        }
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            sb.append("- ").append(item).append("\n");
        }
        return sb.toString();
    }
}
