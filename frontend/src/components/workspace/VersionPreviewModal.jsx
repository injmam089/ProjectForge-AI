import React, { useEffect } from 'react';
import { X, RotateCcw, Copy, Check, FileText } from 'lucide-react';
import { SafeMarkdownRenderer } from './SafeMarkdownRenderer';

export function VersionPreviewModal({
  isOpen,
  onClose,
  versionData,
  onRestore,
}) {
  const [copied, setCopied] = React.useState(false);
  const [viewRaw, setViewRaw] = React.useState(false);

  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen || !versionData) return null;

  const handleCopy = () => {
    if (versionData.content) {
      navigator.clipboard.writeText(versionData.content);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="version-preview-title"
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.75)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1100,
        padding: '20px',
        backdropFilter: 'blur(4px)',
      }}
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        className="card"
        style={{
          width: '100%',
          maxWidth: '960px',
          height: '85vh',
          display: 'flex',
          flexDirection: 'column',
          padding: 0,
          background: 'var(--bg-primary, #0f172a)',
          border: '1px solid var(--border-color, #334155)',
          boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5)',
          overflow: 'hidden',
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
            background: 'var(--bg-secondary, #1e293b)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <FileText size={18} color="#6366f1" />
            <div>
              <h3 id="version-preview-title" style={{ fontWeight: 700, fontSize: '0.95rem', color: '#f8fafc', margin: 0 }}>
                Historical Snapshot: Version {versionData.version}
              </h3>
              <div style={{ fontSize: '0.72rem', color: '#94a3b8' }}>
                {versionData.changeType} • {versionData.characterCount || versionData.content?.length || 0} characters • Created by {versionData.createdBy}
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            {/* View Mode Toggle */}
            <div
              style={{
                display: 'flex',
                background: '#0f172a',
                padding: '2px',
                borderRadius: '6px',
                border: '1px solid #334155',
                fontSize: '0.75rem',
              }}
            >
              <button
                type="button"
                onClick={() => setViewRaw(false)}
                style={{
                  padding: '4px 10px',
                  borderRadius: '4px',
                  border: 'none',
                  background: !viewRaw ? '#6366f1' : 'transparent',
                  color: !viewRaw ? '#ffffff' : '#94a3b8',
                  cursor: 'pointer',
                  fontWeight: 600,
                }}
              >
                Rendered
              </button>
              <button
                type="button"
                onClick={() => setViewRaw(true)}
                style={{
                  padding: '4px 10px',
                  borderRadius: '4px',
                  border: 'none',
                  background: viewRaw ? '#6366f1' : 'transparent',
                  color: viewRaw ? '#ffffff' : '#94a3b8',
                  cursor: 'pointer',
                  fontWeight: 600,
                }}
              >
                Raw
              </button>
            </div>

            <button
              type="button"
              onClick={handleCopy}
              className="btn btn-ghost"
              style={{ fontSize: '0.75rem', padding: '6px 10px', display: 'flex', alignItems: 'center', gap: '6px' }}
              title="Copy historical content"
            >
              {copied ? <Check size={14} color="#10b981" /> : <Copy size={14} />}
              <span>{copied ? 'Copied' : 'Copy'}</span>
            </button>

            <button
              type="button"
              onClick={() => onRestore(versionData.version)}
              className="btn btn-primary"
              style={{
                fontSize: '0.75rem',
                padding: '6px 12px',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                background: '#f59e0b',
                color: '#0f172a',
                border: 'none',
                fontWeight: 600,
              }}
              title="Restore this version snapshot"
            >
              <RotateCcw size={14} />
              <span>Restore this Version</span>
            </button>

            <button
              type="button"
              onClick={onClose}
              className="btn btn-ghost"
              style={{ padding: '6px' }}
              aria-label="Close modal"
              title="Close modal"
            >
              <X size={18} />
            </button>
          </div>
        </div>

        {/* Content Body */}
        <div
          style={{
            flex: 1,
            overflowY: 'auto',
            padding: '24px 32px',
            backgroundColor: viewRaw ? '#0a0f1d' : '#0f172a',
          }}
        >
          {viewRaw ? (
            <pre
              style={{
                margin: 0,
                fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
                fontSize: '0.86rem',
                lineHeight: 1.6,
                color: '#e2e8f0',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
              }}
            >
              {versionData.content}
            </pre>
          ) : (
            <SafeMarkdownRenderer content={versionData.content} />
          )}
        </div>
      </div>
    </div>
  );
}
