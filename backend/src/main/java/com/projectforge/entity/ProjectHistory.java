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
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity recording tamper-evident, meaningful lifecycle events for a project.
 * Guaranteed to never store credentials, passwords, or raw secrets.
 */
@Entity
@Table(name = "project_history", indexes = {
        @Index(name = "idx_project_history_project_id_created_at", columnList = "project_id, created_at"),
        @Index(name = "idx_project_history_project_id", columnList = "project_id")
})
public class ProjectHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private ProjectHistoryEventType eventType;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "metadata", length = 4000)
    private String metadata;

    @Column(name = "created_by", nullable = false, length = 150)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ProjectHistory() {
    }

    public ProjectHistory(Project project, ProjectHistoryEventType eventType, String description, String metadata, String createdBy) {
        this(project, eventType, description, metadata, createdBy, Instant.now());
    }

    public ProjectHistory(Project project, ProjectHistoryEventType eventType, String description, String metadata, String createdBy, Instant createdAt) {
        this.project = project;
        this.eventType = eventType;
        this.description = description;
        this.metadata = metadata;
        this.createdBy = createdBy != null ? createdBy : "system";
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public ProjectHistoryEventType getEventType() {
        return eventType;
    }

    public void setEventType(ProjectHistoryEventType eventType) {
        this.eventType = eventType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectHistory that = (ProjectHistory) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
