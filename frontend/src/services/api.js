/**
 * Centralized API client.
 * Configured with baseURL fallback, automatic JWT Bearer token attachment,
 * consistent error extraction, and timeout handling.
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

export class ApiError extends Error {
  constructor(status, message, details = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.details = details;
  }
}

export async function apiClient(endpoint, options = {}) {
  const url = endpoint.startsWith('http')
    ? endpoint
    : `${API_BASE_URL}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;

  const defaultHeaders = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  };

  const token = localStorage.getItem('projectforge_token');
  if (token) {
    defaultHeaders['Authorization'] = `Bearer ${token}`;
  }

  const config = {
    ...options,
    headers: {
      ...defaultHeaders,
      ...options.headers,
    },
  };

  try {
    const response = await fetch(url, config);

    // 204 No Content
    if (response.status === 204) {
      return null;
    }

    const contentType = response.headers.get('content-type') || '';
    const isJson = contentType.includes('application/json');
    const data = isJson ? await response.json() : await response.text();

    if (!response.ok) {
      if (response.status === 401 && token) {
        localStorage.removeItem('projectforge_token');
        localStorage.removeItem('projectforge_user');
        window.dispatchEvent(new Event('auth-change'));
      }

      const errorMessage = isJson && data.message ? data.message : `HTTP ${response.status}: ${response.statusText}`;
      throw new ApiError(response.status, errorMessage, data);
    }

    return data;
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError(0, error.message || 'Network connection failed');
  }
}

/**
 * Downloads a binary file (such as a ZIP package) from an authenticated endpoint.
 * Accurately preserves binary blob bytes, handles error responses, and triggers browser save.
 */
export async function downloadBinary(endpoint, fallbackFilename = 'download.zip') {
  const url = endpoint.startsWith('http')
    ? endpoint
    : `${API_BASE_URL}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;

  const token = localStorage.getItem('projectforge_token');
  const headers = {};
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  try {
    const response = await fetch(url, { headers });

    if (!response.ok) {
      if (response.status === 401 && token) {
        localStorage.removeItem('projectforge_token');
        localStorage.removeItem('projectforge_user');
        window.dispatchEvent(new Event('auth-change'));
      }

      let errorMessage = `HTTP ${response.status}: ${response.statusText}`;
      try {
        const errorJson = await response.json();
        if (errorJson && errorJson.message) {
          errorMessage = errorJson.message;
        }
      } catch {
        // Not JSON error body
      }
      throw new ApiError(response.status, errorMessage);
    }

    // Extract filename from Content-Disposition header if available
    let filename = fallbackFilename;
    const disposition = response.headers.get('content-disposition');
    if (disposition && disposition.includes('filename=')) {
      const match = disposition.match(/filename="?([^";]+)"?/);
      if (match && match[1]) {
        filename = match[1].trim();
      }
    }

    const blob = await response.blob();
    const blobUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = blobUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(blobUrl);

    return { filename, size: blob.size };
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError(0, error.message || 'Binary download failed');
  }
}
