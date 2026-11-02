import React from 'react';
import { Archive, X, Loader2, AlertTriangle, ShieldCheck, FileText, CheckCircle2 } from 'lucide-react';

export function ExportZipModal({
  isOpen,
  onClose,
  projectName = 'Project',
  documentsCount = 0,
  onConfirmExport,
  loading = false,
  statusMessage = '',
  error = null,
  success = false,
}) {
  if (!isOpen) return null;

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.75)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1200,
        padding: '20px',
        backdropFilter: 'blur(3px)',
      }}
      onClick={(e) => {
        if (!loading && e.target === e.currentTarget) onClose();
      }}
    >
      <div
        className="card"
        style={{
          width: '100%',
          maxWidth: '540px',
          background: '#1e293b',
          border: '1px solid #475569',
          padding: '24px',
          display: 'flex',
          flexDirection: 'column',
          gap: '16px',
          boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5)',
        }}
      >
        {/* Header */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', color: '#38bdf8' }}>
            <Archive size={22} />
            <span style={{ fontWeight: 700, fontSize: '1.05rem', color: '#f8fafc' }}>
              Export Project Documentation
            </span>
          </div>
          {!loading && (
            <button
              type="button"
              onClick={onClose}
              className="btn btn-ghost"
              style={{ padding: '4px' }}
              title="Close modal"
            >
              <X size={16} />
            </button>
          )}
        </div>

        {/* Description */}
        <p style={{ fontSize: '0.86rem', color: '#cbd5e1', lineHeight: 1.5, margin: 0 }}>
          Package all current active Markdown documents into a secure downloadable ZIP file.
        </p>

        {/* Metadata Card */}
        <div
          style={{
            background: '#0f172a',
            border: '1px solid #334155',
            borderRadius: '8px',
            padding: '14px 16px',
            display: 'flex',
            flexDirection: 'column',
            gap: '8px',
            fontSize: '0.82rem',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', color: '#94a3b8' }}>
            <span>Project:</span>
            <span style={{ color: '#f8fafc', fontWeight: 600 }}>{projectName}</span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', color: '#94a3b8' }}>
            <span>Contents:</span>
            <span style={{ color: '#38bdf8', fontWeight: 600 }}>
              {documentsCount} Markdown files + EXPORT_MANIFEST.json
            </span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', color: '#94a3b8' }}>
            <span>Package Format:</span>
            <span style={{ color: '#a5b4fc', fontFamily: 'monospace' }}>
              ProjectForge_*.zip (v1.0)
            </span>
          </div>
        </div>

        {/* Security & Content Note */}
        <div
          style={{
            padding: '10px 14px',
            background: 'rgba(56, 189, 248, 0.08)',
            border: '1px solid rgba(56, 189, 248, 0.25)',
            borderRadius: '6px',
            display: 'flex',
            gap: '10px',
            alignItems: 'flex-start',
            fontSize: '0.78rem',
            color: '#bae6fd',
            lineHeight: 1.45,
          }}
        >
          <ShieldCheck size={18} style={{ flexShrink: 0, marginTop: '2px', color: '#38bdf8' }} />
          <div>
            <strong>Strictly read-only & isolated:</strong> Secrets, API keys, and environment files are never packaged. The approved Project Specification remains 100% immutable.
          </div>
        </div>

        {/* Error Alert */}
        {error && (
          <div
            style={{
              padding: '10px 14px',
              background: 'rgba(239, 68, 68, 0.1)',
              border: '1px solid rgba(239, 68, 68, 0.3)',
              borderRadius: '6px',
              display: 'flex',
              gap: '10px',
              alignItems: 'flex-start',
              color: '#f87171',
              fontSize: '0.82rem',
            }}
          >
            <AlertTriangle size={18} style={{ flexShrink: 0, marginTop: '2px' }} />
            <div>{error}</div>
          </div>
        )}

        {/* Success Alert */}
        {success && (
          <div
            style={{
              padding: '10px 14px',
              background: 'rgba(16, 185, 129, 0.1)',
              border: '1px solid rgba(16, 185, 129, 0.3)',
              borderRadius: '6px',
              display: 'flex',
              gap: '10px',
              alignItems: 'center',
              color: '#34d399',
              fontSize: '0.82rem',
            }}
          >
            <CheckCircle2 size={18} style={{ flexShrink: 0 }} />
            <div>Documentation ZIP package downloaded successfully!</div>
          </div>
        )}

        {/* Footer Actions */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '6px' }}>
          <button
            type="button"
            className="btn btn-ghost"
            onClick={onClose}
            disabled={loading}
            style={{ fontSize: '0.82rem', padding: '6px 14px' }}
          >
            Cancel
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={onConfirmExport}
            disabled={loading}
            style={{
              fontSize: '0.82rem',
              padding: '8px 16px',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              background: 'linear-gradient(135deg, #0284c7, #0369a1)',
              border: 'none',
              fontWeight: 600,
            }}
          >
            {loading ? (
              <>
                <Loader2 size={15} className="animate-spin" />
                <span>{statusMessage || 'Packaging ZIP...'}</span>
              </>
            ) : (
              <>
                <Archive size={15} />
                <span>Export ZIP Package</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
