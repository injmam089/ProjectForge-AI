import React, { useState, useEffect } from 'react';
import { 
  History, X, Clock, User, ChevronRight, ChevronDown, 
  FileText, Sparkles, CheckCircle2, ShieldCheck, Download, 
  RotateCcw, RefreshCw, AlertCircle, FileEdit
} from 'lucide-react';
import { projectService } from '../../services/projectService';

const EVENT_CONFIG = {
  PROJECT_CREATED: {
    label: 'Project Created',
    color: '#818cf8',
    bg: 'rgba(129, 140, 248, 0.15)',
    border: 'rgba(129, 140, 248, 0.3)',
    icon: FileEdit,
  },
  ANALYSIS_STARTED: {
    label: 'Analysis Started',
    color: '#6366f1',
    bg: 'rgba(99, 102, 241, 0.15)',
    border: 'rgba(99, 102, 241, 0.3)',
    icon: Sparkles,
  },
  REQUIREMENTS_READY: {
    label: 'Requirements Ready',
    color: '#10b981',
    bg: 'rgba(16, 185, 129, 0.15)',
    border: 'rgba(16, 185, 129, 0.3)',
    icon: CheckCircle2,
  },
  REVIEW_UPDATED: {
    label: 'Review Updated',
    color: '#f59e0b',
    bg: 'rgba(245, 158, 11, 0.15)',
    border: 'rgba(245, 158, 11, 0.3)',
    icon: FileText,
  },
  PROJECT_APPROVED: {
    label: 'Project Approved',
    color: '#34d399',
    bg: 'rgba(52, 211, 153, 0.15)',
    border: 'rgba(52, 211, 153, 0.3)',
    icon: ShieldCheck,
  },
  DOCUMENTS_GENERATED: {
    label: 'Documents Generated',
    color: '#06b6d4',
    bg: 'rgba(6, 182, 212, 0.15)',
    border: 'rgba(6, 182, 212, 0.3)',
    icon: FileText,
  },
  DOCUMENT_EDITED: {
    label: 'Document Edited',
    color: '#3b82f6',
    bg: 'rgba(59, 130, 246, 0.15)',
    border: 'rgba(59, 130, 246, 0.3)',
    icon: FileEdit,
  },
  DOCUMENT_AI_EDITED: {
    label: 'AI Edited',
    color: '#a855f7',
    bg: 'rgba(168, 85, 247, 0.15)',
    border: 'rgba(168, 85, 247, 0.3)',
    icon: Sparkles,
  },
  DOCUMENT_RESTORED: {
    label: 'Version Restored',
    color: '#f97316',
    bg: 'rgba(249, 115, 22, 0.15)',
    border: 'rgba(249, 115, 22, 0.3)',
    icon: RotateCcw,
  },
  DOCUMENT_REGENERATED: {
    label: 'Document Regenerated',
    color: '#ec4899',
    bg: 'rgba(236, 72, 153, 0.15)',
    border: 'rgba(236, 72, 153, 0.3)',
    icon: RefreshCw,
  },
  PROJECT_EXPORTED: {
    label: 'Project Exported',
    color: '#8b5cf6',
    bg: 'rgba(139, 92, 246, 0.15)',
    border: 'rgba(139, 92, 246, 0.3)',
    icon: Download,
  },
};

