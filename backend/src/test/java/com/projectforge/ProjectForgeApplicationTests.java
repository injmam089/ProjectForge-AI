package com.projectforge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProjectForgeApplicationTests {

    @Test
    @DisplayName("ProjectForgeApplication class should load correctly")
    void mainClassLoads() {
        assertDoesNotThrow(() -> {
            Class<?> clazz = Class.forName("com.projectforge.ProjectForgeApplication");
            assertNotNull(clazz);
        });
    }
}
