import { useState, useEffect, useCallback } from 'react';
import { getBackendHealth } from '../services/healthService';

/**
 * Custom React hook to query and track backend health status.
 */
export function useHealthCheck(pollIntervalMs = 30000) {
  const [data, setData] = useState(null);
  const [status, setStatus] = useState('checking'); // 'checking' | 'healthy' | 'offline'
  const [error, setError] = useState(null);
  const [lastChecked, setLastChecked] = useState(null);

  const check = useCallback(async () => {
    try {
      setStatus('checking');
      const response = await getBackendHealth();
      if (response && response.status === 'UP') {
        setData(response);
        setStatus('healthy');
        setError(null);
      } else {
        setStatus('offline');
        setError('Unexpected response format');
      }
    } catch (err) {
      setStatus('offline');
      setError(err.message || 'Backend unreachable');
    } finally {
      setLastChecked(new Date());
    }
  }, []);

  useEffect(() => {
    check();
    if (pollIntervalMs > 0) {
      const interval = setInterval(check, pollIntervalMs);
      return () => clearInterval(interval);
    }
  }, [check, pollIntervalMs]);

  return {
    data,
    status,
    isHealthy: status === 'healthy',
    isChecking: status === 'checking',
    error,
    lastChecked,
    refresh: check,
  };
}
