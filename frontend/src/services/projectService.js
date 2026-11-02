import { apiClient } from './api';

export const projectService = {
  async getProjects(params = {}) {
    const query = new URLSearchParams();
    if (params.page !== undefined && params.page !== null) query.append('page', params.page);
    if (params.size !== undefined && params.size !== null) query.append('size', params.size);
    if (params.search) query.append('search', params.search.trim());
    if (params.status && params.status !== 'ALL') query.append('status', params.status);
    if (params.type && params.type !== 'ALL') query.append('type', params.type);
    if (params.difficulty && params.difficulty !== 'ALL') query.append('difficulty', params.difficulty);
    if (params.sort) query.append('sort', params.sort);
    if (params.direction) query.append('direction', params.direction);

    const queryString = query.toString();
    const endpoint = queryString ? `/projects?${queryString}` : '/projects';
    return apiClient(endpoint);
  },

  async getProjectStats() {
    return apiClient('/projects/stats');
  },

  async getProjectHistory(id, params = {}) {
    const query = new URLSearchParams();
    if (params.page !== undefined && params.page !== null) query.append('page', params.page);
    if (params.size !== undefined && params.size !== null) query.append('size', params.size);

    const queryString = query.toString();
    const endpoint = queryString ? `/projects/${id}/history?${queryString}` : `/projects/${id}/history`;
    return apiClient(endpoint);
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

  async getReview(id) {
    return apiClient(`/projects/${id}/review`);
  },

  async updateReview(id, specification) {
    return apiClient(`/projects/${id}/review`, {
      method: 'PUT',
      body: JSON.stringify({ specification }),
    });
  },

  async approveProject(id, confirmation = true) {
    return apiClient(`/projects/${id}/approve`, {
      method: 'POST',
      body: JSON.stringify({ confirmation }),
    });
  },

  async generateDocuments(id) {
    return apiClient(`/projects/${id}/generate`, {
      method: 'POST',
    });
  },

  async getDocuments(id) {
    return apiClient(`/projects/${id}/documents`);
  },

  async getDocument(id) {
    return apiClient(`/documents/${id}`);
  },
};
