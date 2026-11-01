import { apiClient } from './api';

export const documentService = {
  /**
   * Fetches the workspace summary and all documents for a project.
   */
  async getWorkspace(projectId) {
    return apiClient(`/projects/${projectId}/workspace`);
  },

  /**
   * Fetches a single document by ID.
   */
  async getDocument(documentId) {
    return apiClient(`/documents/${documentId}`);
  },

  /**
   * Saves manual edits to a document, producing a new MANUAL_EDIT version.
   */
  async saveDocument(documentId, content) {
    return apiClient(`/documents/${documentId}`, {
      method: 'PUT',
      body: JSON.stringify({ content }),
    });
  },

  /**
   * Fetches version history summaries (metadata only) for a document.
   */
  async getVersions(documentId) {
    return apiClient(`/documents/${documentId}/versions`);
  },

  /**
   * Fetches a specific historical version snapshot with full content.
   */
  async getVersion(documentId, version) {
    return apiClient(`/documents/${documentId}/versions/${version}`);
  },

  /**
   * Restores a historical version as a new RESTORE version snapshot.
   */
  async restoreVersion(documentId, version) {
    return apiClient(`/documents/${documentId}/restore/${version}`, {
      method: 'POST',
    });
  },

  /**
   * Deterministically regenerates a single document from the approved project specification.
   */
  async regenerateDocument(documentId) {
    return apiClient(`/documents/${documentId}/regenerate`, {
      method: 'POST',
    });
  },

  /**
   * Applies an AI editing action (IMPROVE, EXPAND, SIMPLIFY, CUSTOM) to a document.
   */
  async aiEditDocument(documentId, action, instruction = null) {
    const payload = { action };
    if (instruction && instruction.trim().length > 0) {
      payload.instruction = instruction.trim();
    }
    return apiClient(`/documents/${documentId}/ai-edit`, {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },
};
