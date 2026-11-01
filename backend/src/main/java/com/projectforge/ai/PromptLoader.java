package com.projectforge.ai;

import com.projectforge.exception.AIServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility component that loads externalized AI prompt templates from classpath resources.
 * Prompts are cached in memory for high-throughput performance.
 */
@Component
public class PromptLoader {

    private static final Logger log = LoggerFactory.getLogger(PromptLoader.class);
    private final Map<String, String> promptCache = new ConcurrentHashMap<>();

    /**
     * Loads a prompt template by relative resource name under the "prompts/" directory.
     *
     * @param promptName the filename (e.g. "requirement-analyzer.txt")
     * @return the prompt text content
     */
    public String loadPrompt(String promptName) {
        return promptCache.computeIfAbsent(promptName, name -> {
            String path = "prompts/" + name;
            log.debug("Loading AI prompt from classpath: {}", path);
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                throw new AIServiceException("Required AI prompt file not found on classpath: " + path);
            }
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
            } catch (IOException e) {
                throw new AIServiceException("Failed to read AI prompt file: " + path, e);
            }
        });
    }
}
