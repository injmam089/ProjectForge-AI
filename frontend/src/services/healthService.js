import { apiClient } from './api';

/**
 * Health check API service.
 * Hits backend GET /api/health to verify service status.
 */
export async function getBackendHealth() {
  return apiClient('/health');
}
