import React from 'react';
import { Activity, RefreshCw, AlertCircle, CheckCircle2 } from 'lucide-react';
import { useHealthCheck } from '../hooks/useHealthCheck';

export function SystemStatusBadge() {
  const { status, data, isChecking, refresh } = useHealthCheck(30000);

  return (
    <div
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '8px',
        padding: '6px 12px',
        borderRadius: 'var(--radius-full)',
        background: 'var(--bg-secondary)',
        border: '1px solid var(--border-color)',
        fontSize: '0.75rem',
        color: 'var(--text-secondary)',
      }}
      title={data ? `Service: ${data.service}` : 'Checking backend status...'}
    >
      {isChecking ? (
        <RefreshCw size={13} className="animate-spin" style={{ color: 'var(--primary)' }} />
      ) : status === 'healthy' ? (
        <span
          style={{
            position: 'relative',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <span
            className="animate-pulse-glow"
            style={{
              position: 'absolute',
              width: '8px',
              height: '8px',
              borderRadius: '50%',
              background: 'var(--accent-emerald)',
            }}
          />
          <CheckCircle2 size={13} style={{ color: 'var(--accent-emerald)', position: 'relative' }} />
        </span>
      ) : (
        <AlertCircle size={13} style={{ color: 'var(--accent-amber)' }} />
      )}

      <span>
        API:{' '}
        <strong style={{ color: status === 'healthy' ? '#34d399' : 'var(--text-secondary)' }}>
          {status === 'healthy' ? 'UP' : isChecking ? 'CONNECTING...' : 'STANDBY'}
        </strong>
      </span>

      <button
        onClick={refresh}
        disabled={isChecking}
        title="Refresh health status"
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          color: 'var(--text-muted)',
          padding: '2px',
          marginLeft: '2px',
        }}
      >
        <RefreshCw size={11} className={isChecking ? 'animate-spin' : ''} />
      </button>
    </div>
  );
}
