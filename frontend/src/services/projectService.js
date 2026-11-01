import { apiClient } from './api';

export const projectService = {
  async getProjects() {
    return apiClient('/projects');
  },

  async getProject(id) {
    return apiClient(`/projects/${id}`);
  },

  async createProject(projectData) {
    return apiClient('/projects', {
      method: 'POST',
      body: JSON.stringify(projectData),
    });
  },

  async updateProject(id, projectData) {
    return apiClient(`/projects/${id}`, {
      method: 'PUT',
      body: JSON.stringify(projectData),
    });
  },

  async deleteProject(id) {
    return apiClient(`/projects/${id}`, {
      method: 'DELETE',
    });
  },

  async prepareProjectForAnalysis(id) {
    return apiClient(`/projects/${id}/prepare-analysis`, {
      method: 'POST',
    });
  },

  async getSpecification(id) {
    return apiClient(`/projects/${id}/specification`);
  },

  async updateSpecification(id, specification) {
    return apiClient(`/projects/${id}/specification`, {
      method: 'PUT',
      body: JSON.stringify({ specification }),
    });
  },

  async analyzeProject(id) {
    return apiClient(`/projects/${id}/analyze`, {
      method: 'POST',
    });
  },

  async getProjectRequirements(id) {
    return apiClient(`/projects/${id}/requirements`);
  },

  async submitClarifications(id, answers) {
    return apiClient(`/projects/${id}/clarifications`, {
      method: 'POST',
      body: JSON.stringify({ answers }),
    });
  },
};
