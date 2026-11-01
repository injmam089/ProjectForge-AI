import React from 'react';
import { BookOpen, Layers, CheckCircle2, ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';

export function DocsPage() {
  const phases = [
    { number: 1, name: 'Foundation', status: 'COMPLETED', desc: 'Repo layout, Spring Boot backend, React + Vite frontend, PostgreSQL/Docker Compose, health check.' },
    { number: 2, name: 'Authentication', status: 'PLANNED', desc: 'User registration, login, BCrypt password hashing, session/token security.' },
    { number: 3, name: 'Project Management', status: 'PLANNED', desc: 'Project CRUD operations, ownership verification, metadata management.' },
    { number: 4, name: 'AI Requirement Engine', status: 'PLANNED', desc: 'Backend-only OpenAI API, structured output validation, clarification questions.' },
    { number: 5, name: 'Requirements Workspace', status: 'PLANNED', desc: 'Review, edit, section regeneration, explicit user approval.' },
    { number: 6, name: 'Markdown Generation', status: 'PLANNED', desc: 'Document planner, 13 core Markdown templates from approved JSON specification.' },
    { number: 7, name: 'Markdown Workspace', status: 'PLANNED', desc: 'Document list, preview, editor, AI-assisted modifications.' },
    { number: 8, name: 'ZIP Export', status: 'PLANNED', desc: 'Path-traversal safe server-side ZIP archive creation and download.' },
    { number: 9, name: 'Dashboard', status: 'PLANNED', desc: 'Project history, lifecycle tracking, search and filtering.' },
    { number: 10, name: 'Hardening', status: 'PLANNED', desc: 'Security review, rate limiting, comprehensive integration testing.' },
    { number: 11, name: 'Deployment', status: 'PLANNED', desc: 'Production docker containers, environment profiles, production docs.' },
    { number: 12, name: 'Polish', status: 'PLANNED', desc: 'Performance optimization, accessibility, final UX refinement.' },
  ];

  return (
    <div>
      <div style={{ marginBottom: '32px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
          <span className="badge badge-primary">
            <BookOpen size={12} />
            <span>Architecture & Roadmap</span>
          </span>
        </div>
        <h1 style={{ fontSize: '2rem', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '8px' }}>
          ProjectForge AI Documentation
        </h1>
        <p style={{ color: 'var(--text-secondary)', maxWidth: '640px' }}>
          Review the engineering blueprint, technical specifications, and 12-phase development plan.
        </p>
      </div>

      {/* Tech Stack Matrix */}
      <div className="card" style={{ padding: '24px', marginBottom: '32px' }}>
        <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '16px' }}>
          Core Technology Stack (V1 Modular Monolith)
        </h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '16px' }}>
          <div style={{ padding: '14px', borderRadius: 'var(--radius-md)', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)' }}>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>FRONTEND</div>
            <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>React 18 + Vite</div>
            <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginTop: '4px' }}>JavaScript, Responsive CSS, React Router DOM, Lucide Icons</div>
          </div>
          <div style={{ padding: '14px', borderRadius: 'var(--radius-md)', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)' }}>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>BACKEND</div>
            <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>Spring Boot 3.3.5</div>
            <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginTop: '4px' }}>Java 17+, Spring Web, Actuator, Spring Data JPA, Maven</div>
          </div>
          <div style={{ padding: '14px', borderRadius: 'var(--radius-md)', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)' }}>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>DATABASE</div>
            <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>PostgreSQL 16</div>
            <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginTop: '4px' }}>Docker Compose with persistent volume & health check</div>
          </div>
          <div style={{ padding: '14px', borderRadius: 'var(--radius-md)', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)' }}>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '4px' }}>SECURITY BASELINE</div>
            <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>Server-Side Isolation</div>
            <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginTop: '4px' }}>CORS restrictions, environment secrets, safe error envelopes</div>
          </div>
        </div>
      </div>

      {/* 12-Phase Roadmap */}
      <div className="card" style={{ padding: '24px' }}>
        <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '20px' }}>
          12-Phase Development Roadmap
        </h3>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {phases.map((phase) => (
            <div
              key={phase.number}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '14px 18px',
                borderRadius: 'var(--radius-md)',
                background: 'var(--bg-secondary)',
                border: '1px solid var(--border-color)',
                flexWrap: 'wrap',
                gap: '12px',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                <span
                  style={{
                    width: '28px',
                    height: '28px',
                    borderRadius: 'var(--radius-sm)',
                    background: phase.status === 'COMPLETED' ? 'var(--accent-emerald-light)' : 'var(--bg-tertiary)',
                    color: phase.status === 'COMPLETED' ? 'var(--accent-emerald)' : 'var(--text-muted)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '0.75rem',
                    fontWeight: 700,
                    fontFamily: 'var(--font-mono)',
                  }}
                >
                  {phase.number}
                </span>
                <div>
                  <div style={{ fontWeight: 600, color: 'var(--text-primary)', fontSize: '0.9375rem' }}>
                    Phase {phase.number}: {phase.name}
                  </div>
                  <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                    {phase.desc}
                  </div>
                </div>
              </div>

              <div>
                <span className={`badge ${phase.status === 'COMPLETED' ? 'badge-success' : 'badge-primary'}`}>
                  {phase.status === 'COMPLETED' ? 'Completed' : 'Planned Next'}
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
