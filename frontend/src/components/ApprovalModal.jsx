import React, { useState, useEffect } from 'react';
import { X, ShieldCheck, CheckCircle2, AlertTriangle, Lock } from 'lucide-react';

export function ApprovalModal({
  isOpen,
  onClose,
  onConfirm,
  loading = false,
  error = '',
  projectName = '',
  qualityScore = 80,
  blockingCount = 0,
  featuresCount = 0,
}) {
  const [confirmed, setConfirmed] = useState(false);

  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && !loading) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, loading, onClose]);

  if (!isOpen) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!confirmed) return;
    onConfirm();
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="approval-modal-title"
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.78)',
        backdropFilter: 'blur(8px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 100,
        padding: '16px',
      }}
    >
      <div
        className="card"
        style={{
          width: '100%',
          maxWidth: '520px',
          padding: '30px',
          border: '1px solid rgba(16, 185, 129, 0.3)',
          background: 'var(--bg-card, #121826)',
          boxShadow: '0 20px 40px rgba(0, 0, 0, 0.5)',
          position: 'relative',
        }}
      >
        <button
          onClick={onClose}
          disabled={loading}
          aria-label="Close approval modal"
          style={{
            position: 'absolute',
            top: '20px',
            right: '20px',
            background: 'none',
            border: 'none',
            color: 'var(--text-muted)',
            cursor: 'pointer',
          }}
        >
          <X size={20} />
        </button>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
          <div
            style={{
              width: '42px',
              height: '42px',
              borderRadius: '50%',
              background: 'rgba(16, 185, 129, 0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#34d399',
            }}
          >
            <ShieldCheck size={24} />
          </div>
          <div>
            <h3 id="approval-modal-title" style={{ fontSize: '1.25rem', fontWeight: 800, margin: 0, color: 'var(--text-primary)' }}>
              Approve Project Requirements
            </h3>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              Human-in-the-Loop Explicit Approval
            </span>
          </div>
        </div>

        {error && (
          <div
            style={{
              padding: '12px 14px',
              borderRadius: 'var(--radius-sm)',
              background: 'rgba(239, 68, 68, 0.15)',
              border: '1px solid rgba(239, 68, 68, 0.3)',
              color: '#f87171',
              fontSize: '0.85rem',
              marginBottom: '16px',
            }}
          >
            {error}
          </div>
        )}

        <div
          style={{
            padding: '14px 16px',
            borderRadius: 'var(--radius-md)',
            background: 'rgba(99, 102, 241, 0.08)',
            border: '1px solid rgba(99, 102, 241, 0.25)',
            marginBottom: '20px',
            fontSize: '0.875rem',
            color: 'var(--text-secondary)',
            lineHeight: 1.5,
          }}
        >
          You are about to approve the final requirements for <strong>{projectName}</strong>. After approval, the specification becomes the locked <strong>single source of truth</strong> for downstream document generation.
        </div>

        {/* Verification Summary Grid */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: '1fr 1fr',
            gap: '12px',
            marginBottom: '20px',
          }}
        >
          <div
            style={{
              padding: '12px',
              borderRadius: 'var(--radius-sm)',
              background: 'var(--bg-secondary)',
              border: '1px solid var(--border-color)',
            }}
          >
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
              Quality Score
            </div>
            <div style={{ fontSize: '1.25rem', fontWeight: 800, color: qualityScore >= 80 ? '#34d399' : '#fbbf24' }}>
              {qualityScore} / 100
            </div>
          </div>

          <div
            style={{
              padding: '12px',
              borderRadius: 'var(--radius-sm)',
              background: 'var(--bg-secondary)',
              border: '1px solid var(--border-color)',
            }}
          >
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
              Features Defined
            </div>
            <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-primary)' }}>
              {featuresCount}
            </div>
          </div>

          <div
            style={{
              padding: '12px',
              borderRadius: 'var(--radius-sm)',
              background: 'var(--bg-secondary)',
              border: '1px solid var(--border-color)',
            }}
          >
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
              Blocking Questions
            </div>
            <div style={{ fontSize: '1.25rem', fontWeight: 800, color: blockingCount === 0 ? '#34d399' : '#f87171' }}>
              {blockingCount}
            </div>
          </div>

          <div
            style={{
              padding: '12px',
              borderRadius: 'var(--radius-sm)',
              background: 'var(--bg-secondary)',
              border: '1px solid var(--border-color)',
            }}
          >
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600 }}>
              Target State
            </div>
            <div style={{ fontSize: '1rem', fontWeight: 700, color: '#34d399', display: 'flex', alignItems: 'center', gap: '4px', marginTop: '2px' }}>
              <Lock size={14} /> APPROVED
            </div>
          </div>
        </div>

        {blockingCount > 0 && (
          <div
            style={{
              padding: '12px',
              borderRadius: 'var(--radius-sm)',
              background: 'rgba(245, 158, 11, 0.15)',
              border: '1px solid rgba(245, 158, 11, 0.3)',
              color: '#fbbf24',
              fontSize: '0.85rem',
              marginBottom: '20px',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}
          >
            <AlertTriangle size={18} style={{ flexShrink: 0 }} />
            <span>Approval is blocked because {blockingCount} critical/high clarification question(s) remain unresolved.</span>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <label
            style={{
              display: 'flex',
              alignItems: 'flex-start',
              gap: '10px',
              fontSize: '0.85rem',
              color: 'var(--text-secondary)',
              marginBottom: '24px',
              cursor: blockingCount > 0 ? 'not-allowed' : 'pointer',
            }}
          >
            <input
              type="checkbox"
              checked={confirmed}
              disabled={blockingCount > 0 || loading}
              onChange={(e) => setConfirmed(e.target.checked)}
              style={{ marginTop: '3px', cursor: 'pointer' }}
            />
            <span>
              I have reviewed the structured requirements and explicitly approve this specification as the locked single source of truth.
            </span>
          </label>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="btn btn-ghost"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={!confirmed || blockingCount > 0 || loading}
              className="btn btn-primary"
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                background: confirmed && blockingCount === 0
                  ? 'linear-gradient(135deg, #10b981 0%, #059669 100%)'
                  : 'var(--bg-tertiary)',
                color: '#fff',
                border: 'none',
              }}
            >
              <CheckCircle2 size={16} />
              <span>{loading ? 'Approving...' : 'Confirm & Approve Requirements'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
