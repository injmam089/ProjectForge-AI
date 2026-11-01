import React from 'react';
import { FolderGit2, FileEdit, Sparkles, CheckCircle2, FileCheck, Archive } from 'lucide-react';

export function ProjectsOverview({ projects = [] }) {
  const stats = [
    {
      label: 'Total Projects',
      value: projects.length,
      icon: FolderGit2,
      color: 'var(--primary)',
      bg: 'var(--primary-light)',
      caption: 'All user workspaces',
    },
    {
      label: 'Draft Projects',
      value: projects.filter((p) => p.status === 'DRAFT').length,
      icon: FileEdit,
      color: 'var(--text-secondary)',
      bg: 'var(--bg-tertiary)',
      caption: 'Initial intake phase',
    },
    {
      label: 'Ready for Analysis',
      value: projects.filter((p) => p.status === 'READY_FOR_ANALYSIS').length,
      icon: Sparkles,
      color: 'var(--accent-amber)',
      bg: 'var(--accent-amber-light)',
      caption: 'Validated for AI queue',
    },
    {
      label: 'Requirements Ready',
      value: projects.filter((p) => p.status === 'REQUIREMENTS_READY').length,
      icon: CheckCircle2,
      color: 'var(--primary)',
      bg: 'var(--primary-light)',
      caption: 'Awaiting user review',
    },
    {
      label: 'Generated',
      value: projects.filter((p) => p.status === 'GENERATED').length,
      icon: FileCheck,
      color: 'var(--accent-emerald)',
      bg: 'var(--accent-emerald-light)',
      caption: 'Markdown packages ready',
    },
    {
      label: 'Exported',
      value: projects.filter((p) => p.status === 'EXPORTED').length,
      icon: Archive,
      color: 'var(--accent-cyan)',
      bg: 'var(--accent-cyan-light)',
      caption: 'ZIP archives downloaded',
    },
  ];

  return (
    <div style={{ marginBottom: '32px' }}>
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
          gap: '14px',
        }}
      >
        {stats.map((stat) => {
          const Icon = stat.icon;
          return (
            <div
              key={stat.label}
              className="card"
              style={{
                padding: '18px',
                display: 'flex',
                alignItems: 'flex-start',
                justifyContent: 'space-between',
              }}
            >
              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', fontWeight: 500, marginBottom: '4px' }}>
                  {stat.label}
                </div>
                <div style={{ fontSize: '1.5rem', fontWeight: 700, letterSpacing: '-0.02em', color: 'var(--text-primary)' }}>
                  {stat.value}
                </div>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                  {stat.caption}
                </div>
              </div>
              <div
                style={{
                  width: '32px',
                  height: '32px',
                  borderRadius: 'var(--radius-md)',
                  background: stat.bg,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: stat.color,
                }}
              >
                <Icon size={16} />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
