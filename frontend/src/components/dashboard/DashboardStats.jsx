import React from 'react';
import { FolderGit2, FileEdit, AlertCircle, CheckCircle2, FileCheck, Archive } from 'lucide-react';

export function DashboardStats({ stats = {}, loading = false }) {
  const statCards = [
    {
      key: 'total',
      label: 'Total Projects',
      value: stats.totalProjects ?? 0,
      icon: FolderGit2,
      color: '#818cf8',
      bg: 'rgba(129, 140, 248, 0.12)',
      border: 'rgba(129, 140, 248, 0.25)',
      description: 'Active workspaces',
    },
    {
      key: 'draft',
      label: 'Draft Projects',
      value: stats.draft ?? stats.draftProjects ?? 0,
      icon: FileEdit,
      color: '#94a3b8',
      bg: 'rgba(148, 163, 184, 0.12)',
      border: 'rgba(148, 163, 184, 0.2)',
      description: 'Initial intake phase',
    },
    {
      key: 'underReview',
      label: 'Under Review',
      value: stats.underReview ?? stats.underReviewProjects ?? 0,
      icon: AlertCircle,
      color: '#f59e0b',
      bg: 'rgba(245, 158, 11, 0.12)',
      border: 'rgba(245, 158, 11, 0.25)',
      description: 'Clarifications & QA',
    },
    {
      key: 'approved',
      label: 'Approved',
      value: stats.approved ?? stats.approvedProjects ?? 0,
      icon: CheckCircle2,
      color: '#34d399',
      bg: 'rgba(52, 211, 153, 0.12)',
      border: 'rgba(52, 211, 153, 0.25)',
      description: 'Locked specifications',
    },
    {
      key: 'generated',
      label: 'Generated Docs',
      value: stats.generated ?? stats.generatedProjects ?? 0,
      icon: FileCheck,
      color: '#06b6d4',
      bg: 'rgba(6, 182, 212, 0.12)',
      border: 'rgba(6, 182, 212, 0.25)',
      description: 'Markdown packages ready',
    },
    {
      key: 'exported',
      label: 'Exported ZIPs',
      value: stats.exported ?? stats.exportedProjects ?? 0,
      icon: Archive,
      color: '#a78bfa',
      bg: 'rgba(167, 139, 250, 0.12)',
      border: 'rgba(167, 139, 250, 0.25)',
      description: 'Bundles packaged',
    },
  ];

  return (
    <div style={{ marginBottom: '28px' }}>
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
          gap: '14px',
        }}
      >
        {statCards.map((card) => {
          const Icon = card.icon;
          return (
            <div
              key={card.key}
              className="card"
              style={{
                padding: '16px 18px',
                background: 'var(--bg-secondary)',
                border: `1px solid ${card.border}`,
                borderRadius: 'var(--radius-md)',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                transition: 'transform 0.15s ease, border-color 0.15s ease',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
                <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                  {card.label}
                </span>
                <div
                  style={{
                    width: '32px',
                    height: '32px',
                    borderRadius: '8px',
                    background: card.bg,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: card.color,
                  }}
                >
                  <Icon size={16} />
                </div>
              </div>

              <div>
                <div
                  style={{
                    fontSize: '1.75rem',
                    fontWeight: 800,
                    color: 'var(--text-primary)',
                    lineHeight: 1.1,
                    marginBottom: '4px',
                  }}
                >
                  {loading ? (
                    <span style={{ opacity: 0.4 }}>...</span>
                  ) : (
                    card.value
                  )}
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  {card.description}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default DashboardStats;
