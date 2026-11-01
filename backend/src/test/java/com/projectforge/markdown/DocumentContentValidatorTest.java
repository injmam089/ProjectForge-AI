package com.projectforge.markdown;

import com.projectforge.exception.MarkdownValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentContentValidatorTest {

    private DocumentContentValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DocumentContentValidator();
    }

    @Test
    @DisplayName("Valid Markdown with heading passes validation")
    void validMarkdownPasses() {
        String content = "# Valid Heading\n\nThis is valid content with more than twenty characters.";
        assertThatCode(() -> validator.validateContent("README.md", content))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Null or blank content throws MarkdownValidationException")
    void blankContentThrows() {
        assertThatThrownBy(() -> validator.validateContent("README.md", null))
                .isInstanceOf(MarkdownValidationException.class)
                .hasMessageContaining("must not be blank");

        assertThatThrownBy(() -> validator.validateContent("README.md", "   \n\t  "))
                .isInstanceOf(MarkdownValidationException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    @DisplayName("Content under 20 characters throws MarkdownValidationException")
    void tooShortThrows() {
        assertThatThrownBy(() -> validator.validateContent("README.md", "# Short"))
                .isInstanceOf(MarkdownValidationException.class)
                .hasMessageContaining("too short");
    }

    @Test
    @DisplayName("Content exceeding 500,000 characters throws MarkdownValidationException")
    void tooLongThrows() {
        String longContent = "# Heading\n" + "x".repeat(500_001);
        assertThatThrownBy(() -> validator.validateContent("README.md", longContent))
                .isInstanceOf(MarkdownValidationException.class)
                .hasMessageContaining("exceeds maximum limit");
    }

    @Test
    @DisplayName("Content without Markdown heading throws MarkdownValidationException")
    void missingHeadingThrows() {
        String content = "This content has no markdown heading at all and is more than twenty characters.";
        assertThatThrownBy(() -> validator.validateContent("README.md", content))
                .isInstanceOf(MarkdownValidationException.class)
                .hasMessageContaining("must contain at least one valid Markdown heading");
    }

    @Test
    @DisplayName("Dangerous HTML tags like <script> throw MarkdownValidationException")
    void scriptTagThrows() {
        String content = "# Title\n\n<script>alert('xss')</script>\nValid markdown text follows.";
        assertThatThrownBy(() -> validator.validateContent("README.md", content))
                .isInstanceOf(MarkdownValidationException.class)
                .hasMessageContaining("contains forbidden HTML");
    }

    @Test
    @DisplayName("Dangerous HTML event handlers like onerror throw MarkdownValidationException")
    void eventHandlerThrows() {
        String content = "# Title\n\n<img src='x' onerror='alert(1)'>\nValid markdown text follows.";
        assertThatThrownBy(() -> validator.validateContent("README.md", content))
                .isInstanceOf(MarkdownValidationException.class)
                .hasMessageContaining("contains forbidden HTML");
    }
}
