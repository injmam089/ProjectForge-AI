import React, { useState } from 'react';
import { History, X, RotateCcw, Eye, FileText, Check, AlertCircle } from 'lucide-react';

const CHANGE_TYPE_CONFIG = {
  GENERATE: { label: 'Generated', color: '#3b82f6', bg: 'rgba(59, 130, 246, 0.15)' },
  MANUAL_EDIT: { label: 'Manual Edit', color: '#10b981', bg: 'rgba(16, 185, 129, 0.15)' },
  AI_EDIT: { label: 'AI Edit', color: '#a855f7', bg: 'rgba(168, 85, 247, 0.15)' },
  REGENERATE: { label: 'Regenerated', color: '#06b6d4', bg: 'rgba(6, 182, 212, 0.15)' },
  RESTORE: { label: 'Restored', color: '#f59e0b', bg: 'rgba(245, 158, 11, 0.15)' },
};

export function DocumentVersionHistory({
  versions = [],
  currentVersion,
  onClose,
  onPreviewVersion,
  onRestoreVersion,
  loading,
}) {
  const [confirmRestoreVersion, setConfirmRestoreVersion] = useState(null);

  const formatDate = (isoString) => {
    if (!isoString) return '';
    try {
      const d = new Date(isoString);
      return d.toLocaleString(undefined, {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return isoString;
    }
  };

  return (
    <aside
      style={{
        width: '360px',
        minWidth: '320px',
        maxWidth: '400px',
        background: 'var(--bg-secondary, #1e293b)',
        borderLeft: '1px solid var(--border-color, #334155)',
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        boxShadow: '-4px 0 16px rgba(0, 0, 0, 0.25)',
      }}
    >
      {/* Header */}
      <div
        style={{
          padding: '16px 20px',
          borderBottom: '1px solid var(--border-color, #334155)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <History size={18} color="#6366f1" />
          <span style={{ fontWeight: 700, fontSize: '0.95rem', color: '#f8fafc' }}>
            Version History ({versions.length})
          </span>
        </div>
        <button
          type="button"
          onClick={onClose}
          className="btn btn-ghost"
          style={{ padding: '4px' }}
          title="Close version history"
        >
          <X size={16} />
        </button>
      </div>

      {/* Confirmation Modal Overlay for Restore */}
      {confirmRestoreVersion && (
        <div
          style={{
            position: 'absolute',
            inset: 0,
            background: 'rgba(15, 23, 42, 0.85)',
            zIndex: 10,
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'center',
            padding: '24px',
            backdropFilter: 'blur(3px)',
          }}
        >
          <div
            style={{
              background: '#1e293b',
              border: '1px solid #475569',
              borderRadius: '8px',
              padding: '20px',
              display: 'flex',
              flexDirection: 'column',
              gap: '12px',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#f59e0b' }}>
              <RotateCcw size={18} />
              <span style={{ fontWeight: 700, fontSize: '0.95rem' }}>
                Restore Version {confirmRestoreVersion.version}?
              </span>
            </div>
            <p style={{ fontSize: '0.82rem', color: '#cbd5e1', lineHeight: 1.5, margin: 0 }}>
              Restoring will create a new version snapshot containing the exact content of version {confirmRestoreVersion.version}. Existing history is preserved.
            </p>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '8px' }}>
              <button
                type="button"
                className="btn btn-ghost"
                onClick={() => setConfirmRestoreVersion(null)}
                style={{ fontSize: '0.8rem', padding: '6px 12px' }}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => {
                  const v = confirmRestoreVersion.version;
                  setConfirmRestoreVersion(null);
                  onRestoreVersion(v);
                }}
                style={{
                  fontSize: '0.8rem',
                  padding: '6px 14px',
                  background: '#f59e0b',
                  color: '#0f172a',
                  border: 'none',
                  fontWeight: 600,
                }}
              >
                Confirm Restore
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Version Timeline List */}
      <div
        style={{
          flex: 1,
          overflowY: 'auto',
          padding: '16px',
          display: 'flex',
          flexDirection: 'column',
          gap: '12px',
        }}
      >
        {versions.length === 0 ? (
          <div style={{ padding: '24px', textAlign: 'center', color: '#64748b', fontSize: '0.85rem' }}>
            No version history recorded yet.
          </div>
        ) : (
          versions.map((ver) => {
            const isCurrent = ver.version === currentVersion;
            const typeConfig = CHANGE_TYPE_CONFIG[ver.changeType] || {
              label: ver.changeType,
              color: '#94a3b8',
              bg: 'rgba(148, 163, 184, 0.1)',
            };

            return (
              <div
                key={ver.id || ver.version}
                style={{
                  background: isCurrent ? 'rgba(99, 102, 241, 0.08)' : 'var(--bg-primary, #0f172a)',
                  border: isCurrent ? '1px solid #6366f1' : '1px solid var(--border-color, #334155)',
                  borderRadius: '8px',
                  padding: '14px',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '8px',
                }}
              >
                {/* Top line: Version and Badge */}
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span
                      style={{
                        fontWeight: 700,
                        fontFamily: 'ui-monospace, monospace',
                        fontSize: '0.92rem',
                        color: '#f8fafc',
                      }}
                    >
                      v{ver.version}
                    </span>
                    <span
                      style={{
                        fontSize: '0.72rem',
                        padding: '2px 8px',
                        borderRadius: '4px',
                        background: typeConfig.bg,
                        color: typeConfig.color,
                        fontWeight: 600,
                      }}
                    >
                      {typeConfig.label}
                    </span>
                  </div>

                  {isCurrent && (
                    <span
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '4px',
                        fontSize: '0.7rem',
                        color: '#818cf8',
                        fontWeight: 600,
                      }}
                    >
                      <Check size={12} /> Current
                    </span>
                  )}
                </div>

                {/* Instruction if AI */}
                {ver.instruction && (
                  <div
                    style={{
                      fontSize: '0.75rem',
                      color: '#cbd5e1',
                      background: 'rgba(255, 255, 255, 0.03)',
                      padding: '6px 8px',
                      borderRadius: '4px',
                      borderLeft: '3px solid #a855f7',
                      fontStyle: 'italic',
                    }}
                  >
                    "{ver.instruction}"
                  </div>
                )}

                {/* Metadata */}
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    fontSize: '0.72rem',
                    color: '#94a3b8',
                  }}
                >
                  <span>{formatDate(ver.createdAt)}</span>
                  <span>{ver.characterCount?.toLocaleString() || 0} chars</span>
                </div>

                {/* Actions */}
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'flex-end',
                    gap: '8px',
                    paddingTop: '6px',
                    borderTop: '1px solid rgba(51, 65, 85, 0.3)',
                  }}
                >
                  <button
                    type="button"
                    onClick={() => onPreviewVersion(ver.version)}
                    className="btn btn-ghost"
                    style={{ fontSize: '0.72rem', padding: '4px 8px', display: 'flex', alignItems: 'center', gap: '4px' }}
                    title="Preview this historical version"
                  >
                    <Eye size={12} /> Preview
                  </button>

                  {!isCurrent && (
                    <button
                      type="button"
                      onClick={() => setConfirmRestoreVersion(ver)}
                      className="btn btn-ghost"
                      style={{
                        fontSize: '0.72rem',
                        padding: '4px 8px',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '4px',
                        color: '#f59e0b',
                      }}
                      title="Restore as new version snapshot"
                    >
                      <RotateCcw size={12} /> Restore
                    </button>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>
    </aside>
  );
}
