import React, { useState } from 'react';
import { Sparkles, X, Wand2, ArrowRight, AlertTriangle, Loader2 } from 'lucide-react';

const ACTIONS = [
  {
    id: 'IMPROVE',
    label: 'Improve',
    desc: 'Enhance clarity, structure, terminology, and technical precision.',
  },
  {
    id: 'EXPAND',
    label: 'Expand',
    desc: 'Add deeper technical explanations, architecture details, and edge cases.',
  },
  {
    id: 'SIMPLIFY',
    label: 'Simplify',
    desc: 'Make concise, streamlined, and direct while preserving core facts.',
  },
  {
    id: 'CUSTOM',
    label: 'Custom Instruction',
    desc: 'Provide custom AI editing directions for this document.',
  },
];

export function AiDocumentEditPanel({
  onClose,
  onApplyAiEdit,
  loading,
  error,
}) {
  const [selectedAction, setSelectedAction] = useState('IMPROVE');
  const [instruction, setInstruction] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (loading) return;
    onApplyAiEdit(selectedAction, instruction);
  };

  return (
    <aside
      style={{
        width: '340px',
        minWidth: '300px',
        maxWidth: '380px',
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
          <Sparkles size={18} color="#a855f7" />
          <span style={{ fontWeight: 700, fontSize: '0.95rem', color: '#f8fafc' }}>
            AI Document Editor
          </span>
        </div>
        <button
          type="button"
          onClick={onClose}
          className="btn btn-ghost"
          style={{ padding: '4px' }}
          title="Close AI panel"
        >
          <X size={16} />
        </button>
      </div>

      {/* Body */}
      <form
        onSubmit={handleSubmit}
        style={{
          flex: 1,
          padding: '20px',
          overflowY: 'auto',
          display: 'flex',
          flexDirection: 'column',
          gap: '16px',
        }}
      >
        {/* Specification Untouched Notice */}
        <div
          style={{
            padding: '10px 12px',
            background: 'rgba(99, 102, 241, 0.08)',
            border: '1px solid rgba(99, 102, 241, 0.25)',
            borderRadius: '6px',
            fontSize: '0.75rem',
            color: '#a5b4fc',
            lineHeight: 1.5,
          }}
        >
          AI editing generates a new version snapshot for this document. The approved Project Specification remains completely untouched.
        </div>

        {error && (
          <div
            style={{
              padding: '10px 12px',
              background: 'rgba(239, 68, 68, 0.1)',
              border: '1px solid rgba(239, 68, 68, 0.3)',
              borderRadius: '6px',
              display: 'flex',
              gap: '8px',
              alignItems: 'flex-start',
              color: '#f87171',
              fontSize: '0.8rem',
            }}
          >
            <AlertTriangle size={16} style={{ flexShrink: 0, marginTop: '2px' }} />
            <div>{error}</div>
          </div>
        )}

        {/* Action Selector */}
        <div>
          <label
            style={{
              display: 'block',
              fontSize: '0.8rem',
              fontWeight: 600,
              color: '#cbd5e1',
              marginBottom: '8px',
            }}
          >
            Select Action
          </label>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            {ACTIONS.map((action) => {
              const isSelected = selectedAction === action.id;
              return (
                <button
                  key={action.id}
                  type="button"
                  onClick={() => setSelectedAction(action.id)}
                  disabled={loading}
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'flex-start',
                    padding: '10px 12px',
                    borderRadius: '6px',
                    border: isSelected ? '1px solid #a855f7' : '1px solid #334155',
                    background: isSelected
                      ? 'rgba(168, 85, 247, 0.12)'
                      : 'var(--bg-primary, #0f172a)',
                    cursor: loading ? 'not-allowed' : 'pointer',
                    textAlign: 'left',
                    transition: 'all 0.15s ease',
                  }}
                >
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      width: '100%',
                    }}
                  >
                    <span
                      style={{
                        fontSize: '0.86rem',
                        fontWeight: 600,
                        color: isSelected ? '#e9d5ff' : '#f1f5f9',
                      }}
                    >
                      {action.label}
                    </span>
                    {isSelected && <Wand2 size={14} color="#c084fc" />}
                  </div>
                  <span
                    style={{
                      fontSize: '0.74rem',
                      color: '#94a3b8',
                      marginTop: '3px',
                      lineHeight: 1.4,
                    }}
                  >
                    {action.desc}
                  </span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Custom Instruction Input */}
        <div>
          <label
            htmlFor="custom-instruction"
            style={{
              display: 'block',
              fontSize: '0.8rem',
              fontWeight: 600,
              color: '#cbd5e1',
              marginBottom: '6px',
            }}
          >
            {selectedAction === 'CUSTOM' ? 'Instruction (Required)' : 'Additional Notes (Optional)'}
          </label>
          <textarea
            id="custom-instruction"
            rows={4}
            value={instruction}
            onChange={(e) => setInstruction(e.target.value)}
            disabled={loading}
            placeholder={
              selectedAction === 'CUSTOM'
                ? 'E.g., Add a section comparing PostgreSQL vs Redis with throughput metrics...'
                : 'Optional instructions or focus areas for the AI...'
            }
            style={{
              width: '100%',
              padding: '10px',
              borderRadius: '6px',
              border: '1px solid var(--border-color, #334155)',
              background: 'var(--bg-primary, #0f172a)',
              color: '#f8fafc',
              fontSize: '0.82rem',
              resize: 'vertical',
              outline: 'none',
              fontFamily: 'inherit',
            }}
          />
        </div>

        {/* Submit Button */}
        <div style={{ marginTop: 'auto', paddingTop: '16px' }}>
          <button
            type="submit"
            disabled={loading || (selectedAction === 'CUSTOM' && !instruction.trim())}
            className="btn btn-primary"
            style={{
              width: '100%',
              padding: '10px 16px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px',
              background: 'linear-gradient(135deg, #7c3aed, #4f46e5)',
              border: 'none',
              fontWeight: 600,
              fontSize: '0.88rem',
            }}
          >
            {loading ? (
              <>
                <Loader2 size={16} className="animate-spin" />
                <span>AI is editing document...</span>
              </>
            ) : (
              <>
                <Sparkles size={16} />
                <span>Apply AI Edit</span>
                <ArrowRight size={14} />
              </>
            )}
          </button>
        </div>
      </form>
    </aside>
  );
}
