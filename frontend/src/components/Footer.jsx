import React from 'react';
import { Layers, ShieldCheck, Database, Cpu } from 'lucide-react';
import { APP_CONFIG } from '../utils/constants';

export function Footer() {
  return (
    <footer
      style={{
        marginTop: 'auto',
        borderTop: '1px solid var(--border-color)',
        background: 'var(--bg-secondary)',
        padding: '32px 0 24px',
        color: 'var(--text-muted)',
        fontSize: '0.8125rem',
      }}
    >
      <div className="container">
        <div
          style={{
            display: 'flex',
            flexWrap: 'wrap',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '20px',
            marginBottom: '20px',
          }}
        >
          <div>
            <div style={{ fontWeight: 600, color: 'var(--text-primary)', marginBottom: '4px' }}>
              {APP_CONFIG.NAME}
            </div>
            <div>{APP_CONFIG.TAGLINE}</div>
          </div>

          {/* Architecture Badges */}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
            <span
              className="badge"
              style={{ background: 'var(--bg-tertiary)', color: 'var(--text-secondary)' }}
            >
              <Cpu size={12} style={{ color: 'var(--primary)' }} />
              <span>Spring Boot 3.3.5 (Java 17+)</span>
            </span>
            <span
              className="badge"
              style={{ background: 'var(--bg-tertiary)', color: 'var(--text-secondary)' }}
            >
              <Layers size={12} style={{ color: 'var(--accent-cyan)' }} />
              <span>React 18 + Vite</span>
            </span>
            <span
              className="badge"
              style={{ background: 'var(--bg-tertiary)', color: 'var(--text-secondary)' }}
            >
              <Database size={12} style={{ color: 'var(--accent-emerald)' }} />
              <span>PostgreSQL 16</span>
            </span>
          </div>
        </div>

        <div
          style={{
            borderTop: '1px solid var(--border-color)',
            paddingTop: '16px',
            display: 'flex',
            flexWrap: 'wrap',
            justifyContent: 'space-between',
            alignItems: 'center',
            gap: '12px',
            fontSize: '0.75rem',
          }}
        >
          <div>
            &copy; {new Date().getFullYear()} ProjectForge AI. Modular Monolith Architecture.
          </div>
          <div style={{ display: 'flex', gap: '16px' }}>
            <a href="/docs" style={{ color: 'var(--text-secondary)' }}>
              Documentation
            </a>
            <span>•</span>
            <span style={{ color: 'var(--accent-emerald)' }}>Phase 1 Foundation Active</span>
          </div>
        </div>
      </div>
    </footer>
  );
}
