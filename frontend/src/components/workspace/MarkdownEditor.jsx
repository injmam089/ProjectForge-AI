import React, { useEffect, useRef } from 'react';

export function MarkdownEditor({
  content,
  onChange,
  onSave,
  disabled,
}) {
  const textareaRef = useRef(null);

  // Handle Ctrl+S / Cmd+S save shortcut
  useEffect(() => {
    const handleKeyDown = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 's') {
        e.preventDefault();
        if (!disabled && onSave) {
          onSave();
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [disabled, onSave]);

  const lineCount = (content || '').split('\n').length;
  const charCount = (content || '').length;

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        width: '100%',
        background: '#0a0f1d',
        position: 'relative',
      }}
    >
      {/* Editor Content Area */}
      <textarea
        ref={textareaRef}
        value={content || ''}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled}
        placeholder="Enter markdown content..."
        spellCheck="false"
        style={{
          flex: 1,
          width: '100%',
          padding: '20px 24px',
          background: 'transparent',
          color: '#f1f5f9',
          fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
          fontSize: '0.9rem',
          lineHeight: 1.65,
          border: 'none',
          outline: 'none',
          resize: 'none',
          overflowY: 'auto',
          whiteSpace: 'pre-wrap',
          wordBreak: 'break-word',
          tabSize: 2,
        }}
      />

      {/* Editor Footer Status Bar */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '6px 16px',
          background: 'var(--bg-secondary, #1e293b)',
          borderTop: '1px solid var(--border-color, #334155)',
          fontSize: '0.74rem',
          color: '#94a3b8',
          fontFamily: 'ui-monospace, monospace',
        }}
      >
        <div style={{ display: 'flex', gap: '16px' }}>
          <span>{lineCount} lines</span>
          <span>{charCount.toLocaleString()} characters</span>
        </div>
        <div style={{ color: '#64748b' }}>
          Ctrl+S to save
        </div>
      </div>
    </div>
  );
}
