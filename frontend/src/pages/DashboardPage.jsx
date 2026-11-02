import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Sparkles, Terminal, Shield, FileCheck, RefreshCw, Eye, 
  Clock, Trash2, UserCheck, BookOpen, Download, History, 
  ChevronLeft, ChevronRight, AlertCircle, FileText
} from 'lucide-react';
import { ProjectCreationCard } from '../components/ProjectCreationCard';
import { EmptyState } from '../components/EmptyState';
import { 
  DashboardStats, 
  ProjectFilters, 
  DeleteConfirmModal, 
  ProjectHistoryModal 
} from '../components/dashboard';
import { ExportZipModal } from '../components/workspace/ExportZipModal';
import { projectService } from '../services/projectService';
import { exportService } from '../services/exportService';
import { authService } from '../services/authService';

const PAGE_SIZE = 12;

export function DashboardPage() {
  const navigate = useNavigate();
  const [projects, setProjects] = useState([]);
  const [stats, setStats] = useState({
    totalProjects: 0,
    draftProjects: 0,
    underReviewProjects: 0,
    approvedProjects: 0,
    generatedProjects: 0,
    exportedProjects: 0,
  });
  const [loading, setLoading] = useState(false);
  const [statsLoading, setStatsLoading] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [isAuthenticated, setIsAuthenticated] = useState(authService.isAuthenticated());

  // Pagination state
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // Filters state
  const [filters, setFilters] = useState({
    search: '',
    status: 'ALL',
    type: 'ALL',
    difficulty: 'ALL',
    sort: 'updatedAt',
    direction: 'DESC',
  });

  // Modal states
  const [historyModalProject, setHistoryModalProject] = useState(null);
  const [deleteModalProject, setDeleteModalProject] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [actionLoadingId, setActionLoadingId] = useState(null);

  // Export ZIP modal state
  const [exportModalProject, setExportModalProject] = useState(null);
  const [exportLoading, setExportLoading] = useState(false);
  const [exportStatusMessage, setExportStatusMessage] = useState('');
  const [exportError, setExportError] = useState(null);
  const [exportSuccess, setExportSuccess] = useState(false);

  const loadStats = useCallback(async () => {
    if (!authService.isAuthenticated()) return;
    setStatsLoading(true);
    try {
      const data = await projectService.getProjectStats();
      if (data) {
        setStats(data);
      }
    } catch (err) {
      console.error('Failed to load project stats:', err);
    } finally {
      setStatsLoading(false);
    }
  }, []);

  const loadProjects = useCallback(async (targetPage = page, currentFilters = filters) => {
    if (!authService.isAuthenticated()) {
      setProjects([]);
      return;
    }

    setLoading(true);
    setError('');
    try {
      const data = await projectService.getProjects({
        page: targetPage,
        size: PAGE_SIZE,
        search: currentFilters.search,
        status: currentFilters.status,
        type: currentFilters.type,
        difficulty: currentFilters.difficulty,
        sort: currentFilters.sort,
        direction: currentFilters.direction,
      });

      if (data && Array.isArray(data.content)) {
        setProjects(data.content);
        setPage(data.page || 0);
        setTotalPages(data.totalPages || 1);
        setTotalElements(data.totalElements || 0);
      } else if (Array.isArray(data)) {
        setProjects(data);
        setTotalPages(1);
        setTotalElements(data.length);
      } else {
        setProjects([]);
      }
    } catch (err) {
      console.error('Failed to load projects:', err);
      setError(err.message || 'Failed to load projects. Please try again.');
      setProjects([]);
    } finally {
      setLoading(false);
    }
  }, [page, filters]);

  useEffect(() => {
    if (isAuthenticated) {
      loadStats();
      loadProjects(page, filters);
    }

    const handleAuthChange = () => {
      const authed = authService.isAuthenticated();
      setIsAuthenticated(authed);
      if (authed) {
        loadStats();
        loadProjects(0, filters);
      } else {
        setProjects([]);
      }
    };

    window.addEventListener('auth-change', handleAuthChange);
    return () => {
      window.removeEventListener('auth-change', handleAuthChange);
    };
  }, [isAuthenticated, page, filters, loadStats, loadProjects]);

  const handleFilterChange = (key, value) => {
    const updated = { ...filters, [key]: value };
    setFilters(updated);
    setPage(0);
  };

  const handleResetFilters = () => {
    const reset = {
      search: '',
      status: 'ALL',
      type: 'ALL',
      difficulty: 'ALL',
      sort: 'updatedAt',
      direction: 'DESC',
    };
    setFilters(reset);
    setPage(0);
  };

  const hasActiveFilters = Boolean(
    filters.search ||
    filters.status !== 'ALL' ||
    filters.type !== 'ALL' ||
    filters.difficulty !== 'ALL' ||
    filters.sort !== 'updatedAt' ||
    filters.direction !== 'DESC'
  );

  const handleProjectCreated = (newProject) => {
    setMessage(`Project "${newProject.name}" created successfully.`);
    loadStats();
    loadProjects(0, filters);
  };

  const handleDeleteConfirm = async (projectId) => {
    setDeleteLoading(true);
    try {
      await projectService.deleteProject(projectId);
      setMessage(`Project deleted successfully.`);
      setDeleteModalProject(null);
      loadStats();
      loadProjects(page, filters);
    } catch (err) {
      throw err;
    } finally {
      setDeleteLoading(false);
    }
  };

  const handlePrepareForAnalysis = async (projectId, e) => {
    e.stopPropagation();
    setActionLoadingId(projectId);
    try {
      const updated = await projectService.prepareProjectForAnalysis(projectId);
      setProjects((prev) => prev.map((p) => (p.id === projectId ? { ...p, ...updated } : p)));
      setMessage(`Project "${updated.name}" validated and marked READY_FOR_ANALYSIS!`);
      loadStats();
    } catch (err) {
      alert(err.message || 'Failed to prepare project for analysis.');
    } finally {
      setActionLoadingId(null);
    }
  };

  // ZIP export handler
  const handleStartExport = (project, e) => {
    e.stopPropagation();
    setExportModalProject(project);
    setExportError(null);
    setExportSuccess(false);
    setExportStatusMessage('');
  };

  const handleConfirmExport = async (options) => {
    if (!exportModalProject) return;
    setExportLoading(true);
    setExportError(null);
    setExportStatusMessage('Validating active documentation package...');
    try {
      setExportStatusMessage('Packaging Markdown documents and metadata...');
      await exportService.downloadExport(
        exportModalProject.id, 
        `${exportModalProject.name.toLowerCase().replace(/[^a-z0-9_-]/g, '_')}_docs.zip`
      );
      setExportSuccess(true);
      setExportStatusMessage('ZIP documentation package downloaded successfully!');
      loadStats();
      loadProjects(page, filters);
      setTimeout(() => {
        setExportModalProject(null);
        setExportSuccess(false);
        setExportStatusMessage('');
      }, 2000);
    } catch (err) {
      setExportError(err.message || 'Export failed.');
    } finally {
      setExportLoading(false);
    }
  };

  return (
    <div>
      {/* Welcome Hero Section */}
      <div style={{ marginBottom: '32px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
          <span className="badge badge-primary">
            <Sparkles size={12} />
            <span>AI Requirements Architecture</span>
          </span>
          <span className="badge" style={{ background: 'var(--bg-tertiary)', color: 'var(--text-secondary)' }}>
            Phase 9: Dashboard & Project History
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
          ProjectForge AI deconstructs rough software concepts into structured specifications, asks targeted clarification questions, generates consistent Markdown packages, and tracks complete lifecycle activity.
        </p>

        {/* Feature Highlights */}
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
            <span>Multi-Tenant User Isolation</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <History size={15} style={{ color: 'var(--accent-cyan)' }} />
            <span>Full Audit Trail & History</span>
          </div>
        </div>
      </div>

      {/* Metrics Overview from /api/projects/stats */}
      <DashboardStats stats={stats} loading={statsLoading} />

      {/* Project Creation Card */}
      <div style={{ marginBottom: '32px' }}>
        <ProjectCreationCard onCreated={handleProjectCreated} />
      </div>

      {/* Notifications */}
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
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <span>{message}</span>
          <button
            onClick={() => setMessage('')}
            className="btn btn-ghost"
            style={{ padding: '2px 6px', fontSize: '0.75rem', color: '#34d399' }}
          >
            Dismiss
          </button>
        </div>
      )}

      {error && (
        <div
          style={{
            padding: '14px 18px',
            borderRadius: 'var(--radius-md)',
            background: 'rgba(239, 68, 68, 0.12)',
            border: '1px solid rgba(239, 68, 68, 0.3)',
            color: '#f87171',
            fontSize: '0.875rem',
            marginBottom: '20px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <AlertCircle size={18} />
            <span>{error}</span>
          </div>
          <button
            onClick={() => loadProjects(page, filters)}
            className="btn btn-ghost"
            style={{ padding: '4px 8px', fontSize: '0.75rem', color: '#f87171' }}
          >
            Retry
          </button>
        </div>
      )}

      {/* Search, Filter & Sort Controls */}
      {isAuthenticated && (
        <ProjectFilters
          filters={filters}
          onFilterChange={handleFilterChange}
          onResetFilters={handleResetFilters}
          hasActiveFilters={hasActiveFilters}
        />
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
          <span className="badge badge-primary">{totalElements}</span>
          {hasActiveFilters && (
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              (Filtered results)
            </span>
          )}
        </div>

        {isAuthenticated && (
          <button
            onClick={() => {
              loadStats();
              loadProjects(page, filters);
            }}
            className="btn btn-ghost"
            disabled={loading}
            style={{ fontSize: '0.8125rem', display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
            <span>Refresh</span>
          </button>
        )}
      </div>

      {/* Content Rendering: Unauthenticated, Empty, or Projects Grid */}
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
      ) : loading && projects.length === 0 ? (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
            gap: '16px',
          }}
        >
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <div
              key={i}
              className="card"
              style={{
                padding: '22px',
                height: '240px',
                background: 'var(--bg-secondary)',
                opacity: 0.5,
              }}
            >
              <div style={{ height: '20px', background: 'var(--bg-tertiary)', borderRadius: '4px', marginBottom: '16px', width: '40%' }} />
              <div style={{ height: '24px', background: 'var(--bg-tertiary)', borderRadius: '4px', marginBottom: '12px', width: '70%' }} />
              <div style={{ height: '50px', background: 'var(--bg-tertiary)', borderRadius: '4px', marginBottom: '16px' }} />
              <div style={{ height: '30px', background: 'var(--bg-tertiary)', borderRadius: '4px', marginTop: 'auto' }} />
            </div>
          ))}
        </div>
      ) : projects.length === 0 ? (
        hasActiveFilters ? (
          <div
            className="card"
            style={{
              padding: '48px 24px',
              textAlign: 'center',
              background: 'var(--bg-secondary)',
              border: '1px dashed var(--border-color)',
            }}
          >
            <AlertCircle size={32} style={{ opacity: 0.4, margin: '0 auto 12px' }} />
            <h3 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '6px' }}>
              No projects match your current filters
            </h3>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '16px' }}>
              Try adjusting your search keyword or clearing status/type filters.
            </p>
            <button
              onClick={handleResetFilters}
              className="btn btn-primary"
              style={{ margin: '0 auto' }}
            >
              Reset Filters
            </button>
          </div>
        ) : (
          <EmptyState onSelectTemplate={() => window.scrollTo({ top: 200, behavior: 'smooth' })} />
        )
      ) : (
        <>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))',
              gap: '16px',
            }}
          >
            {projects.map((p) => {
              const hasDocs = (p.documentCount && p.documentCount > 0) || p.status === 'GENERATED' || p.status === 'EXPORTED';
              return (
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
                    {/* Badges: Type, Status, Doc Count, Quality */}
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        marginBottom: '12px',
                        flexWrap: 'wrap',
                        gap: '6px',
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px', flexWrap: 'wrap' }}>
                        <span className="badge badge-primary" style={{ fontSize: '0.7rem' }}>
                          {p.projectType ? p.projectType.replace(/_/g, ' ') : 'WEB APP'}
                        </span>
                        {p.documentCount !== undefined && p.documentCount > 0 && (
                          <span
                            className="badge"
                            style={{
                              fontSize: '0.7rem',
                              background: 'rgba(6, 182, 212, 0.12)',
                              color: '#06b6d4',
                              border: '1px solid rgba(6, 182, 212, 0.3)',
                              display: 'flex',
                              alignItems: 'center',
                              gap: '4px',
                            }}
                          >
                            <FileText size={10} />
                            <span>{p.documentCount} Docs</span>
                          </span>
                        )}
                        {p.qualityScore !== undefined && p.qualityScore !== null && p.qualityScore > 0 && (
                          <span
                            className="badge"
                            style={{
                              fontSize: '0.7rem',
                              background: p.qualityScore >= 80 ? 'rgba(52, 211, 153, 0.12)' : 'rgba(245, 158, 11, 0.12)',
                              color: p.qualityScore >= 80 ? '#34d399' : '#f59e0b',
                              border: `1px solid ${p.qualityScore >= 80 ? 'rgba(52, 211, 153, 0.3)' : 'rgba(245, 158, 11, 0.3)'}`,
                            }}
                          >
                            QA {p.qualityScore}%
                          </span>
                        )}
                      </div>

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
                              : p.status === 'GENERATED'
                              ? 'rgba(6, 182, 212, 0.15)'
                              : p.status === 'EXPORTED'
                              ? 'rgba(167, 139, 250, 0.15)'
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
                              : p.status === 'GENERATED'
                              ? '#06b6d4'
                              : p.status === 'EXPORTED'
                              ? '#a78bfa'
                              : 'var(--text-secondary)',
                          border: '1px solid var(--border-color)',
                          fontWeight: 600,
                        }}
                      >
                        {p.status}
                      </span>
                    </div>

                    {/* Project Name */}
                    <h3
                      style={{
                        fontSize: '1.15rem',
                        fontWeight: 700,
                        color: 'var(--text-primary)',
                        marginBottom: '8px',
                        lineHeight: 1.3,
                      }}
                    >
                      {p.name}
                    </h3>

                    {/* Description snippet */}
                    <p
                      style={{
                        fontSize: '0.825rem',
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
                      <span className="badge" style={{ background: 'var(--bg-tertiary)', color: 'var(--text-secondary)', fontSize: '0.7rem' }}>
                        {p.difficulty}
                      </span>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <Clock size={12} />
                        <span>{p.updatedAt ? new Date(p.updatedAt).toLocaleDateString() : 'Recent'}</span>
                      </div>
                    </div>
                  </div>

                  {/* Actions Footer per Section 23 of prompt */}
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
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', flexWrap: 'wrap' }}>
                      {/* Open Project */}
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          navigate(`/projects/${p.id}`);
                        }}
                        className="btn btn-ghost"
                        style={{ padding: '4px 8px', fontSize: '0.75rem' }}
                        title="Open Project Details"
                      >
                        <Eye size={13} />
                        <span>Open</span>
                      </button>

                      {/* Open Workspace (if generated/exported or has docs) */}
                      {hasDocs && (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            navigate(`/projects/${p.id}/workspace`);
                          }}
                          className="btn btn-ghost"
                          style={{ padding: '4px 8px', fontSize: '0.75rem', color: '#38bdf8' }}
                          title="Open Markdown Workspace"
                        >
                          <BookOpen size={13} />
                          <span>Workspace</span>
                        </button>
                      )}

                      {/* Export ZIP */}
                      {hasDocs && (
                        <button
                          onClick={(e) => handleStartExport(p, e)}
                          className="btn btn-ghost"
                          style={{ padding: '4px 8px', fontSize: '0.75rem', color: '#a78bfa' }}
                          title="Export Documentation ZIP"
                        >
                          <Download size={13} />
                          <span>Export</span>
                        </button>
                      )}

                      {/* View Project Activity / History */}
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setHistoryModalProject(p);
                        }}
                        className="btn btn-ghost"
                        style={{ padding: '4px 8px', fontSize: '0.75rem' }}
                        title="View Project History"
                      >
                        <History size={13} />
                        <span>History</span>
                      </button>

                      {/* Delete Project */}
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setDeleteModalProject(p);
                        }}
                        className="btn btn-ghost"
                        style={{ padding: '4px 8px', fontSize: '0.75rem', color: '#f87171' }}
                        title="Delete Project"
                      >
                        <Trash2 size={13} />
                      </button>
                    </div>

                    {/* DRAFT CTA: Prepare for Analysis */}
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
              );
            })}
          </div>

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                paddingTop: '24px',
                marginTop: '16px',
                borderTop: '1px solid var(--border-color)',
                flexWrap: 'wrap',
                gap: '12px',
              }}
            >
              <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                Showing {page * PAGE_SIZE + 1}–{Math.min((page + 1) * PAGE_SIZE, totalElements)} of {totalElements} projects
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <button
                  onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                  disabled={page <= 0 || loading}
                  className="btn btn-ghost"
                  style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.8125rem' }}
                >
                  <ChevronLeft size={16} />
                  <span>Previous</span>
                </button>

                <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', padding: '0 8px' }}>
                  Page {page + 1} of {totalPages}
                </span>

                <button
                  onClick={() => setPage((prev) => Math.min(totalPages - 1, prev + 1))}
                  disabled={page >= totalPages - 1 || loading}
                  className="btn btn-ghost"
                  style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.8125rem' }}
                >
                  <span>Next</span>
                  <ChevronRight size={16} />
                </button>
              </div>
            </div>
          )}
        </>
      )}

      {/* Phase 9 Modals */}

      {/* History Timeline Modal */}
      {historyModalProject && (
        <ProjectHistoryModal
          isOpen={Boolean(historyModalProject)}
          onClose={() => setHistoryModalProject(null)}
          projectId={historyModalProject.id}
          projectName={historyModalProject.name}
        />
      )}

      {/* Safe Delete Cascade Confirmation Modal */}
      {deleteModalProject && (
        <DeleteConfirmModal
          isOpen={Boolean(deleteModalProject)}
          onClose={() => setDeleteModalProject(null)}
          onConfirm={handleDeleteConfirm}
          project={deleteModalProject}
          loading={deleteLoading}
        />
      )}

      {/* ZIP Export Modal */}
      {exportModalProject && (
        <ExportZipModal
          isOpen={Boolean(exportModalProject)}
          onClose={() => {
            if (!exportLoading) setExportModalProject(null);
          }}
          projectName={exportModalProject.name}
          documentsCount={exportModalProject.documentCount || 0}
          onConfirmExport={handleConfirmExport}
          loading={exportLoading}
          statusMessage={exportStatusMessage}
          error={exportError}
          success={exportSuccess}
        />
      )}
    </div>
  );
}

export default DashboardPage;
