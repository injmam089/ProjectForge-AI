import React from 'react';
import { Layers, FileText, CheckCircle2, FileCode2, ArrowRight } from 'lucide-react';

export function EmptyState({ onSelectTemplate }) {
  const blueprints = [
    {
      title: 'DevOps Health Orchestrator',
      type: 'FULLSTACK_WEB',
      difficulty: 'INTERMEDIATE',
      desc: 'Automated infrastructure monitoring dashboard with metric alerts and incident logs.',
    },
    {
      title: 'AI Documentation Engine',
      type: 'REST_API',
      difficulty: 'ADVANCED',
      desc: 'Backend pipeline turning database schemas and OpenAPI specs into validated Markdown guides.',
    },
    {
      title: 'TaskFlow Developer CLI',
      type: 'CLI_TOOL',
      difficulty: 'BEGINNER',
      desc: 'Lightweight terminal assistant for project scaffolding and local git worktree management.',
    },
  ];

  const steps = [
    { number: '01', title: 'Rough Idea', desc: 'Describe vision & constraints' },
    { number: '02', title: 'AI Analysis', desc: 'Deconstruct scope & roles' },
    { number: '03', title: 'Structured Spec', desc: 'JSON source of truth' },
    { number: '04', title: 'User Approval', desc: 'Human in the loop' },
    { number: '05', title: 'Markdown Package', desc: 'Consistent docs & ZIP' },
  ];

  return (
    <div
      className="card"
      style={{
        padding: '40px 32px',
        textAlign: 'center',
        marginTop: '24px',
        borderStyle: 'dashed',
        borderColor: 'rgba(148, 163, 184, 0.2)',
      }}
    >
      {/* Icon with glowing ring */}
      <div
        style={{
          width: '64px',
          height: '64px',
          borderRadius: 'var(--radius-xl)',
          background: 'radial-gradient(circle, rgba(99, 102, 241, 0.25) 0%, rgba(15, 23, 42, 0.5) 100%)',
          border: '1px solid var(--primary-border)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          margin: '0 auto 20px',
          color: 'var(--primary)',
          boxShadow: 'var(--glow-primary)',
        }}
      >
        <Layers size={30} />
      </div>

      <h3 style={{ fontSize: '1.25rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '8px' }}>
        No Software Projects Forged Yet
      </h3>

      <p
        style={{
          fontSize: '0.875rem',
          color: 'var(--text-secondary)',
          maxWidth: '560px',
          margin: '0 auto 32px',
          lineHeight: 1.6,
        }}
      >
        Your workspace is clean. Use the creation card above to input your rough software concept, or inspect the ProjectForge AI requirement pipeline below.
      </p>

      {/* Lifecycle Visualizer */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(170px, 1fr))',
          gap: '12px',
          marginBottom: '36px',
          textAlign: 'left',
        }}
      >
        {steps.map((step) => (
          <div
            key={step.number}
            style={{
              padding: '14px 16px',
              borderRadius: 'var(--radius-md)',
              background: 'var(--bg-secondary)',
              border: '1px solid var(--border-color)',
            }}
          >
            <div
              style={{
                fontSize: '0.75rem',
                fontFamily: 'var(--font-mono)',
                color: 'var(--primary)',
                fontWeight: 600,
                marginBottom: '4px',
              }}
            >
              {step.number}
            </div>
            <div style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)' }}>
              {step.title}
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '2px' }}>
              {step.desc}
            </div>
          </div>
        ))}
      </div>

      {/* Quick Start Blueprints */}
      <div style={{ textAlign: 'left', borderTop: '1px solid var(--border-color)', paddingTop: '24px' }}>
        <div
          style={{
            fontSize: '0.8125rem',
            fontWeight: 600,
            textTransform: 'uppercase',
            letterSpacing: '0.05em',
            color: 'var(--text-muted)',
            marginBottom: '16px',
          }}
        >
          Suggested Project Blueprints
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '16px' }}>
          {blueprints.map((item) => (
            <div
              key={item.title}
              className="card"
              style={{
                padding: '16px',
                background: 'var(--bg-secondary)',
                cursor: 'pointer',
              }}
              onClick={() => onSelectTemplate && onSelectTemplate(item)}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                  {item.title}
                </span>
                <span className="badge badge-primary" style={{ fontSize: '0.65rem' }}>
                  {item.difficulty}
                </span>
              </div>
              <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', lineHeight: 1.5, marginBottom: '12px' }}>
                {item.desc}
              </p>
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '4px',
                  fontSize: '0.75rem',
                  color: 'var(--primary)',
                  fontWeight: 600,
                }}
              >
                <span>Load into Forge</span>
                <ArrowRight size={13} />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
