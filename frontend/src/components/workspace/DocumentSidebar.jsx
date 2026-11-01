import React, { useState } from 'react';
import { FileText, Search, AlertCircle, ChevronLeft, ChevronRight } from 'lucide-react';

export function DocumentSidebar({
  documents = [],
  selectedDocId,
  onSelectDocument,
  hasUnsavedChanges,
  collapsed,
  onToggleCollapse,
}) {
  const [filter, setFilter] = useState('');

  const filteredDocs = documents.filter((doc) =>
    (doc.filename || '').toLowerCase().includes(filter.toLowerCase()) ||
    (doc.type || '').toLowerCase().includes(filter.toLowerCase())
  );

  if (collapsed) {
    return (
      <div
        style={{
          width: '56px',
          background: 'var(--bg-secondary, #1e293b)',
          borderRight: '1px solid var(--border-color, #334155)',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          padding: '16px 8px',
          gap: '12px',
        }}
      >
        <button
          type="button"
          onClick={onToggleCollapse}
          className="btn btn-ghost"
          style={{ padding: '8px' }}
          title="Expand sidebar"
        >
          <ChevronRight size={18} />
        </button>
        <div style={{ width: '100%', height: '1px', background: 'var(--border-color, #334155)' }} />
        <div
          style={{
            writingMode: 'vertical-rl',
            textOrientation: 'mixed',
            color: 'var(--text-muted, #94a3b8)',
            fontSize: '0.8rem',
            letterSpacing: '0.05em',
            marginTop: '8px',
          }}
        >
          {documents.length} Documents
        </div>
      </div>
    );
  }

  return (
    <aside
      style={{
        width: '280px',
        minWidth: '240px',
        maxWidth: '320px',
        background: 'var(--bg-secondary, #1e293b)',
        borderRight: '1px solid var(--border-color, #334155)',
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        userSelect: 'none',
      }}
    >
      {/* Header */}
      <div
        style={{
          padding: '14px 16px',
          borderBottom: '1px solid var(--border-color, #334155)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <FileText size={18} color="#6366f1" />
          <span style={{ fontWeight: 700, fontSize: '0.92rem', color: '#f8fafc' }}>
            Documents ({documents.length})
          </span>
        </div>
        <button
          type="button"
          onClick={onToggleCollapse}
          className="btn btn-ghost"
          style={{ padding: '4px' }}
          title="Collapse sidebar"
        >
          <ChevronLeft size={16} />
        </button>
      </div>

      {/* Search Input */}
      <div style={{ padding: '10px 14px', borderBottom: '1px solid rgba(51, 65, 85, 0.4)' }}>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            background: 'var(--bg-primary, #0f172a)',
            borderRadius: '6px',
            padding: '6px 10px',
            border: '1px solid var(--border-color, #334155)',
            gap: '8px',
          }}
        >
          <Search size={14} color="#94a3b8" />
          <input
            type="text"
            placeholder="Filter documents..."
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            style={{
              background: 'transparent',
              border: 'none',
              outline: 'none',
              color: '#f8fafc',
              fontSize: '0.82rem',
              width: '100%',
            }}
          />
        </div>
      </div>

      {/* Document List */}
      <div
        style={{
          flex: 1,
          overflowY: 'auto',
          padding: '8px',
          display: 'flex',
          flexDirection: 'column',
          gap: '4px',
        }}
      >
        {filteredDocs.length === 0 ? (
          <div style={{ padding: '24px 12px', textAlign: 'center', color: '#64748b', fontSize: '0.82rem' }}>
            No matching documents found.
          </div>
        ) : (
          filteredDocs.map((doc) => {
            const isSelected = doc.id === selectedDocId;
            const isCurrentUnsaved = isSelected && hasUnsavedChanges;

            return (
              <button
                key={doc.id}
                type="button"
                onClick={() => onSelectDocument(doc.id)}
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'flex-start',
                  width: '100%',
                  padding: '9px 12px',
                  borderRadius: '6px',
                  border: isSelected ? '1px solid #6366f1' : '1px solid transparent',
                  background: isSelected
                    ? 'rgba(99, 102, 241, 0.15)'
                    : 'transparent',
                  cursor: 'pointer',
                  textAlign: 'left',
                  transition: 'background 0.15s ease, border-color 0.15s ease',
                }}
                onMouseEnter={(e) => {
                  if (!isSelected) e.currentTarget.style.background = 'rgba(255, 255, 255, 0.04)';
                }}
                onMouseLeave={(e) => {
                  if (!isSelected) e.currentTarget.style.background = 'transparent';
                }}
              >
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    width: '100%',
                    gap: '6px',
                  }}
                >
                  <span
                    style={{
                      fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
                      fontSize: '0.84rem',
                      fontWeight: isSelected ? 700 : 500,
                      color: isSelected ? '#ffffff' : '#e2e8f0',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {doc.filename}
                  </span>
                  {isCurrentUnsaved && (
                    <span
                      title="Unsaved changes"
                      style={{
                        width: '8px',
                        height: '8px',
                        borderRadius: '50%',
                        background: '#f59e0b',
                        flexShrink: 0,
                      }}
                    />
                  )}
                </div>

                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    marginTop: '4px',
                    fontSize: '0.72rem',
                    color: '#94a3b8',
                  }}
                >
                  <span
                    style={{
                      background: 'rgba(51, 65, 85, 0.5)',
                      padding: '1px 6px',
                      borderRadius: '4px',
                      fontFamily: 'monospace',
                      color: '#a5b4fc',
                    }}
                  >
                    v{doc.version || 1}
                  </span>
                  <span>{doc.type}</span>
                </div>
              </button>
            );
          })
        )}
      </div>
    </aside>
  );
}