export function ProjectHistoryModal({
  isOpen,
  onClose,
  projectId,
  projectName,
}) {
  const [historyItems, setHistoryItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [expandedMetaId, setExpandedMetaId] = useState(null);

  useEffect(() => {
    if (isOpen && projectId) {
      loadHistory(0);
    }
  }, [isOpen, projectId]);

  const loadHistory = async (pageNum = 0) => {
    setLoading(true);
    setError('');
    try {
      const res = await projectService.getProjectHistory(projectId, { page: pageNum, size: 15 });
      if (res && Array.isArray(res.content)) {
        setHistoryItems(res.content);
        setPage(res.page || 0);
        setTotalPages(res.totalPages || 1);
      } else if (Array.isArray(res)) {
        setHistoryItems(res);
        setTotalPages(1);
      }
    } catch (err) {
      setError(err.message || 'Failed to load project history.');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.75)',
        backdropFilter: 'blur(4px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
        padding: '16px',
      }}
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        className="card animate-fade-in"
        style={{
          width: '100%',
          maxWidth: '680px',
          maxHeight: '85vh',
          display: 'flex',
          flexDirection: 'column',
          padding: '24px',
          background: 'var(--bg-primary)',
          border: '1px solid var(--border-color)',
          borderRadius: 'var(--radius-lg)',
          boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.5)',
        }}
      >
        {/* Header */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            paddingBottom: '16px',
            borderBottom: '1px solid var(--border-color)',
            marginBottom: '16px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: '8px',
                background: 'rgba(129, 140, 248, 0.15)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#818cf8',
              }}
            >
              <History size={18} />
            </div>
            <div>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
                Project Activity & History
              </h3>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', margin: 0 }}>
                {projectName ? `Timeline for "${projectName}"` : 'Lifecycle audit trail'}
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="btn btn-ghost"
            style={{ padding: '6px', color: 'var(--text-muted)' }}
          >
            <X size={18} />
          </button>
        </div>

        {/* Content Body */}
        <div
          style={{
            flex: 1,
            overflowY: 'auto',
            paddingRight: '6px',
            display: 'flex',
            flexDirection: 'column',
            gap: '12px',
          }}
        >
          {loading && historyItems.length === 0 ? (
            <div style={{ padding: '40px 0', textAlign: 'center', color: 'var(--text-muted)' }}>
              <RefreshCw size={24} className="animate-spin" style={{ margin: '0 auto 12px' }} />
              <p style={{ fontSize: '0.875rem' }}>Loading activity history...</p>
            </div>
          ) : error ? (
            <div
              style={{
                padding: '16px',
                borderRadius: 'var(--radius-md)',
                background: 'rgba(239, 68, 68, 0.12)',
                border: '1px solid rgba(239, 68, 68, 0.3)',
                color: '#f87171',
                fontSize: '0.875rem',
              }}
            >
              <AlertCircle size={16} style={{ display: 'inline', marginRight: '6px' }} />
              {error}
            </div>
          ) : historyItems.length === 0 ? (
            <div style={{ padding: '40px 0', textAlign: 'center', color: 'var(--text-muted)' }}>
              <History size={32} style={{ opacity: 0.3, margin: '0 auto 12px' }} />
              <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)' }}>No history events recorded yet.</p>
            </div>
          ) : (
            <div style={{ position: 'relative', paddingLeft: '28px' }}>
              {/* Vertical timeline line */}
              <div
                style={{
                  position: 'absolute',
                  left: '11px',
                  top: '12px',
                  bottom: '12px',
                  width: '2px',
                  background: 'var(--border-color)',
                }}
              />

              {historyItems.map((item, idx) => {
                const conf = EVENT_CONFIG[item.eventType] || {
                  label: item.eventType,
                  color: '#94a3b8',
                  bg: 'rgba(148, 163, 184, 0.15)',
                  border: 'rgba(148, 163, 184, 0.3)',
                  icon: Clock,
                };
                const Icon = conf.icon;
                const isMetaExpanded = expandedMetaId === item.id;

                return (
                  <div
                    key={item.id || idx}
                    style={{
                      position: 'relative',
                      marginBottom: '18px',
                    }}
                  >
                    {/* Timeline Node Dot */}
                    <div
                      style={{
                        position: 'absolute',
                        left: '-28px',
                        top: '4px',
                        width: '22px',
                        height: '22px',
                        borderRadius: '50%',
                        background: conf.bg,
                        border: `2px solid ${conf.color}`,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: conf.color,
                        zIndex: 1,
                      }}
                    >
                      <Icon size={11} />
                    </div>

                    {/* Timeline Card */}
                    <div
                      style={{
                        padding: '12px 14px',
                        borderRadius: 'var(--radius-md)',
                        background: 'var(--bg-secondary)',
                        border: `1px solid ${conf.border}`,
                        display: 'flex',
                        flexDirection: 'column',
                        gap: '6px',
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '6px' }}>
                        <span
                          className="badge"
                          style={{
                            background: conf.bg,
                            color: conf.color,
                            border: `1px solid ${conf.border}`,
                            fontSize: '0.725rem',
                            fontWeight: 700,
                          }}
                        >
                          {conf.label}
                        </span>

                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '0.725rem', color: 'var(--text-muted)' }}>
                          <span style={{ display: 'flex', alignItems: 'center', gap: '3px' }}>
                            <User size={11} />
                            {item.createdBy || 'system'}
                          </span>
                          <span style={{ display: 'flex', alignItems: 'center', gap: '3px' }}>
                            <Clock size={11} />
                            {item.createdAt ? new Date(item.createdAt).toLocaleString() : 'Just now'}
                          </span>
                        </div>
                      </div>

                      <p style={{ fontSize: '0.85rem', color: 'var(--text-primary)', margin: 0, lineHeight: 1.45 }}>
                        {item.description}
                      </p>

                      {item.metadata && (
                        <div>
                          <button
                            type="button"
                            onClick={() => setExpandedMetaId(isMetaExpanded ? null : item.id)}
                            style={{
                              background: 'transparent',
                              border: 'none',
                              color: 'var(--primary)',
                              fontSize: '0.725rem',
                              padding: 0,
                              cursor: 'pointer',
                              display: 'flex',
                              alignItems: 'center',
                              gap: '4px',
                              marginTop: '2px',
                            }}
                          >
                            {isMetaExpanded ? <ChevronDown size={12} /> : <ChevronRight size={12} />}
                            <span>{isMetaExpanded ? 'Hide Details' : 'View Details'}</span>
                          </button>

                          {isMetaExpanded && (
                            <pre
                              style={{
                                marginTop: '6px',
                                padding: '8px 10px',
                                background: 'var(--bg-tertiary)',
                                borderRadius: 'var(--radius-sm)',
                                fontSize: '0.725rem',
                                color: 'var(--text-secondary)',
                                overflowX: 'auto',
                                maxHeight: '160px',
                                fontFamily: 'var(--font-mono)',
                                whiteSpace: 'pre-wrap',
                                wordBreak: 'break-word',
                              }}
                            >
                              {item.metadata}
                            </pre>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Pagination Footer */}
        {totalPages > 1 && (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              paddingTop: '14px',
              borderTop: '1px solid var(--border-color)',
              marginTop: '12px',
              fontSize: '0.8rem',
            }}
          >
            <button
              onClick={() => loadHistory(page - 1)}
              disabled={page <= 0 || loading}
              className="btn btn-ghost"
              style={{ padding: '4px 10px', fontSize: '0.75rem' }}
            >
              Previous
            </button>
            <span style={{ color: 'var(--text-muted)' }}>
              Page {page + 1} of {totalPages}
            </span>
            <button
              onClick={() => loadHistory(page + 1)}
              disabled={page >= totalPages - 1 || loading}
              className="btn btn-ghost"
              style={{ padding: '4px 10px', fontSize: '0.75rem' }}
            >
              Next
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

export default ProjectHistoryModal;
