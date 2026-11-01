package com.projectforge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * Project JPA entity representing a software project created by an authenticated user.
 *
 * Implements strict ownership:
 * - Belongs to exactly one User (@ManyToOne)
 * - Indexed by user_id and composite (user_id, status) for high-efficiency ownership lookup
 * - Stores structured requirements intake data preparing the project for AI analysis
 */
@Entity
@Table(name = "projects", indexes = {
        @Index(name = "idx_projects_user_id", columnList = "user_id"),
        @Index(name = "idx_projects_user_id_status", columnList = "user_id, status")
})
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "problem_statement", columnDefinition = "TEXT")
    private String problemStatement;

    @Column(name = "target_users", length = 2000)
    private String targetUsers;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_type", nullable = false, length = 50)
    private ProjectType projectType = ProjectType.WEB_APPLICATION;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectDifficulty difficulty = ProjectDifficulty.INTERMEDIATE;

    @Column(name = "preferred_tech_stack", length = 2000)
    private String preferredTechStack;

    @Column(name = "special_requirements", columnDefinition = "TEXT")
    private String specialRequirements;

    @Column(name = "project_specification", columnDefinition = "TEXT")
    private String projectSpecification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ProjectStatus status = ProjectStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Project() {
    }

    public Project(
            User user,
            String name,
            String description,
            String problemStatement,
            String targetUsers,
            ProjectType projectType,
            ProjectDifficulty difficulty,
            String preferredTechStack,
            String specialRequirements
    ) {
        this.user = user;
        this.name = name;
        this.description = description;
        this.problemStatement = problemStatement;
        this.targetUsers = targetUsers;
        this.projectType = projectType != null ? projectType : ProjectType.WEB_APPLICATION;
        this.difficulty = difficulty != null ? difficulty : ProjectDifficulty.INTERMEDIATE;
        this.preferredTechStack = preferredTechStack;
        this.specialRequirements = specialRequirements;
        this.status = ProjectStatus.DRAFT;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.status == null) {
            this.status = ProjectStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Alias for description
    public String getProjectIdea() {
        return description;
    }

    public void setProjectIdea(String projectIdea) {
        this.description = projectIdea;
    }

    public String getProblemStatement() {
        return problemStatement;
    }

    public void setProblemStatement(String problemStatement) {
        this.problemStatement = problemStatement;
    }

    public String getTargetUsers() {
        return targetUsers;
    }

    public void setTargetUsers(String targetUsers) {
        this.targetUsers = targetUsers;
    }

    public ProjectType getProjectType() {
        return projectType;
    }

    public void setProjectType(ProjectType projectType) {
        this.projectType = projectType;
    }

    public ProjectDifficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(ProjectDifficulty difficulty) {
        this.difficulty = difficulty;
    }

    public String getPreferredTechStack() {
        return preferredTechStack;
    }

    public void setPreferredTechStack(String preferredTechStack) {
        this.preferredTechStack = preferredTechStack;
    }

    public String getSpecialRequirements() {
        return specialRequirements;
    }

    public void setSpecialRequirements(String specialRequirements) {
        this.specialRequirements = specialRequirements;
    }

    public String getProjectSpecification() {
        return projectSpecification;
    }

    public void setProjectSpecification(String projectSpecification) {
        this.projectSpecification = projectSpecification;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return Objects.equals(id, project.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Project{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", projectType=" + projectType +
                ", difficulty=" + difficulty +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
