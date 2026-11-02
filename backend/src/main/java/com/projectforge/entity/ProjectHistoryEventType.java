package com.projectforge.entity;

/**
 * Controlled event types for the project history and audit activity log.
 */
public enum ProjectHistoryEventType {
    PROJECT_CREATED,
    ANALYSIS_STARTED,
    REQUIREMENTS_READY,
    REVIEW_UPDATED,
    PROJECT_APPROVED,
    DOCUMENTS_GENERATED,
    DOCUMENT_EDITED,
    DOCUMENT_AI_EDITED,
    DOCUMENT_RESTORED,
    DOCUMENT_REGENERATED,
    PROJECT_EXPORTED
}
