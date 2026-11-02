import { apiClient, downloadBinary } from './api';

export const exportService = {
  /**
   * Prepares export metadata (verifies documents, calculates file count, returns projected filename).
   */
  async prepareExport(projectId) {
    return apiClient(`/projects/${projectId}/export`, {
      method: 'POST',
    });
  },

  /**
   * Downloads the finalized project Markdown documentation package as a secure binary ZIP.
   */
  async downloadExport(projectId, fallbackFilename = 'project_documentation.zip') {
    return downloadBinary(`/projects/${projectId}/export/download`, fallbackFilename);
  },
};
