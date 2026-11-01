import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Sparkles, Terminal, Shield, FileCheck, RefreshCw, Eye, Calendar, Clock, Edit3, Trash2, ArrowRight, UserCheck } from 'lucide-react';
import { ProjectsOverview } from '../components/ProjectsOverview';
import { ProjectCreationCard } from '../components/ProjectCreationCard';
import { EmptyState } from '../components/EmptyState';
import { projectService } from '../services/projectService';
import { authService } from '../services/authService';

export function DashboardPage() {
  const navigate = useNavigate();
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(false);
  const [actionLoadingId, setActionLoadingId] = useState(null);
  const [message, setMessage] = useState('');
  const [isAuthenticated, setIsAuthenticated] = useState(authService.isAuthenticated());

  const loadProjects = useCallback(async () => {
    if (!authService.isAuthenticated()) {
      setProjects([]);
      return;
    }

    setLoading(true);
    try {
      const data = await projectService.getProjects();
      setProjects(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Failed to load projects:', err);
      setProjects([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadProjects();

    const handleAuthChange = () => {
      setIsAuthenticated(authService.isAuthenticated());
      loadProjects();
    };

    window.addEventListener('auth-change', handleAuthChange);
    return () => {
      window.removeEventListener('auth-change', handleAuthChange);
    };
  }, [loadProjects]);

  const handleProjectCreated = (newProject) => {
    setProjects((prev) => [newProject, ...prev]);
  };

  const handleDelete = async (projectId, projectName, e) => {
    e.stopPropagation();
    if (!window.confirm(`Are you sure you want to permanently delete "${projectName}"?`)) {
      return;
    }

    try {
      await projectService.deleteProject(projectId);
      setProjects((prev) => prev.filter((p) => p.id !== projectId));
      setMessage(`Project "${projectName}" deleted successfully.`);
    } catch (err) {
      alert(err.message || 'Failed to delete project.');
    }
  };

  const handlePrepareForAnalysis = async (projectId, e) => {
    e.stopPropagation();
    setActionLoadingId(projectId);
    try {
      const updated = await projectService.prepareProjectForAnalysis(projectId);
      setProjects((prev) => prev.map((p) => (p.id === projectId ? { ...p, ...updated } : p)));
      setMessage(`Project "${updated.name}" validated and marked READY_FOR_ANALYSIS!`);
    } catch (err) {
      alert(err.message || 'Failed to prepare project for analysis.');
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleSelectTemplate = () => {
    window.scrollTo({ top: 200, behavior: 'smooth' });
  };

  return (
    <div>
      {/* Welcome Hero Section */}
      <div style={{ marginBottom: '36px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
          <span className="badge badge-primary">
            <Sparkles size={12} />
            <span>AI Requirements Architecture</span>
          </span>
          <span className="badge" style={{ background: 'var(--bg-tertiary)', color: 'var(--text-secondary)' }}>
            Phase 3: Project Management & Requirements Intake
          </span>
        </div>

        <h1
          style={{
            fontSize: 'clamp(1.75rem, 3vw, 2.5rem)',
            fontWeight: 800,
            letterSpacing: '-0.03em',
            lineHeight: 1.2,
            color: 'var(--text-primary)',
            marginBottom: '12px',
          }}
        >
          Transform Software Ideas into{' '}
          <span
            style={{
              background: 'linear-gradient(135deg, #818cf8 0%, #06b6d4 100%)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
            }}
          >
            Airtight Requirements & Specs
          </span>
        </h1>

        <p
          style={{
            fontSize: '1rem',
            color: 'var(--text-secondary)',
            maxWidth: '680px',
            lineHeight: 1.6,
          }}
        >
          ProjectForge AI deconstructs rough software concepts into structured specifications, asks targeted clarification questions, and generates consistent Markdown document packages ready for immediate development.
        </p>

        {/* Feature Highlights Pills */}
        <div
          style={{
            display: 'flex',
            flexWrap: 'wrap',
            gap: '16px',
            marginTop: '20px',
            fontSize: '0.8125rem',
            color: 'var(--text-muted)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <FileCheck size={15} style={{ color: 'var(--accent-emerald)' }} />
            <span>Structured Requirements Intake</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <Shield size={15} style={{ color: 'var(--primary)' }} />
            <span>Strict Server-Side Ownership</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <Terminal size={15} style={{ color: 'var(--accent-cyan)' }} />
            <span>Production-Grade PostgreSQL Model</span>
          </div>
        </div>
      </div>

      {/* Metrics Overview (6 stats per Section 18) */}
      <ProjectsOverview projects={projects} />

      {/* Project Creation Card (Section 17) */}
      <div style={{ marginBottom: '32px' }}>
        <ProjectCreationCard onCreated={handleProjectCreated} />
      </div>

      {message && (
        <div
          className="animate-fade-in"
          style={{
            padding: '12px 16px',
            borderRadius: 'var(--radius-md)',
            background: 'var(--accent-emerald-light)',
            border: '1px solid var(--accent-emerald-border)',
            color: '#34d399',
            fontSize: '0.875rem',
            marginBottom: '20px',
          }}
        >
          {message}
        </div>
      )}

      {/* Projects List Header */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: '16px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-primary)' }}>
            Your Projects
          </h2>
          <span className="badge badge-primary">{projects.length}</span>
        </div>

        {isAuthenticated && (
          <button
            onClick={loadProjects}
            className="btn btn-ghost"
            disabled={loading}
            style={{ fontSize: '0.8125rem', display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
            <span>Refresh</span>
          </button>
        )}
      </div>

      {/* Projects List, Empty State, or Sign-In Prompt */}
      {!isAuthenticated ? (
        <div
          className="card"
          style={{
            padding: '40px 24px',
            textAlign: 'center',
            background: 'var(--bg-secondary)',
            border: '1px dashed var(--border-color)',
          }}
        >
          <div
            style={{
              width: '48px',
              height: '48px',
              borderRadius: '50%',
              background: 'var(--primary-light)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--primary)',
              margin: '0 auto 16px',
            }}
          >
            <UserCheck size={24} />
          </div>
          <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '8px' }}>
            Sign In to Manage Your Projects
          </h3>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', maxWidth: '440px', margin: '0 auto 20px' }}>
            Projects in ProjectForge AI are securely tied to authenticated user accounts with strict ownership enforcement.
          </p>
          <button
            onClick={() => window.dispatchEvent(new Event('open-auth-modal'))}
            className="btn btn-primary"
            style={{ margin: '0 auto' }}
          >
            Sign In or Register
          </button>
        </div>
      ) : projects.length === 0 ? (
        <EmptyState onSelectTemplate={handleSelectTemplate} />
      ) : (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
            gap: '16px',
          }}
        >
          {projects.map((p) => (
            <div
              key={p.id}
              className="card card-interactive"
              onClick={() => navigate(`/projects/${p.id}`)}
              style={{
                padding: '22px',
                cursor: 'pointer',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
              }}
            >
              <div>
                {/* Badges: Type & Status */}
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    marginBottom: '12px',
                  }}
                >
                  <span className="badge badge-primary" style={{ fontSize: '0.7rem' }}>
                    {p.projectType ? p.projectType.replace(/_/g, ' ') : 'WEB APP'}
                  </span>
                  <span
                    className="badge"
                    style={{
                      fontSize: '0.7rem',
                      background:
                        p.status === 'READY_FOR_ANALYSIS'
                          ? 'var(--accent-amber-light)'
                          : p.status === 'UNDER_REVIEW'
                          ? 'rgba(245, 158, 11, 0.15)'
                          : p.status === 'REQUIREMENTS_READY'
                          ? 'var(--primary-light)'
                          : p.status === 'APPROVED'
                          ? 'var(--accent-emerald-light)'
                          : 'var(--bg-tertiary)',
                      color:
                        p.status === 'READY_FOR_ANALYSIS'
                          ? '#fbbf24'
                          : p.status === 'UNDER_REVIEW'
                          ? '#f59e0b'
                          : p.status === 'REQUIREMENTS_READY'
                          ? 'var(--primary)'
                          : p.status === 'APPROVED'
                          ? '#34d399'
                          : 'var(--text-secondary)',
                      border: '1px solid var(--border-color)',
                      fontWeight: 600,
                    }}
                  >
                    {p.status}
                  </span>
                </div>

                {/* Name */}
                <h3
                  style={{
                    fontSize: '1.1rem',
                    fontWeight: 700,
                    color: 'var(--text-primary)',
                    marginBottom: '8px',
                  }}
                >
                  {p.name}
                </h3>

                {/* Description snippet */}
                <p
                  style={{
                    fontSize: '0.8125rem',
                    color: 'var(--text-secondary)',
                    lineHeight: 1.5,
                    marginBottom: '16px',
                    display: '-webkit-box',
                    WebkitLineClamp: 3,
                    WebkitBoxOrient: 'vertical',
                    overflow: 'hidden',
                  }}
                >
                  {p.description || p.projectIdea || 'No description provided.'}
                </p>

                {/* Difficulty & Last Updated */}
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '14px' }}>
                  <span className="badge" style={{ background: 'var(--bg-tertiary)', color: 'var(--text-secondary)' }}>
                    {p.difficulty}
                  </span>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <Clock size={12} />
                    <span>{p.updatedAt ? new Date(p.updatedAt).toLocaleDateString() : 'Recent'}</span>
                  </div>
                </div>
              </div>

              {/* Actions Footer per Section 19: Open, Edit, Delete, Prepare for Analysis */}
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  paddingTop: '14px',
                  borderTop: '1px solid var(--border-color)',
                  flexWrap: 'wrap',
                  gap: '8px',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      navigate(`/projects/${p.id}`);
                    }}
                    className="btn btn-ghost"
                    style={{ padding: '4px 8px', fontSize: '0.75rem' }}
                    title="Open Project"
                  >
                    <Eye size={13} />
                    <span>Open</span>
                  </button>

                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      navigate(`/projects/${p.id}/edit`);
                    }}
                    className="btn btn-ghost"
                    style={{ padding: '4px 8px', fontSize: '0.75rem' }}
                    title="Edit Project"
                  >
                    <Edit3 size={13} />
                    <span>Edit</span>
                  </button>

                  <button
                    onClick={(e) => handleDelete(p.id, p.name, e)}
                    className="btn btn-ghost"
                    style={{ padding: '4px 8px', fontSize: '0.75rem', color: '#f87171' }}
                    title="Delete Project"
                  >
                    <Trash2 size={13} />
                  </button>
                </div>

                {p.status === 'DRAFT' && (
                  <button
                    onClick={(e) => handlePrepareForAnalysis(p.id, e)}
                    className="btn btn-primary"
                    disabled={actionLoadingId === p.id}
                    style={{ padding: '4px 10px', fontSize: '0.75rem', display: 'flex', alignItems: 'center', gap: '4px' }}
                  >
                    <Sparkles size={12} />
                    <span>{actionLoadingId === p.id ? 'Validating...' : 'Prepare Analysis'}</span>
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
