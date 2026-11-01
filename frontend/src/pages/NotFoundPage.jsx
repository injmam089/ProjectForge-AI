import React from 'react';
import { Link } from 'react-router-dom';
import { AlertTriangle, ArrowLeft } from 'lucide-react';

export function NotFoundPage() {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '80px 20px',
        textAlign: 'center',
      }}
    >
      <div
        style={{
          width: '56px',
          height: '56px',
          borderRadius: 'var(--radius-lg)',
          background: 'var(--accent-amber-light)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: 'var(--accent-amber)',
          marginBottom: '20px',
        }}
      >
        <AlertTriangle size={28} />
      </div>

      <h1 style={{ fontSize: '2rem', fontWeight: 700, marginBottom: '8px' }}>
        404 — Page Not Found
      </h1>

      <p style={{ color: 'var(--text-secondary)', maxWidth: '420px', marginBottom: '24px' }}>
        The page you are looking for does not exist in the ProjectForge AI workspace.
      </p>

      <Link to="/" className="btn btn-primary">
        <ArrowLeft size={16} />
        <span>Return to Dashboard</span>
      </Link>
    </div>
  );
}
