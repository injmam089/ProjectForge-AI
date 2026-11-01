import React from 'react';
import { SafeMarkdownRenderer } from './SafeMarkdownRenderer';

export function MarkdownPreview({ content }) {
  const lineCount = (content || '').split('\n').length;
  const charCount = (content || '').length;

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        width: '100%',
        background: 'var(--bg-primary, #0f172a)',
        position: 'relative',
      }}
    >
      {/* Preview Content Area */}
      <div
        style={{
          flex: 1,
          padding: '24px 32px',
          overflowY: 'auto',
        }}
      >
        <SafeMarkdownRenderer content={content} />
      </div>

      {/* Preview Footer Status Bar */}
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
        <div style={{ color: '#6366f1', fontWeight: 600 }}>
          Preview Mode
        </div>
      </div>
    </div>
  );
}
