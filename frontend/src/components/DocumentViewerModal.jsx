import React from 'react';
import { X, FileText, Copy, Check } from 'lucide-react';

export function DocumentViewerModal({ isOpen, onClose, document, loading }) {
  const [copied, setCopied] = React.useState(false);

  if (!isOpen) return null;

  const handleCopy = () => {
    if (document?.content) {
      navigator.clipboard.writeText(document.content);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.75)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
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
          maxWidth: '900px',
          maxHeight: '90vh',
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
            padding: '18px 24px',
            borderBottom: '1px solid var(--border-color, #334155)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            background: 'var(--bg-secondary, #1e293b)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <FileText size={20} color="#6366f1" />
            <div>
              <div style={{ fontWeight: 800, fontSize: '1.05rem', color: 'var(--text-primary, #f8fafc)', fontFamily: 'var(--font-mono)' }}>
                {document?.filename || 'Document'}
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted, #94a3b8)' }}>
                {document?.type} • {document?.characterCount || document?.content?.length || 0} characters
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            {document?.content && (
              <button
                type="button"
                onClick={handleCopy}
                className="btn btn-ghost"
                style={{ fontSize: '0.8rem', padding: '6px 12px', display: 'flex', alignItems: 'center', gap: '6px' }}
                title="Copy markdown content"
              >
                {copied ? <Check size={14} color="#10b981" /> : <Copy size={14} />}
                <span>{copied ? 'Copied' : 'Copy'}</span>
              </button>
            )}
            <button
              type="button"
              onClick={onClose}
              className="btn btn-ghost"
              style={{ padding: '6px' }}
              title="Close modal"
            >
              <X size={18} />
            </button>
          </div>
        </div>

        {/* Content Body */}
        <div
          style={{
            padding: '24px',
            overflowY: 'auto',
            flex: 1,
            backgroundColor: '#0b1120',
          }}
        >
          {loading ? (
            <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
              Loading document content...
            </div>
          ) : (
            <pre
              style={{
                margin: 0,
                fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
                fontSize: '0.875rem',
                lineHeight: 1.6,
                color: '#e2e8f0',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
              }}
            >
              {document?.content || 'No content available.'}
            </pre>
          )}
        </div>
      </div>
    </div>
  );
}
