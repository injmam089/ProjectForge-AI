package com.projectforge.entity;

/**
 * Type of modification that produced a DocumentVersion.
 */
public enum DocumentChangeType {
    GENERATE,
    MANUAL_EDIT,
    AI_EDIT,
    REGENERATE,
    RESTORE
}
