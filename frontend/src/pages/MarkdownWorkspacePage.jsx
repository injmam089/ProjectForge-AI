import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  ArrowLeft,
  Save,
  Copy,
  Download,
  RotateCcw,
  Sparkles,
  History,
  Check,
  AlertTriangle,
  Loader2,
  Columns,
  Eye,
  Edit3,
  Archive,
} from 'lucide-react';
import { documentService } from '../services/documentService';
import { exportService } from '../services/exportService';
import { DocumentSidebar } from '../components/workspace/DocumentSidebar';
import { MarkdownEditor } from '../components/workspace/MarkdownEditor';
import { MarkdownPreview } from '../components/workspace/MarkdownPreview';
import { AiDocumentEditPanel } from '../components/workspace/AiDocumentEditPanel';
import { DocumentVersionHistory } from '../components/workspace/DocumentVersionHistory';
import { VersionPreviewModal } from '../components/workspace/VersionPreviewModal';
import { ExportZipModal } from '../components/workspace/ExportZipModal';

export function MarkdownWorkspacePage() {
  const { id: projectId } = useParams();
  const navigate = useNavigate();

  // Workspace & document state
  const [loading, setLoading] = useState(true);
  const [workspace, setWorkspace] = useState(null);
  const [documents, setDocuments] = useState([]);
  const [selectedDocId, setSelectedDocId] = useState(null);
  const [documentContent, setDocumentContent] = useState('');
  const [savedContent, setSavedContent] = useState('');
  const [error, setError] = useState(null);

  // View & UI state
  // viewMode: 'split' | 'edit' | 'preview'
  const [viewMode, setViewMode] = useState('split');
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [activeRightPanel, setActiveRightPanel] = useState(null); // null | 'ai' | 'history'

  // Operation status state
  const [saving, setSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [copied, setCopied] = useState(false);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiError, setAiError] = useState(null);

  // Version history state
  const [versions, setVersions] = useState([]);
  const [versionsLoading, setVersionsLoading] = useState(false);
  const [previewVersionData, setPreviewVersionData] = useState(null);

  // Modals
  const [showRegenerateModal, setShowRegenerateModal] = useState(false);
  const [regenerating, setRegenerating] = useState(false);
  const [pendingDocSwitchId, setPendingDocSwitchId] = useState(null);

  // Phase 8 ZIP Export State
  const [showExportModal, setShowExportModal] = useState(false);
  const [exportLoading, setExportLoading] = useState(false);
  const [exportStatusMessage, setExportStatusMessage] = useState('');
  const [exportError, setExportError] = useState(null);
  const [exportSuccess, setExportSuccess] = useState(false);

  const selectedDoc = documents.find((d) => d.id === selectedDocId) || null;
  const hasUnsavedChanges = Boolean(selectedDoc && documentContent !== savedContent);

  // Load workspace data
  const loadWorkspace = useCallback(async (preferredDocId = null) => {
    try {
      setLoading(true);
      setError(null);
      const data = await documentService.getWorkspace(projectId);
      setWorkspace(data);
      const docs = data.documents || [];
      setDocuments(docs);

      if (docs.length > 0) {
        let initialDoc = null;
        if (preferredDocId) {
          initialDoc = docs.find((d) => d.id === preferredDocId);
        }
        if (!initialDoc) {
          // Default to README if available, else first doc
          initialDoc = docs.find((d) => d.filename.toLowerCase().includes('readme')) || docs[0];
        }
        setSelectedDocId(initialDoc.id);
        const fullDoc = await documentService.getDocument(initialDoc.id);
        setDocumentContent(fullDoc.content || '');
        setSavedContent(fullDoc.content || '');
      }
    } catch (err) {
      setError(err.message || 'Failed to load workspace.');
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    loadWorkspace();
  }, [loadWorkspace]);

  // Window beforeunload guard for unsaved edits
  useEffect(() => {
    const handleBeforeUnload = (e) => {
      if (hasUnsavedChanges) {
        e.preventDefault();
        e.returnValue = '';
      }
    };
    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, [hasUnsavedChanges]);

  // Handle document selection with unsaved changes confirmation
  const handleSelectDocument = (docId) => {
    if (docId === selectedDocId) return;

    if (hasUnsavedChanges) {
      setPendingDocSwitchId(docId);
      return;
    }

    switchToDocument(docId);
  };

  const switchToDocument = async (docId) => {
    const targetDoc = documents.find((d) => d.id === docId);
    if (!targetDoc) return;

    setSelectedDocId(targetDoc.id);
    setSaveSuccess(false);
    setAiError(null);

    try {
      const fullDoc = await documentService.getDocument(targetDoc.id);
      setDocumentContent(fullDoc.content || '');
      setSavedContent(fullDoc.content || '');
    } catch (err) {
      setError(err.message || 'Failed to fetch document content.');
    }

    // Refresh version list if history panel is open
    if (activeRightPanel === 'history') {
      fetchVersions(targetDoc.id);
    }
  };

  // Save document (creates MANUAL_EDIT version)
  const handleSave = async () => {
    if (!selectedDoc || saving) return;

    try {
      setSaving(true);
      setError(null);
      const res = await documentService.saveDocument(selectedDoc.id, documentContent);
      setSavedContent(documentContent);
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 2500);

      // Update document list with new version
      setDocuments((prev) =>
        prev.map((d) =>
          d.id === selectedDoc.id
            ? { ...d, version: res.version, content: documentContent, characterCount: res.characterCount }
            : d
        )
      );

      if (activeRightPanel === 'history') {
        fetchVersions(selectedDoc.id);
      }
    } catch (err) {
      setError(err.message || 'Failed to save document.');
    } finally {
      setSaving(false);
    }
  };

  // Copy document content
  const handleCopy = () => {
    if (documentContent) {
      navigator.clipboard.writeText(documentContent);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  // Single-file client-side download using Blob
  const handleDownload = () => {
    if (!selectedDoc || !documentContent) return;

    const blob = new Blob([documentContent], { type: 'text/markdown;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = selectedDoc.filename || 'document.md';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  // Regenerate single document from approved specification
  const handleRegenerate = async () => {
    if (!selectedDoc || regenerating) return;

    try {
      setRegenerating(true);
      setError(null);
      const res = await documentService.regenerateDocument(selectedDoc.id);
      setDocumentContent(res.content);
      setSavedContent(res.content);
      setShowRegenerateModal(false);

      // Update document in list
      setDocuments((prev) =>
        prev.map((d) =>
          d.id === selectedDoc.id
            ? { ...d, version: res.version, content: res.content, characterCount: res.characterCount }
            : d
        )
      );

      if (activeRightPanel === 'history') {
        fetchVersions(selectedDoc.id);
      }
    } catch (err) {
      setError(err.message || 'Failed to regenerate document.');
    } finally {
      setRegenerating(false);
    }
  };

  // AI Edit
  const handleApplyAiEdit = async (action, instruction) => {
    if (!selectedDoc || aiLoading) return;

    try {
      setAiLoading(true);
      setAiError(null);
      const res = await documentService.aiEditDocument(selectedDoc.id, action, instruction);
      setDocumentContent(res.content);
      setSavedContent(res.content);

      // Update document list
      setDocuments((prev) =>
        prev.map((d) =>
          d.id === selectedDoc.id
            ? { ...d, version: res.version, content: res.content, characterCount: res.content.length }
            : d
        )
      );

      if (activeRightPanel === 'history') {
        fetchVersions(selectedDoc.id);
      }
    } catch (err) {
      setAiError(err.message || 'AI document edit failed.');
    } finally {
      setAiLoading(false);
    }
  };

  // Version History loading
  const fetchVersions = async (docId) => {
    try {
      setVersionsLoading(true);
      const data = await documentService.getVersions(docId);
      setVersions(data || []);
    } catch (err) {
      console.error('Failed to fetch versions', err);
    } finally {
      setVersionsLoading(false);
    }
  };

  const handleToggleHistory = () => {
    if (activeRightPanel === 'history') {
      setActiveRightPanel(null);
    } else {
      setActiveRightPanel('history');
      if (selectedDoc) {
        fetchVersions(selectedDoc.id);
      }
    }
  };

  const handleToggleAi = () => {
    if (activeRightPanel === 'ai') {
      setActiveRightPanel(null);
    } else {
      setActiveRightPanel('ai');
    }
  };

  const handlePreviewVersion = async (version) => {
    if (!selectedDoc) return;
    try {
      const data = await documentService.getVersion(selectedDoc.id, version);
      setPreviewVersionData(data);
    } catch (err) {
      setError(err.message || 'Failed to load version snapshot.');
    }
  };

  const handleRestoreVersion = async (version) => {
    if (!selectedDoc) return;
    try {
      setLoading(true);
      const res = await documentService.restoreVersion(selectedDoc.id, version);
      setDocumentContent(res.content);
      setSavedContent(res.content);
      setPreviewVersionData(null);

      // Update document list
      setDocuments((prev) =>
        prev.map((d) =>
          d.id === selectedDoc.id
            ? { ...d, version: res.version, content: res.content, characterCount: res.characterCount }
            : d
        )
      );

      // Refresh version list
      fetchVersions(selectedDoc.id);
    } catch (err) {
      setError(err.message || 'Failed to restore version.');
    } finally {
      setLoading(false);
    }
  };

  // Phase 8 ZIP Export Handlers
  const handleOpenExport = () => {
    setExportError(null);
    setExportSuccess(false);
    setExportStatusMessage('');
    setShowExportModal(true);
  };

  const handleExecuteExport = async () => {
    if (exportLoading) return;
    try {
      setExportLoading(true);
      setExportError(null);
      setExportSuccess(false);
      setExportStatusMessage('Validating documents & manifest...');

      // 1. Prepare export metadata
      const meta = await exportService.prepareExport(projectId);

      // 2. Download binary ZIP package
      setExportStatusMessage('Packaging & downloading ZIP archive...');
      await exportService.downloadExport(projectId, meta.filename);

      setExportSuccess(true);
      setExportStatusMessage('Download complete!');
      setTimeout(() => {
        setShowExportModal(false);
        setExportSuccess(false);
        setExportLoading(false);
      }, 2000);
    } catch (err) {
      setExportError(err.message || 'Failed to export project documentation.');
      setExportLoading(false);
    }
  };

  if (loading && documents.length === 0) {
    return (
      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          height: 'calc(100vh - 120px)',
          gap: '16px',
        }}
      >
        <Loader2 size={32} className="animate-spin" color="#6366f1" />
        <div style={{ color: 'var(--text-muted)' }}>Loading Markdown Workspace...</div>
      </div>
    );
  }

  if (error && documents.length === 0) {
    return (
      <div style={{ maxWidth: '800px', margin: '40px auto', padding: '0 20px' }}>
        <div
          className="card"
          style={{
            border: '1px solid rgba(239, 68, 68, 0.4)',
            background: 'rgba(239, 68, 68, 0.08)',
            padding: '24px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', color: '#f87171' }}>
            <AlertTriangle size={24} />
            <span style={{ fontWeight: 700, fontSize: '1.1rem' }}>Workspace Error</span>
          </div>
          <p style={{ marginTop: '12px', color: '#cbd5e1', fontSize: '0.9rem' }}>{error}</p>
          <div style={{ marginTop: '20px' }}>
            <Link to={`/projects/${projectId}`} className="btn btn-primary" style={{ display: 'inline-flex', gap: '8px', alignItems: 'center' }}>
              <ArrowLeft size={16} /> Back to Project
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        height: 'calc(100vh - 110px)',
        background: 'var(--bg-primary, #0f172a)',
        margin: '-24px -32px',
        overflow: 'hidden',
      }}
    >
      {/* Top Workspace Toolbar */}
      <header
        style={{
          height: '56px',
          background: 'var(--bg-secondary, #1e293b)',
          borderBottom: '1px solid var(--border-color, #334155)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0 16px',
          zIndex: 20,
        }}
      >
        {/* Left: Project & Document Navigation */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <button
            type="button"
            onClick={() => {
              if (hasUnsavedChanges) {
                if (window.confirm('You have unsaved changes. Leave workspace anyway?')) {
                  navigate(`/projects/${projectId}`);
                }
              } else {
                navigate(`/projects/${projectId}`);
              }
            }}
            className="btn btn-ghost"
            style={{ padding: '6px 10px', display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.82rem' }}
            title="Return to project overview"
          >
            <ArrowLeft size={16} />
            <span>Project</span>
          </button>

          <div style={{ height: '20px', width: '1px', background: 'var(--border-color, #334155)' }} />

          {selectedDoc && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span
                style={{
                  fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
                  fontWeight: 700,
                  fontSize: '0.92rem',
                  color: '#f8fafc',
                }}
              >
                {selectedDoc.filename}
              </span>

              <span
                style={{
                  fontSize: '0.72rem',
                  padding: '1px 6px',
                  borderRadius: '4px',
                  background: 'rgba(99, 102, 241, 0.15)',
                  color: '#a5b4fc',
                  fontFamily: 'monospace',
                }}
              >
                v{selectedDoc.version || 1}
              </span>

              {hasUnsavedChanges && (
                <span
                  style={{
                    fontSize: '0.72rem',
                    padding: '1px 6px',
                    borderRadius: '4px',
                    background: 'rgba(245, 158, 11, 0.15)',
                    color: '#fbbf24',
                    fontWeight: 600,
                  }}
                >
                  Unsaved
                </span>
              )}

              {saveSuccess && (
                <span
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px',
                    fontSize: '0.72rem',
                    padding: '1px 6px',
                    borderRadius: '4px',
                    background: 'rgba(16, 185, 129, 0.15)',
                    color: '#34d399',
                    fontWeight: 600,
                  }}
                >
                  <Check size={12} /> Saved
                </span>
              )}
            </div>
          )}
        </div>

        {/* Center: View Mode Switcher */}
        <div
          style={{
            display: 'flex',
            background: 'var(--bg-primary, #0f172a)',
            padding: '2px',
            borderRadius: '6px',
            border: '1px solid var(--border-color, #334155)',
            gap: '2px',
          }}
        >
          <button
            type="button"
            onClick={() => setViewMode('edit')}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              padding: '4px 10px',
              borderRadius: '4px',
              border: 'none',
              background: viewMode === 'edit' ? '#6366f1' : 'transparent',
              color: viewMode === 'edit' ? '#ffffff' : '#94a3b8',
              fontSize: '0.78rem',
              fontWeight: 600,
              cursor: 'pointer',
              transition: 'background 0.15s ease',
            }}
            title="Full Edit view"
          >
            <Edit3 size={13} />
            <span>Edit</span>
          </button>

          <button
            type="button"
            onClick={() => setViewMode('split')}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              padding: '4px 10px',
              borderRadius: '4px',
              border: 'none',
              background: viewMode === 'split' ? '#6366f1' : 'transparent',
              color: viewMode === 'split' ? '#ffffff' : '#94a3b8',
              fontSize: '0.78rem',
              fontWeight: 600,
              cursor: 'pointer',
              transition: 'background 0.15s ease',
            }}
            title="Split Editor & Preview"
          >
            <Columns size={13} />
            <span>Split</span>
          </button>

          <button
            type="button"
            onClick={() => setViewMode('preview')}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              padding: '4px 10px',
              borderRadius: '4px',
              border: 'none',
              background: viewMode === 'preview' ? '#6366f1' : 'transparent',
              color: viewMode === 'preview' ? '#ffffff' : '#94a3b8',
              fontSize: '0.78rem',
              fontWeight: 600,
              cursor: 'pointer',
              transition: 'background 0.15s ease',
            }}
            title="Rendered Preview view"
          >
            <Eye size={13} />
            <span>Preview</span>
          </button>
        </div>

        {/* Right: Actions */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          {/* Save Button */}
          <button
            type="button"
            onClick={handleSave}
            disabled={saving || !hasUnsavedChanges}
            className="btn btn-primary"
            style={{
              fontSize: '0.78rem',
              padding: '6px 12px',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              opacity: !hasUnsavedChanges ? 0.6 : 1,
            }}
            title="Save changes (Ctrl+S)"
          >
            {saving ? <Loader2 size={13} className="animate-spin" /> : <Save size={13} />}
            <span>{saving ? 'Saving...' : 'Save'}</span>
          </button>

          {/* Copy Button */}
          <button
            type="button"
            onClick={handleCopy}
            className="btn btn-ghost"
            style={{ fontSize: '0.78rem', padding: '6px 10px', display: 'flex', alignItems: 'center', gap: '6px' }}
            title="Copy document content"
          >
            {copied ? <Check size={13} color="#10b981" /> : <Copy size={13} />}
            <span>{copied ? 'Copied' : 'Copy'}</span>
          </button>

          {/* Download Single File */}
          <button
            type="button"
            onClick={handleDownload}
            className="btn btn-ghost"
            style={{ fontSize: '0.78rem', padding: '6px 10px', display: 'flex', alignItems: 'center', gap: '6px' }}
            title="Download this markdown file"
          >
            <Download size={13} />
            <span>Download</span>
          </button>

          {/* Export Project as ZIP */}
          <button
            type="button"
            onClick={handleOpenExport}
            className="btn btn-ghost"
            style={{
              fontSize: '0.78rem',
              padding: '6px 10px',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              color: '#38bdf8',
            }}
            title="Export all project documentation as a secure ZIP package"
          >
            <Archive size={13} />
            <span>Export ZIP</span>
          </button>

          {/* Regenerate from Specification */}
          <button
            type="button"
            onClick={() => setShowRegenerateModal(true)}
            className="btn btn-ghost"
            style={{ fontSize: '0.78rem', padding: '6px 10px', display: 'flex', alignItems: 'center', gap: '6px' }}
            title="Regenerate canonically from approved specification"
          >
            <RotateCcw size={13} />
            <span>Regenerate</span>
          </button>

          <div style={{ height: '20px', width: '1px', background: 'var(--border-color, #334155)', margin: '0 4px' }} />

          {/* AI Edit Drawer Toggle */}
          <button
            type="button"
            onClick={handleToggleAi}
            style={{
              fontSize: '0.78rem',
              padding: '6px 10px',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              borderRadius: '6px',
              border: activeRightPanel === 'ai' ? '1px solid #a855f7' : '1px solid #334155',
              background: activeRightPanel === 'ai' ? 'rgba(168, 85, 247, 0.2)' : 'var(--bg-primary, #0f172a)',
              color: activeRightPanel === 'ai' ? '#e9d5ff' : '#cbd5e1',
              cursor: 'pointer',
              fontWeight: 600,
            }}
            title="AI Document Editor"
          >
            <Sparkles size={13} color="#c084fc" />
            <span>AI Edit</span>
          </button>

          {/* Version History Drawer Toggle */}
          <button
            type="button"
            onClick={handleToggleHistory}
            style={{
              fontSize: '0.78rem',
              padding: '6px 10px',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              borderRadius: '6px',
              border: activeRightPanel === 'history' ? '1px solid #6366f1' : '1px solid #334155',
              background: activeRightPanel === 'history' ? 'rgba(99, 102, 241, 0.2)' : 'var(--bg-primary, #0f172a)',
              color: activeRightPanel === 'history' ? '#c7d2fe' : '#cbd5e1',
              cursor: 'pointer',
              fontWeight: 600,
            }}
            title="Document Version History"
          >
            <History size={13} color="#818cf8" />
            <span>History</span>
          </button>
        </div>
      </header>

      {/* Main Workspace Workspace Layout */}
      <div style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>
        {/* Left: Document List Sidebar */}
        <DocumentSidebar
          documents={documents}
          selectedDocId={selectedDocId}
          onSelectDocument={handleSelectDocument}
          hasUnsavedChanges={hasUnsavedChanges}
          collapsed={sidebarCollapsed}
          onToggleCollapse={() => setSidebarCollapsed(!sidebarCollapsed)}
        />

        {/* Center: Editor and/or Preview */}
        <main style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>
          {viewMode === 'edit' && (
            <div style={{ flex: 1, height: '100%' }}>
              <MarkdownEditor
                content={documentContent}
                onChange={setDocumentContent}
                onSave={handleSave}
                disabled={saving}
              />
            </div>
          )}

          {viewMode === 'preview' && (
            <div style={{ flex: 1, height: '100%' }}>
              <MarkdownPreview content={documentContent} />
            </div>
          )}

          {viewMode === 'split' && (
            <>
              {/* Left pane: Editor */}
              <div
                style={{
                  flex: 1,
                  height: '100%',
                  borderRight: '1px solid var(--border-color, #334155)',
                }}
              >
                <MarkdownEditor
                  content={documentContent}
                  onChange={setDocumentContent}
                  onSave={handleSave}
                  disabled={saving}
                />
              </div>

              {/* Right pane: Preview */}
              <div style={{ flex: 1, height: '100%' }}>
                <MarkdownPreview content={documentContent} />
              </div>
            </>
          )}
        </main>

        {/* Right Drawer: AI Edit Panel */}
        {activeRightPanel === 'ai' && (
          <AiDocumentEditPanel
            onClose={() => setActiveRightPanel(null)}
            onApplyAiEdit={handleApplyAiEdit}
            loading={aiLoading}
            error={aiError}
          />
        )}

        {/* Right Drawer: Version History */}
        {activeRightPanel === 'history' && (
          <DocumentVersionHistory
            versions={versions}
            currentVersion={selectedDoc?.version || 1}
            onClose={() => setActiveRightPanel(null)}
            onPreviewVersion={handlePreviewVersion}
            onRestoreVersion={handleRestoreVersion}
            loading={versionsLoading}
          />
        )}
      </div>

      {/* Confirmation Modal for Document Switch with Unsaved Changes */}
      {pendingDocSwitchId && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0, 0, 0, 0.75)',
            zIndex: 1200,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px',
            backdropFilter: 'blur(3px)',
          }}
        >
          <div
            className="card"
            style={{
              maxWidth: '480px',
              background: '#1e293b',
              border: '1px solid #475569',
              padding: '24px',
              display: 'flex',
              flexDirection: 'column',
              gap: '14px',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#f59e0b' }}>
              <AlertTriangle size={20} />
              <span style={{ fontWeight: 700, fontSize: '1rem' }}>Unsaved Changes</span>
            </div>
            <p style={{ fontSize: '0.86rem', color: '#cbd5e1', lineHeight: 1.5, margin: 0 }}>
              You have unsaved changes in <strong style={{ color: '#fff' }}>{selectedDoc?.filename}</strong>. Discard your changes and switch documents?
            </p>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '6px' }}>
              <button
                type="button"
                className="btn btn-ghost"
                onClick={() => setPendingDocSwitchId(null)}
                style={{ fontSize: '0.82rem' }}
              >
                Keep Editing
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => {
                  const targetId = pendingDocSwitchId;
                  setPendingDocSwitchId(null);
                  switchToDocument(targetId);
                }}
                style={{ fontSize: '0.82rem', background: '#ef4444', border: 'none' }}
              >
                Discard & Switch
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Confirmation Modal for Canonical Regeneration */}
      {showRegenerateModal && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0, 0, 0, 0.75)',
            zIndex: 1200,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px',
            backdropFilter: 'blur(3px)',
          }}
        >
          <div
            className="card"
            style={{
              maxWidth: '520px',
              background: '#1e293b',
              border: '1px solid #475569',
              padding: '24px',
              display: 'flex',
              flexDirection: 'column',
              gap: '14px',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#06b6d4' }}>
              <RotateCcw size={20} />
              <span style={{ fontWeight: 700, fontSize: '1rem' }}>
                Regenerate from Approved Specification?
              </span>
            </div>
            <p style={{ fontSize: '0.86rem', color: '#cbd5e1', lineHeight: 1.5, margin: 0 }}>
              This will re-run the canonical document generator for <strong style={{ color: '#fff' }}>{selectedDoc?.filename}</strong> against the approved project specification.
            </p>
            <p style={{ fontSize: '0.82rem', color: '#94a3b8', lineHeight: 1.5, margin: 0 }}>
              Any manual edits or AI modifications will be overwritten in the active document, but safely preserved in version history as a new snapshot.
            </p>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '6px' }}>
              <button
                type="button"
                className="btn btn-ghost"
                onClick={() => setShowRegenerateModal(false)}
                disabled={regenerating}
                style={{ fontSize: '0.82rem' }}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleRegenerate}
                disabled={regenerating}
                style={{
                  fontSize: '0.82rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  background: '#06b6d4',
                  color: '#0f172a',
                  border: 'none',
                  fontWeight: 600,
                }}
              >
                {regenerating && <Loader2 size={14} className="animate-spin" />}
                <span>{regenerating ? 'Regenerating...' : 'Regenerate Document'}</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Historical Version Preview Modal */}
      {previewVersionData && (
        <VersionPreviewModal
          isOpen={Boolean(previewVersionData)}
          onClose={() => setPreviewVersionData(null)}
          versionData={previewVersionData}
          onRestore={(v) => handleRestoreVersion(v)}
        />
      )}

      {/* Phase 8 ZIP Export Modal */}
      <ExportZipModal
        isOpen={showExportModal}
        onClose={() => setShowExportModal(false)}
        projectName={workspace?.projectName || 'Project'}
        documentsCount={documents.length}
        onConfirmExport={handleExecuteExport}
        loading={exportLoading}
        statusMessage={exportStatusMessage}
        error={exportError}
        success={exportSuccess}
      />
    </div>
  );
}
