import React, { useState, useEffect } from 'react';
import { X, Edit2, Trash2, Save, FileCode, CheckCircle, AlertCircle, Calendar, Tag, ShieldAlert } from 'lucide-react';
import { projectService } from '../services/projectService';
import { PROJECT_TYPES, DIFFICULTY_LEVELS, PROJECT_STATES } from '../utils/constants';

export function ProjectDetailsModal({ project, isOpen, onClose, onProjectUpdated, onProjectDeleted }) {
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({});
  const [specification, setSpecification] = useState(null);
  const [specLoading, setSpecLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState('details'); // 'details' | 'specification'

  useEffect(() => {
    if (project) {
      setFormData({
        name: project.name || '',
        projectIdea: project.projectIdea || '',
        problemStatement: project.problemStatement || '',
        targetUsers: project.targetUsers || '',
        projectType: project.projectType || 'WEB_APPLICATION',
        difficulty: project.difficulty || 'INTERMEDIATE',
        preferredTechStack: project.preferredTechStack || '',
        specialRequirements: project.specialRequirements || '',
      });
      setIsEditing(false);
      setError('');
      fetchSpecification(project.id);
    }
  }, [project]);

  if (!isOpen || !project) return null;

  const fetchSpecification = async (projectId) => {
    setSpecLoading(true);
    try {
      const res = await projectService.getSpecification(projectId);
      setSpecification(res.specification);
    } catch {
      setSpecification(null);
    } finally {
      setSpecLoading(false);
    }
  };

  const handleUpdate = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');

    try {
      const updated = await projectService.updateProject(project.id, formData);
      setIsEditing(false);
      if (onProjectUpdated) onProjectUpdated(updated);
    } catch (err) {
      setError(err.message || 'Failed to update project.');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm(`Are you sure you want to permanently delete "${project.name}"? This action cannot be undone.`)) {
      return;
    }

    setDeleting(true);
    setError('');
    try {
      await projectService.deleteProject(project.id);
      onClose();
      if (onProjectDeleted) onProjectDeleted(project.id);
    } catch (err) {
      setError(err.message || 'Failed to delete project.');
    } finally {
      setDeleting(false);
    }
  };

  const statusConfig = PROJECT_STATES[project.status] || { label: project.status, color: 'badge-primary' };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.75)',
        backdropFilter: 'blur(8px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 100,
        padding: '20px',
      }}
    >
      <div
        className="card"
        style={{
          width: '100%',
          maxWidth: '720px',
          maxHeight: '85vh',
          display: 'flex',
          flexDirection: 'column',
          padding: '28px',
          background: 'var(--bg-secondary)',
          border: '1px solid var(--border-color)',
          borderRadius: 'var(--radius-lg)',
          boxShadow: 'var(--shadow-lg)',
        }}
      >
        {/* Header */}
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '16px' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '6px' }}>
              <span className={`badge ${statusConfig.color}`}>{statusConfig.label}</span>
              <span className="badge" style={{ background: 'var(--bg-tertiary)', color: 'var(--text-secondary)' }}>
                {project.projectType}
              </span>
              <span className="badge" style={{ background: 'var(--bg-tertiary)', color: 'var(--text-secondary)' }}>
                {project.difficulty}
              </span>
            </div>
            <h2 style={{ fontSize: '1.375rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              {project.name}
            </h2>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            {!isEditing && (
              <>
                <button
                  onClick={() => setIsEditing(true)}
                  className="btn btn-secondary"
                  style={{ padding: '6px 12px', fontSize: '0.8125rem' }}
                  title="Edit Project"
                >
                  <Edit2 size={14} />
                  <span>Edit</span>
                </button>
                <button
                  onClick={handleDelete}
                  disabled={deleting}
                  className="btn btn-danger"
                  style={{ padding: '6px 12px', fontSize: '0.8125rem' }}
                  title="Delete Project"
                >
                  <Trash2 size={14} />
                  <span>{deleting ? 'Deleting...' : 'Delete'}</span>
                </button>
              </>
            )}
            <button
              onClick={onClose}
              className="btn btn-ghost"
              style={{ padding: '6px', borderRadius: '50%' }}
              aria-label="Close"
            >
              <X size={18} />
            </button>
          </div>
        </div>

        {/* Tabs */}
        <div style={{ display: 'flex', gap: '8px', borderBottom: '1px solid var(--border-color)', paddingBottom: '10px', marginBottom: '16px' }}>
          <button
            onClick={() => setActiveTab('details')}
            className={`btn ${activeTab === 'details' ? 'btn-primary' : 'btn-ghost'}`}
            style={{ padding: '6px 14px', fontSize: '0.8125rem' }}
          >
            Project Overview
          </button>
          <button
            onClick={() => setActiveTab('specification')}
            className={`btn ${activeTab === 'specification' ? 'btn-primary' : 'btn-ghost'}`}
            style={{ padding: '6px 14px', fontSize: '0.8125rem' }}
          >
            <FileCode size={14} />
            <span>Project Specification</span>
          </button>
        </div>

        {/* Error Alert */}
        {error && (
          <div
            style={{
              padding: '10px 14px',
              borderRadius: 'var(--radius-md)',
              backgroundColor: 'rgba(239, 68, 68, 0.1)',
              border: '1px solid var(--accent-rose)',
              color: 'var(--accent-rose)',
              fontSize: '0.8125rem',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              marginBottom: '16px',
            }}
          >
            <AlertCircle size={16} />
            <span>{error}</span>
          </div>
        )}

        {/* Content Body (Scrollable) */}
        <div style={{ flex: 1, overflowY: 'auto', paddingRight: '4px' }}>
          {activeTab === 'details' ? (
            isEditing ? (
              <form onSubmit={handleUpdate} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 500, marginBottom: '4px' }}>Project Name</label>
                  <input
                    type="text"
                    required
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: 'var(--radius-md)', background: 'var(--bg-tertiary)', border: '1px solid var(--border-color)', color: 'var(--text-primary)' }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 500, marginBottom: '4px' }}>Project Idea & Description</label>
                  <textarea
                    required
                    rows={4}
                    value={formData.projectIdea}
                    onChange={(e) => setFormData({ ...formData, projectIdea: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: 'var(--radius-md)', background: 'var(--bg-tertiary)', border: '1px solid var(--border-color)', color: 'var(--text-primary)', resize: 'vertical' }}
                  />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 500, marginBottom: '4px' }}>Project Type</label>
                    <select
                      value={formData.projectType}
                      onChange={(e) => setFormData({ ...formData, projectType: e.target.value })}
                      style={{ width: '100%', padding: '8px 12px', borderRadius: 'var(--radius-md)', background: 'var(--bg-tertiary)', border: '1px solid var(--border-color)', color: 'var(--text-primary)' }}
                    >
                      {PROJECT_TYPES.map((t) => (
                        <option key={t.id} value={t.id}>{t.label}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 500, marginBottom: '4px' }}>Difficulty</label>
                    <select
                      value={formData.difficulty}
                      onChange={(e) => setFormData({ ...formData, difficulty: e.target.value })}
                      style={{ width: '100%', padding: '8px 12px', borderRadius: 'var(--radius-md)', background: 'var(--bg-tertiary)', border: '1px solid var(--border-color)', color: 'var(--text-primary)' }}
                    >
                      {DIFFICULTY_LEVELS.map((d) => (
                        <option key={d.id} value={d.id}>{d.label}</option>
                      ))}
                    </select>
                  </div>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 500, marginBottom: '4px' }}>Problem Statement</label>
                  <textarea
                    rows={2}
                    value={formData.problemStatement}
                    onChange={(e) => setFormData({ ...formData, problemStatement: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: 'var(--radius-md)', background: 'var(--bg-tertiary)', border: '1px solid var(--border-color)', color: 'var(--text-primary)' }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 500, marginBottom: '4px' }}>Target Audience</label>
                  <input
                    type="text"
                    value={formData.targetUsers}
                    onChange={(e) => setFormData({ ...formData, targetUsers: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: 'var(--radius-md)', background: 'var(--bg-tertiary)', border: '1px solid var(--border-color)', color: 'var(--text-primary)' }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 500, marginBottom: '4px' }}>Preferred Tech Stack</label>
                  <input
                    type="text"
                    value={formData.preferredTechStack}
                    onChange={(e) => setFormData({ ...formData, preferredTechStack: e.target.value })}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: 'var(--radius-md)', background: 'var(--bg-tertiary)', border: '1px solid var(--border-color)', color: 'var(--text-primary)' }}
                  />
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
                  <button
                    type="button"
                    onClick={() => setIsEditing(false)}
                    className="btn btn-ghost"
                    style={{ padding: '8px 14px' }}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={saving}
                    className="btn btn-primary"
                    style={{ padding: '8px 16px' }}
                  >
                    <Save size={15} />
                    <span>{saving ? 'Saving Changes...' : 'Save Changes'}</span>
                  </button>
                </div>
              </form>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                <div>
                  <h4 style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '6px' }}>
                    Concept & Core Idea
                  </h4>
                  <p style={{ fontSize: '0.9375rem', color: 'var(--text-secondary)', lineHeight: 1.6, whiteSpace: 'pre-wrap' }}>
                    {project.projectIdea}
                  </p>
                </div>

                {project.problemStatement && (
                  <div>
                    <h4 style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '6px' }}>
                      Problem Statement
                    </h4>
                    <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                      {project.problemStatement}
                    </p>
                  </div>
                )}

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '12px', background: 'var(--bg-tertiary)', padding: '14px', borderRadius: 'var(--radius-md)' }}>
                  <div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Target Users</div>
                    <div style={{ fontSize: '0.875rem', fontWeight: 500, color: 'var(--text-primary)', marginTop: '2px' }}>
                      {project.targetUsers || 'Not specified'}
                    </div>
                  </div>
                  <div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Preferred Tech Stack</div>
                    <div style={{ fontSize: '0.875rem', fontWeight: 500, color: 'var(--text-primary)', marginTop: '2px' }}>
                      {project.preferredTechStack || 'None specified'}
                    </div>
                  </div>
                  <div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Created On</div>
                    <div style={{ fontSize: '0.875rem', color: 'var(--text-primary)', marginTop: '2px' }}>
                      {new Date(project.createdAt).toLocaleDateString()}
                    </div>
                  </div>
                </div>

                {project.specialRequirements && (
                  <div>
                    <h4 style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '6px' }}>
                      Special Requirements & Constraints
                    </h4>
                    <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                      {project.specialRequirements}
                    </p>
                  </div>
                )}
              </div>
            )
          ) : (
            <div>
              {specLoading ? (
                <div style={{ textAlign: 'center', padding: '30px', color: 'var(--text-muted)' }}>
                  Loading project specification...
                </div>
              ) : specification ? (
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--accent-emerald)', fontSize: '0.8125rem', marginBottom: '10px' }}>
                    <CheckCircle size={15} />
                    <span>Project specification loaded as single source of truth</span>
                  </div>
                  <pre
                    style={{
                      background: 'var(--bg-tertiary)',
                      padding: '16px',
                      borderRadius: 'var(--radius-md)',
                      fontSize: '0.8125rem',
                      fontFamily: 'var(--font-mono)',
                      color: 'var(--text-primary)',
                      overflowX: 'auto',
                      maxHeight: '380px',
                    }}
                  >
                    {JSON.stringify(specification, null, 2)}
                  </pre>
                </div>
              ) : (
                <div
                  style={{
                    textAlign: 'center',
                    padding: '36px 20px',
                    background: 'var(--bg-tertiary)',
                    borderRadius: 'var(--radius-md)',
                    border: '1px dashed var(--border-color)',
                  }}
                >
                  <FileCode size={32} style={{ color: 'var(--text-muted)', marginBottom: '12px' }} />
                  <h4 style={{ fontSize: '0.9375rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '4px' }}>
                    No Specification Generated Yet
                  </h4>
                  <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', maxWidth: '420px', margin: '0 auto' }}>
                    This project is in <strong>DRAFT</strong> status. Once Phase 4 AI Requirement Analysis is performed, the structured JSON specification will be stored here.
                  </p>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
