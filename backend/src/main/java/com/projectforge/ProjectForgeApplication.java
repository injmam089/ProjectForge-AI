package com.projectforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ProjectForge AI - Main Spring Boot Application Entry Point.
 *
 * Provides the bootstrap configuration for the modular monolith
 * requirements engineering and documentation generation backend.
 */
@SpringBootApplication
public class ProjectForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectForgeApplication.class, args);
    }
}
