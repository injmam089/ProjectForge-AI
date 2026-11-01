import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { 
  ArrowLeft, Edit3, Trash2, Sparkles, Shield, AlertCircle, 
  CheckCircle2, Clock, Calendar, Tag, Layers, Server, 
  HelpCircle, Send, Database, Code, Activity, RefreshCw,
  Check, Lock, ChevronRight, ShieldCheck, AlertTriangle, Plus,
  FileText, Eye, BookOpen
} from 'lucide-react';
import { projectService } from '../services/projectService';
import { authService } from '../services/authService';
import { ApprovalModal } from '../components/ApprovalModal';
import { DocumentViewerModal } from '../components/DocumentViewerModal';
import { PROJECT_TYPES, DIFFICULTY_LEVELS } from '../utils/constants';

export function ProjectDetailsPage({ initialEdit = false }) {
  const { id } = useParams();
  const navigate = useNavigate();

  const [project, setProject] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [isEditing, setIsEditing] = useState(initialEdit);
  const [saveLoading, setSaveLoading] = useState(false);
  const [prepareLoading, setPrepareLoading] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');

  // Phase 4 AI State
  const [specification, setSpecification] = useState(null);
  const [qualityScore, setQualityScore] = useState(null);
  const [openQuestions, setOpenQuestions] = useState([]);
  const [clarificationAnswers, setClarificationAnswers] = useState({});
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [isSubmittingClarifications, setIsSubmittingClarifications] = useState(false);
  const [aiError, setAiError] = useState('');
  const [activeSpecTab, setActiveSpecTab] = useState('features');

  // Phase 5 Human-in-the-Loop Review & Approval State
  const [isEditingSpec, setIsEditingSpec] = useState(false);
  const [editedSpec, setEditedSpec] = useState(null);
  const [isSavingSpec, setIsSavingSpec] = useState(false);
  const [rawJsonMode, setRawJsonMode] = useState(false);
  const [rawJsonText, setRawJsonText] = useState('');
  const [showApprovalModal, setShowApprovalModal] = useState(false);
  const [isApproving, setIsApproving] = useState(false);
  const [approvalError, setApprovalError] = useState('');

  // Phase 6 Markdown Documentation Generation State
  const [documents, setDocuments] = useState([]);
  const [isGeneratingDocs, setIsGeneratingDocs] = useState(false);
  const [selectedDoc, setSelectedDoc] = useState(null);
  const [isViewerOpen, setIsViewerOpen] = useState(false);
  const [docLoading, setDocLoading] = useState(false);

  const [formData, setFormData] = useState({
    name: '',
    description: '',
    problemStatement: '',
    targetUsers: '',
    projectType: 'WEB_APPLICATION',
    difficulty: 'INTERMEDIATE',
    preferredTechStack: '',
    specialRequirements: '',
  });

  useEffect(() => {
    if (!authService.isAuthenticated()) {
      navigate('/');
      return;
    }
    loadProject();
  }, [id]);

  const loadProject = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await projectService.getProject(id);
      setProject(data);
      setFormData({
        name: data.name || '',
        description: data.description || '',
        problemStatement: data.problemStatement || '',
        targetUsers: data.targetUsers || '',
        projectType: data.projectType || 'WEB_APPLICATION',
        difficulty: data.difficulty || 'INTERMEDIATE',
        preferredTechStack: data.preferredTechStack || '',
        specialRequirements: data.specialRequirements || '',
      });

      // Parse existing specification if present
      if (data.projectSpecification) {
        try {
          const review = await projectService.getReview(id);
          setSpecification(review.specification);
          setQualityScore(review.qualityScore);
          setOpenQuestions(review.openQuestions || []);
        } catch (revErr) {
          try {
            const parsed = typeof data.projectSpecification === 'string'
              ? JSON.parse(data.projectSpecification)
              : data.projectSpecification;
            setSpecification(parsed);
            setQualityScore(parsed.qualityScore || 80);
            if (parsed.openQuestions && Array.isArray(parsed.openQuestions)) {
              setOpenQuestions(parsed.openQuestions);
            }
          } catch (e) {
            console.error("Failed to parse stored specification", e);
          }
        }
      }
      // Load documents if already generated (Phase 6)
      if (data.status === 'GENERATED') {
        try {
          const docs = await projectService.getDocuments(id);
          setDocuments(docs || []);
        } catch (docErr) {
          console.error("Failed to load documents", docErr);
        }
      }
    } catch (err) {
      setError(err.message || 'Failed to load project details.');
    } finally {
      setLoading(false);
    }
  };

  const blockingQuestions = (openQuestions || []).filter(
    (q) => q && q.importance && (q.importance.toUpperCase() === 'CRITICAL' || q.importance.toUpperCase() === 'HIGH')
  );

  const isApproved = project && project.status === 'APPROVED';

  const canApprove =
    project &&
    !isApproved &&
    (project.status === 'REQUIREMENTS_READY' || project.status === 'UNDER_REVIEW') &&
    blockingQuestions.length === 0;

  // Phase 5: Start editing specification
  const handleStartEditingSpec = () => {
    const clone = JSON.parse(JSON.stringify(specification || {}));
    setEditedSpec(clone);
    setRawJsonText(JSON.stringify(clone, null, 2));
    setRawJsonMode(false);
    setIsEditingSpec(true);
  };

  const handleUpdateFeature = (index, field, value) => {
    setEditedSpec((prev) => {
      const next = { ...prev };
      const features = [...(next.features || [])];
      features[index] = { ...features[index], [field]: value };
      next.features = features;
      return next;
    });
  };

  const handleAddFeature = () => {
    setEditedSpec((prev) => {
      const next = { ...prev };
      const features = [...(next.features || [])];
      const newId = `FEAT-${String(features.length + 1).padStart(3, '0')}`;
      features.push({
        id: newId,
        name: 'New Feature',
        category: 'Core',
        priority: 'MEDIUM',
        description: 'Describe feature functionality and requirements.'
      });
      next.features = features;
      return next;
    });
  };

  const handleRemoveFeature = (index) => {
    setEditedSpec((prev) => {
      const next = { ...prev };
      const features = [...(next.features || [])];
      features.splice(index, 1);
      next.features = features;
      return next;
    });
  };

  // Phase 5: Save edited specification
  const handleSaveSpecification = async () => {
    let payload = editedSpec;
    if (rawJsonMode) {
      try {
        payload = JSON.parse(rawJsonText);
      } catch (parseErr) {
        setAiError(`Invalid JSON format: ${parseErr.message}`);
        return;
      }
    }
    if (!payload) return;

    setIsSavingSpec(true);
    setAiError('');
    setSuccessMessage('');
    try {
      const res = await projectService.updateReview(id, payload);
      setProject((prev) => ({ ...prev, status: res.status }));
      setSpecification(res.specification);
      setQualityScore(res.qualityScore);
      setOpenQuestions(res.openQuestions || []);
      setIsEditingSpec(false);
      setRawJsonMode(false);
      setSuccessMessage(res.message || 'Specification updated and re-evaluated successfully.');
    } catch (err) {
      setAiError(err.message || 'Failed to update specification. Please verify your edits.');
    } finally {
      setIsSavingSpec(false);
    }
  };

  // Phase 5: Approve requirements explicitly
  const handleApproveProject = async () => {
    setIsApproving(true);
    setApprovalError('');
    try {
      const res = await projectService.approveProject(id, true);
      setProject((prev) => ({ ...prev, status: res.status }));
      setSpecification(res.specification);
      setQualityScore(res.qualityScore);
      setShowApprovalModal(false);
      setSuccessMessage(res.message || 'Project requirements explicitly approved! Specification locked as single source of truth.');
    } catch (err) {
      setApprovalError(err.message || 'Failed to approve project requirements.');
    } finally {
      setIsApproving(false);
    }
  };

  // Phase 6: Generate documentation
  const handleGenerateDocs = async () => {
    setIsGeneratingDocs(true);
    setError('');
    setSuccessMessage('');
    try {
      const res = await projectService.generateDocuments(id);
      setProject((prev) => ({ ...prev, status: res.status }));
      setDocuments(res.documents || []);
      setSuccessMessage(res.message || `Successfully generated ${res.documentsCount} documentation files!`);
    } catch (err) {
      setError(err.message || 'Failed to generate documentation.');
    } finally {
      setIsGeneratingDocs(false);
    }
  };

  const handleViewDoc = async (docSummary) => {
    setSelectedDoc({ ...docSummary, content: '' });
    setIsViewerOpen(true);
    setDocLoading(true);
    try {
      const fullDoc = await projectService.getDocument(docSummary.id);
      setSelectedDoc(fullDoc);
    } catch (err) {
      setError('Failed to fetch document content: ' + err.message);
      setIsViewerOpen(false);
    } finally {
      setDocLoading(false);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleUpdate = async (e) => {
    e.preventDefault();
    setSaveLoading(true);
    setError('');
    setSuccessMessage('');
    try {
      const updated = await projectService.updateProject(id, formData);
      setProject(updated);
      setIsEditing(false);
      setSuccessMessage('Project requirements intake updated successfully!');
    } catch (err) {
      setError(err.message || 'Failed to update project.');
    } finally {
      setSaveLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm(`Are you sure you want to permanently delete "${project.name}"? This action cannot be undone.`)) {
      return;
    }
    try {
      await projectService.deleteProject(id);
      navigate('/');
    } catch (err) {
      setError(err.message || 'Failed to delete project.');
    }
  };

  const handlePrepareForAnalysis = async () => {
    setPrepareLoading(true);
    setError('');
    setSuccessMessage('');
    try {
      const updated = await projectService.prepareProjectForAnalysis(id);
      setProject(updated);
      setSuccessMessage('Project validated and marked READY_FOR_ANALYSIS!');
    } catch (err) {
      setError(err.message || 'Failed to prepare project for analysis.');
    } finally {
      setPrepareLoading(false);
    }
  };

  // Phase 4: Run AI Analysis
  const handleRunAnalysis = async () => {
    setIsAnalyzing(true);
    setAiError('');
    setSuccessMessage('');
    try {
      const res = await projectService.analyzeProject(id);
      setProject(prev => ({ ...prev, status: res.status }));
      setSpecification(res.specification);
      setQualityScore(res.qualityScore);
      setOpenQuestions(res.openQuestions || []);
      setSuccessMessage(res.message || 'AI Requirements Analysis completed successfully!');
    } catch (err) {
      setAiError(err.message || 'Failed to analyze project requirements. Please check your OpenAI API key or try again.');
    } finally {
      setIsAnalyzing(false);
    }
  };

  // Phase 4: Answer Clarification Questions
  const handleClarificationChange = (qId, value) => {
    setClarificationAnswers(prev => ({ ...prev, [qId]: value }));
  };

  const handleSubmitClarifications = async (e) => {
    e.preventDefault();
    const answers = Object.entries(clarificationAnswers)
      .filter(([_, ans]) => ans && ans.trim().length > 0)
      .map(([qId, ans]) => ({ questionId: qId, answer: ans.trim() }));

    if (answers.length === 0) {
      setAiError('Please provide an answer to at least one question before submitting.');
      return;
    }

    setIsSubmittingClarifications(true);
    setAiError('');
    setSuccessMessage('');
    try {
      const res = await projectService.submitClarifications(id, answers);
      setProject(prev => ({ ...prev, status: res.status }));
      setSpecification(res.specification);
      setQualityScore(res.qualityScore);
      setOpenQuestions(res.openQuestions || []);
      setClarificationAnswers({});
      setSuccessMessage(res.message || 'Clarification answers successfully incorporated! Requirements refined.');
    } catch (err) {
      setAiError(err.message || 'Failed to incorporate clarifications.');
    } finally {
      setIsSubmittingClarifications(false);
    }
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '60px 0' }}>
        <div className="badge badge-primary">Loading Project Details...</div>
      </div>
    );
  }

  if (error && !project) {
    return (
      <div className="card" style={{ padding: '32px', textAlign: 'center', maxWidth: '540px', margin: '40px auto' }}>
        <AlertCircle size={36} color="#f87171" style={{ margin: '0 auto 16px' }} />
        <h2 style={{ fontSize: '1.25rem', marginBottom: '8px' }}>Project Unavailable</h2>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '20px' }}>{error}</p>
        <Link to="/" className="btn btn-primary">Return to Dashboard</Link>
      </div>
    );
  }

  const canEdit = project && (project.status === 'DRAFT' || project.status === 'READY_FOR_ANALYSIS');

  return (
    <div>
      {/* Header Bar */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px', flexWrap: 'wrap', gap: '12px' }}>
        <Link to="/" className="btn btn-ghost" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <ArrowLeft size={16} />
          <span>Back to Dashboard</span>
        </Link>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
          {project.status === 'DRAFT' && (
            <button
              onClick={handlePrepareForAnalysis}
              className="btn btn-primary"
              disabled={prepareLoading}
              style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
            >
              <Sparkles size={16} />
              <span>{prepareLoading ? 'Validating...' : 'Prepare for Analysis'}</span>
            </button>
          )}

          {/* Phase 4 Analysis Trigger CTA */}
          {(project.status === 'READY_FOR_ANALYSIS' || project.status === 'DRAFT') && (
            <button
              onClick={handleRunAnalysis}
              className="btn btn-primary"
              disabled={isAnalyzing}
              style={{ display: 'flex', alignItems: 'center', gap: '6px', background: 'linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)', border: 'none' }}
            >
              <Sparkles size={16} />
              <span>{isAnalyzing ? 'Analyzing Requirements...' : 'Run AI Requirement Analysis'}</span>
            </button>
          )}

          {(project.status === 'REQUIREMENTS_READY' || project.status === 'UNDER_REVIEW') && (
            <button
              onClick={handleRunAnalysis}
              className="btn btn-ghost"
              disabled={isAnalyzing}
              style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
            >
              <RefreshCw size={14} className={isAnalyzing ? 'animate-spin' : ''} />
              <span>{isAnalyzing ? 'Re-analyzing...' : 'Re-run Analysis'}</span>
            </button>
          )}

          {/* Phase 5 Approval CTA */}
          {(project.status === 'REQUIREMENTS_READY' || project.status === 'UNDER_REVIEW') && (
            <button
              onClick={() => {
                setApprovalError('');
                setShowApprovalModal(true);
              }}
              disabled={!canApprove}
              className="btn btn-primary"
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                background: canApprove
                  ? 'linear-gradient(135deg, #10b981 0%, #059669 100%)'
                  : 'var(--bg-tertiary)',
                color: canApprove ? '#fff' : 'var(--text-muted)',
                border: 'none',
                cursor: canApprove ? 'pointer' : 'not-allowed',
              }}
              title={
                blockingQuestions.length > 0
                  ? `${blockingQuestions.length} blocking question(s) must be resolved before approval`
                  : 'Approve final requirements specification'
              }
            >
              <ShieldCheck size={16} />
              <span>
                {blockingQuestions.length > 0
                  ? `Approve (${blockingQuestions.length} Blocking)`
                  : 'Approve Requirements'}
              </span>
            </button>
          )}

          {isApproved && (
            <span
              className="badge badge-success"
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                padding: '8px 14px',
                fontSize: '0.85rem',
              }}
            >
              <Lock size={14} />
              <span>Approved & Locked</span>
            </span>
          )}

          {!isEditing ? (
            canEdit && (
              <button
                onClick={() => setIsEditing(true)}
                className="btn btn-ghost"
                style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
              >
                <Edit3 size={16} />
                <span>Edit Intake</span>
              </button>
            )
          ) : (
            <button
              onClick={() => setIsEditing(false)}
              className="btn btn-ghost"
            >
              Cancel Edit
            </button>
          )}

          <button
            onClick={handleDelete}
            className="btn btn-danger"
            style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <Trash2 size={16} />
            <span>Delete</span>
          </button>
        </div>
      </div>

      {/* Notifications */}
      {successMessage && (
        <div style={{ padding: '12px 16px', borderRadius: 'var(--radius-md)', background: 'rgba(16, 185, 129, 0.1)', border: '1px solid rgba(16, 185, 129, 0.3)', color: '#34d399', marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <CheckCircle2 size={16} />
          <span>{successMessage}</span>
        </div>
      )}

      {error && (
        <div style={{ padding: '12px 16px', borderRadius: 'var(--radius-md)', background: 'rgba(239, 68, 68, 0.1)', border: '1px solid rgba(239, 68, 68, 0.3)', color: '#f87171', marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <AlertCircle size={16} />
          <span>{error}</span>
        </div>
      )}

      {aiError && (
        <div style={{ padding: '14px 18px', borderRadius: 'var(--radius-md)', background: 'rgba(239, 68, 68, 0.15)', border: '1px solid rgba(239, 68, 68, 0.4)', color: '#fca5a5', marginBottom: '20px', display: 'flex', alignItems: 'flex-start', gap: '10px' }}>
          <AlertCircle size={18} style={{ marginTop: '2px', flexShrink: 0 }} />
          <div>
            <strong>AI Analysis Error:</strong> {aiError}
          </div>
        </div>
      )}

      {/* Analyzing In-Progress Banner */}
      {isAnalyzing && (
        <div className="card" style={{ padding: '24px', marginBottom: '24px', background: 'rgba(99, 102, 241, 0.08)', borderColor: 'rgba(99, 102, 241, 0.3)', textAlign: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '12px', marginBottom: '8px' }}>
            <Sparkles size={20} className="animate-spin" color="#818cf8" />
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#c7d2fe', margin: 0 }}>
              AI Requirement Analysis in Progress...
            </h3>
          </div>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', maxWidth: '600px', margin: '0 auto' }}>
            The AI Requirement Analyzer is decomposing your project intake into prioritized features, domain modules, database schemas, and REST APIs.
          </p>
        </div>
      )}

      {/* Phase 6 Generating In-Progress Banner */}
      {project.status === 'GENERATING' && (
        <div className="card" style={{ padding: '24px', marginBottom: '24px', background: 'rgba(99, 102, 241, 0.08)', borderColor: 'rgba(99, 102, 241, 0.3)', textAlign: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '12px', marginBottom: '8px' }}>
            <RefreshCw size={20} className="spin" color="#818cf8" />
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#c7d2fe', margin: 0 }}>
              Generating Consistent Markdown Documentation...
            </h3>
          </div>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', maxWidth: '600px', margin: '0 auto' }}>
            Planning document set, generating structured Markdown from the approved specification, and verifying cross-document consistency.
          </p>
        </div>
      )}

      {/* Phase 5/6 Approved State Banner */}
      {isApproved && (
        <div className="card" style={{ padding: '20px 24px', marginBottom: '24px', background: 'rgba(16, 185, 129, 0.1)', borderColor: 'rgba(16, 185, 129, 0.3)', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
            <div style={{ background: 'rgba(16, 185, 129, 0.2)', padding: '10px', borderRadius: '50%', color: '#34d399' }}>
              <CheckCircle2 size={24} />
            </div>
            <div>
              <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#34d399', margin: '0 0 4px 0' }}>
                Requirements Approved — Single Source of Truth Locked
              </h3>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', margin: 0 }}>
                This specification has been human-reviewed and explicitly approved. Ready to generate the complete Markdown documentation package.
              </p>
            </div>
          </div>
          <button
            onClick={handleGenerateDocs}
            disabled={isGeneratingDocs}
            className="btn btn-primary"
            style={{ display: 'flex', alignItems: 'center', gap: '8px', background: 'linear-gradient(135deg, #6366f1 0%, #4f46e5 100%)', border: 'none', color: '#fff', padding: '10px 20px', fontWeight: 700 }}
          >
            <Sparkles size={16} />
            <span>{isGeneratingDocs ? 'Generating Documentation...' : 'Generate Documentation'}</span>
          </button>
        </div>
      )}

      {/* Phase 6 Generated State Banner */}
      {project.status === 'GENERATED' && (
        <div className="card" style={{ padding: '20px 24px', marginBottom: '24px', background: 'rgba(99, 102, 241, 0.1)', borderColor: 'rgba(99, 102, 241, 0.3)', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
            <div style={{ background: 'rgba(99, 102, 241, 0.2)', padding: '10px', borderRadius: '50%', color: '#818cf8' }}>
              <BookOpen size={24} />
            </div>
            <div>
              <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#c7d2fe', margin: '0 0 4px 0' }}>
                Documentation Generated ({documents.length} Files Ready)
              </h3>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', margin: 0 }}>
                All standard and specialized Markdown documents have been verified and generated from the approved specification.
              </p>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Link
              to={`/projects/${id}/workspace`}
              className="btn btn-primary"
              style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '8px 16px', textDecoration: 'none' }}
            >
              <Edit3 size={15} />
              <span>Open Markdown Workspace</span>
            </Link>
            <button
              onClick={handleGenerateDocs}
              disabled={isGeneratingDocs}
              className="btn btn-ghost"
              style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
            >
              <RefreshCw size={14} className={isGeneratingDocs ? 'spin' : ''} />
              <span>{isGeneratingDocs ? 'Regenerating...' : 'Regenerate Documentation'}</span>
            </button>
          </div>
        </div>
      )}

      {/* Phase 5 Under Review Banner */}
      {project.status === 'UNDER_REVIEW' && (
        <div className="card" style={{ padding: '18px 22px', marginBottom: '24px', background: 'rgba(245, 158, 11, 0.08)', borderColor: 'rgba(245, 158, 11, 0.3)', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '14px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <AlertTriangle size={22} color="#f59e0b" style={{ flexShrink: 0 }} />
            <div>
              <h4 style={{ fontSize: '0.975rem', fontWeight: 700, color: '#fbbf24', margin: '0 0 2px 0' }}>
                Human-in-the-Loop Review: Action Required
              </h4>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', margin: 0 }}>
                There {blockingQuestions.length === 1 ? 'is' : 'are'} {blockingQuestions.length} unresolved blocking architectural question(s). Answer questions below or edit the specification before approving.
              </p>
            </div>
          </div>
          <span className="badge badge-warning" style={{ fontSize: '0.75rem' }}>
            Approval Blocked
          </span>
        </div>
      )}

      {/* Phase 5 Requirements Ready Banner */}
      {project.status === 'REQUIREMENTS_READY' && (
        <div className="card" style={{ padding: '18px 22px', marginBottom: '24px', background: 'rgba(99, 102, 241, 0.08)', borderColor: 'rgba(99, 102, 241, 0.3)', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '14px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <Sparkles size={22} color="#818cf8" style={{ flexShrink: 0 }} />
            <div>
              <h4 style={{ fontSize: '0.975rem', fontWeight: 700, color: '#c7d2fe', margin: '0 0 2px 0' }}>
                Specification Ready for Human Review & Approval
              </h4>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', margin: 0 }}>
                Review the requirements, edit any section if needed, and explicitly approve when satisfied.
              </p>
            </div>
          </div>
          <button
            onClick={() => {
              setApprovalError('');
              setShowApprovalModal(true);
            }}
            className="btn btn-primary"
            style={{ display: 'flex', alignItems: 'center', gap: '6px', background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)', border: 'none', color: '#fff' }}
          >
            <ShieldCheck size={16} />
            <span>Approve Requirements</span>
          </button>
        </div>
      )}

      {/* Main Content */}
      {isEditing ? (
        /* Edit Form */
        <div className="card" style={{ padding: '28px' }}>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '20px' }}>
            Edit Requirements Intake
          </h2>
          <form onSubmit={handleUpdate}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '18px', marginBottom: '18px' }}>
              <div>
                <label className="form-label">Project Name *</label>
                <input
                  type="text"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  className="form-input"
                  required
                />
              </div>

              <div>
                <label className="form-label">Project Type *</label>
                <select
                  name="projectType"
                  value={formData.projectType}
                  onChange={handleInputChange}
                  className="form-input"
                >
                  {PROJECT_TYPES.map((t) => (
                    <option key={t.value} value={t.value}>{t.label}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="form-label">Architectural Difficulty *</label>
                <select
                  name="difficulty"
                  value={formData.difficulty}
                  onChange={handleInputChange}
                  className="form-input"
                >
                  {DIFFICULTY_LEVELS.map((d) => (
                    <option key={d.value} value={d.value}>{d.label}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="form-label">Target Users</label>
                <input
                  type="text"
                  name="targetUsers"
                  value={formData.targetUsers}
                  onChange={handleInputChange}
                  className="form-input"
                  placeholder="e.g. SREs, Product Managers"
                />
              </div>
            </div>

            <div style={{ marginBottom: '18px' }}>
              <label className="form-label">Project Description *</label>
              <textarea
                name="description"
                value={formData.description}
                onChange={handleInputChange}
                className="form-input"
                rows={3}
                required
              />
            </div>

            <div style={{ marginBottom: '18px' }}>
              <label className="form-label">Problem Statement</label>
              <textarea
                name="problemStatement"
                value={formData.problemStatement}
                onChange={handleInputChange}
                className="form-input"
                rows={3}
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '18px', marginBottom: '24px' }}>
              <div>
                <label className="form-label">Preferred Tech Stack</label>
                <input
                  type="text"
                  name="preferredTechStack"
                  value={formData.preferredTechStack}
                  onChange={handleInputChange}
                  className="form-input"
                  placeholder="e.g. Java 17, React, PostgreSQL"
                />
              </div>

              <div>
                <label className="form-label">Special Constraints / Requirements</label>
                <input
                  type="text"
                  name="specialRequirements"
                  value={formData.specialRequirements}
                  onChange={handleInputChange}
                  className="form-input"
                  placeholder="e.g. Sub-10ms latency, zero-trust"
                />
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
              <button
                type="button"
                onClick={() => setIsEditing(false)}
                className="btn btn-ghost"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={saveLoading}
              >
                {saveLoading ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </form>
        </div>
      ) : (
        /* View Mode */
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          
          {/* Project Overview Card */}
          <div className="card" style={{ padding: '28px' }}>
            <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '16px', flexWrap: 'wrap', gap: '12px' }}>
              <div>
                <h1 style={{ fontSize: '1.625rem', fontWeight: 800, color: 'var(--text-primary)', marginBottom: '8px' }}>
                  {project.name}
                </h1>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                  <span className={`badge ${
                    project.status === 'APPROVED' ? 'badge-success' :
                    project.status === 'REQUIREMENTS_READY' ? 'badge-success' :
                    project.status === 'READY_FOR_ANALYSIS' ? 'badge-primary' :
                    project.status === 'UNDER_REVIEW' ? 'badge-warning' :
                    project.status === 'ANALYZING' ? 'badge-warning' : 'badge-ghost'
                  }`}>
                    {project.status.replace(/_/g, ' ')}
                  </span>
                  <span className="badge badge-secondary">{project.projectType.replace(/_/g, ' ')}</span>
                  <span className="badge badge-ghost">{project.difficulty}</span>
                </div>
              </div>

              {/* Quality Score Meter */}
              {qualityScore !== null && (
                <div style={{ 
                  background: 'var(--bg-secondary)', 
                  border: '1px solid var(--border-color)', 
                  padding: '12px 18px', 
                  borderRadius: 'var(--radius-md)',
                  textAlign: 'right'
                }}>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>
                    Requirement Quality
                  </div>
                  <div style={{ 
                    fontSize: '1.5rem', 
                    fontWeight: 800, 
                    color: qualityScore >= 80 ? '#34d399' : qualityScore >= 60 ? '#fbbf24' : '#f87171' 
                  }}>
                    {qualityScore}<span style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>/100</span>
                  </div>
                  {specification?.qualityBreakdown && (
                    <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: '6px', textAlign: 'left', lineHeight: 1.4, borderTop: '1px solid var(--border-color)', paddingTop: '6px' }}>
                      <div>Completeness: {specification.qualityBreakdown.completeness}/25</div>
                      <div>Clarity: {specification.qualityBreakdown.clarity}/15</div>
                      <div>Consistency: {specification.qualityBreakdown.consistency}/15</div>
                      <div>Feasibility: {specification.qualityBreakdown.technicalFeasibility}/15</div>
                      <div>Security: {specification.qualityBreakdown.securityCoverage}/15</div>
                      <div>Testability: {specification.qualityBreakdown.testability}/15</div>
                    </div>
                  )}
                </div>
              )}
            </div>

            <p style={{ fontSize: '0.95rem', color: 'var(--text-secondary)', lineHeight: 1.6, marginBottom: '20px' }}>
              {project.description}
            </p>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '14px', paddingTop: '16px', borderTop: '1px solid var(--border-color)' }}>
              <div>
                <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '4px' }}>Target Users</div>
                <div style={{ fontSize: '0.875rem', color: 'var(--text-primary)' }}>{project.targetUsers || 'Not specified'}</div>
              </div>
              <div>
                <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '4px' }}>Preferred Stack</div>
                <div style={{ fontSize: '0.875rem', color: 'var(--text-primary)', fontFamily: 'var(--font-mono)' }}>{project.preferredTechStack || 'None specified'}</div>
              </div>
              <div>
                <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '4px' }}>Created</div>
                <div style={{ fontSize: '0.875rem', color: 'var(--text-primary)' }}>{new Date(project.createdAt).toLocaleDateString()}</div>
              </div>
            </div>
          </div>

          {/* Clarification Questions Card (If open questions exist) */}
          {openQuestions && openQuestions.length > 0 && (
            <div className="card" style={{ padding: '28px', borderLeft: '4px solid #f59e0b' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '12px' }}>
                <HelpCircle size={22} color="#f59e0b" />
                <h3 style={{ fontSize: '1.2rem', fontWeight: 700, margin: 0, color: 'var(--text-primary)' }}>
                  Open Clarification Questions ({openQuestions.length})
                </h3>
              </div>
              {project.status === 'UNDER_REVIEW' && (
                <div style={{ padding: '10px 14px', borderRadius: 'var(--radius-sm)', background: 'rgba(245, 158, 11, 0.12)', border: '1px solid rgba(245, 158, 11, 0.3)', color: '#fbbf24', fontSize: '0.825rem', marginBottom: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <AlertCircle size={15} style={{ flexShrink: 0 }} />
                  <span>
                    <strong>Project is Under Review:</strong> Critical or high architectural decisions must be clarified before requirements are finalized into REQUIREMENTS_READY.
                  </span>
                </div>
              )}
              <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '20px' }}>
                The AI identified these architectural decisions as critical to clarifying your requirements. Providing answers will enhance the specification quality.
              </p>

              <form onSubmit={handleSubmitClarifications}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '18px', marginBottom: '20px' }}>
                  {openQuestions.map((q) => (
                    <div key={q.id} style={{ padding: '16px', borderRadius: 'var(--radius-md)', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)' }}>
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px', flexWrap: 'wrap', gap: '8px' }}>
                        <span style={{ fontWeight: 700, color: 'var(--text-primary)', fontSize: '0.95rem' }}>
                          {q.id}: {q.question}
                        </span>
                        <span className={`badge ${
                          q.importance === 'CRITICAL' ? 'badge-danger' :
                          q.importance === 'HIGH' ? 'badge-warning' : 'badge-ghost'
                        }`} style={{ fontSize: '0.7rem' }}>
                          {q.importance}
                        </span>
                      </div>
                      {q.reason && (
                        <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '10px' }}>
                          <em>Rationale: {q.reason}</em>
                        </p>
                      )}
                      <textarea
                        value={clarificationAnswers[q.id] || ''}
                        onChange={(e) => handleClarificationChange(q.id, e.target.value)}
                        placeholder="Provide your domain decision or preferred approach here..."
                        className="form-input"
                        rows={2}
                        style={{ fontSize: '0.875rem' }}
                      />
                    </div>
                  ))}
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={isSubmittingClarifications}
                    style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
                  >
                    <Send size={16} />
                    <span>{isSubmittingClarifications ? 'Submitting Answers...' : 'Submit Clarifications & Refine'}</span>
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* Phase 6: Generated Documentation Package */}
          {project.status === 'GENERATED' && documents.length > 0 && (
            <div className="card" style={{ padding: '28px' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <BookOpen size={22} color="#6366f1" />
                  <h3 style={{ fontSize: '1.25rem', fontWeight: 800, margin: 0, color: 'var(--text-primary)' }}>
                    Generated Documentation Package ({documents.length} Files)
                  </h3>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <Link
                    to={`/projects/${id}/workspace`}
                    className="btn btn-primary"
                    style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.82rem', padding: '6px 12px', textDecoration: 'none' }}
                  >
                    <Edit3 size={14} />
                    <span>Open Workspace</span>
                  </Link>
                  <span className="badge badge-success" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <Check size={12} />
                    <span>Verified & Consistent</span>
                  </span>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '14px' }}>
                {documents.map((doc) => (
                  <div
                    key={doc.id}
                    style={{
                      padding: '16px',
                      borderRadius: 'var(--radius-md)',
                      background: 'var(--bg-secondary)',
                      border: '1px solid var(--border-color)',
                      display: 'flex',
                      flexDirection: 'column',
                      justifyContent: 'space-between',
                      gap: '12px',
                    }}
                  >
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '6px' }}>
                        <span style={{ fontFamily: 'var(--font-mono)', fontWeight: 700, fontSize: '0.9rem', color: 'var(--text-primary)' }}>
                          {doc.filename}
                        </span>
                        <span className="badge badge-primary" style={{ fontSize: '0.7rem' }}>
                          v{doc.version}
                        </span>
                      </div>
                      <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', margin: '0 0 8px 0', lineHeight: 1.4 }}>
                        {doc.description}
                      </p>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                        {doc.characterCount ? doc.characterCount.toLocaleString() : 0} characters
                      </div>
                    </div>

                    <button
                      type="button"
                      onClick={() => handleViewDoc(doc)}
                      className="btn btn-ghost"
                      style={{ fontSize: '0.8rem', padding: '6px 12px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px', width: '100%' }}
                    >
                      <Eye size={14} />
                      <span>View Content</span>
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Structured Specification Section */}
          {specification ? (
            <div className="card" style={{ padding: '28px' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <Layers size={22} color="#6366f1" />
                  <h3 style={{ fontSize: '1.25rem', fontWeight: 800, margin: 0, color: 'var(--text-primary)' }}>
                    Structured Project Specification
                  </h3>
                </div>
                <div>
                  {isApproved ? (
                    <span className="badge badge-success" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <Lock size={13} />
                      <span>Approved & Locked (Single Source of Truth)</span>
                    </span>
                  ) : isEditingSpec ? (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                      <button
                        type="button"
                        onClick={() => {
                          if (!rawJsonMode) {
                            setRawJsonText(JSON.stringify(editedSpec, null, 2));
                          } else {
                            try {
                              const parsed = JSON.parse(rawJsonText);
                              setEditedSpec(parsed);
                            } catch (e) {
                              // keep raw
                            }
                          }
                          setRawJsonMode(!rawJsonMode);
                        }}
                        className="btn btn-ghost"
                        style={{ fontSize: '0.8rem', padding: '6px 12px' }}
                      >
                        <Code size={14} style={{ marginRight: '4px' }} />
                        {rawJsonMode ? 'Visual Form Mode' : 'Raw JSON Mode'}
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          setIsEditingSpec(false);
                          setRawJsonMode(false);
                          setEditedSpec(null);
                        }}
                        disabled={isSavingSpec}
                        className="btn btn-ghost"
                        style={{ fontSize: '0.8rem', padding: '6px 12px' }}
                      >
                        Cancel
                      </button>
                      <button
                        type="button"
                        onClick={handleSaveSpecification}
                        disabled={isSavingSpec}
                        className="btn btn-primary"
                        style={{ fontSize: '0.8rem', padding: '6px 14px', display: 'flex', alignItems: 'center', gap: '6px' }}
                      >
                        <Check size={14} />
                        <span>{isSavingSpec ? 'Saving...' : 'Save & Re-evaluate'}</span>
                      </button>
                    </div>
                  ) : (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                      <button
                        type="button"
                        onClick={handleStartEditingSpec}
                        className="btn btn-ghost"
                        style={{ fontSize: '0.85rem', padding: '6px 14px', display: 'flex', alignItems: 'center', gap: '6px' }}
                      >
                        <Edit3 size={14} />
                        <span>Edit Specification</span>
                      </button>
                      <span className="badge badge-secondary" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <Check size={12} />
                        <span>Draft Specification</span>
                      </span>
                    </div>
                  )}
                </div>
              </div>

              {/* Raw JSON Editor when active */}
              {isEditingSpec && rawJsonMode ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', margin: 0 }}>
                    Directly modify the JSON specification below. Changes will be validated and re-evaluated by the backend.
                  </p>
                  <textarea
                    value={rawJsonText}
                    onChange={(e) => setRawJsonText(e.target.value)}
                    rows={18}
                    className="form-input"
                    style={{ fontFamily: 'var(--font-mono)', fontSize: '0.85rem', lineHeight: 1.5 }}
                  />
                </div>
              ) : (
                <>
                  {/* Specification Sub-navigation Tabs */}
                  <div style={{ display: 'flex', gap: '8px', borderBottom: '1px solid var(--border-color)', paddingBottom: '12px', marginBottom: '20px', overflowX: 'auto' }}>
                    {[
                      { id: 'features', label: 'Features' },
                      { id: 'modules', label: 'Modules' },
                      { id: 'tech', label: 'Tech Stack' },
                      { id: 'database', label: 'Database' },
                      { id: 'apis', label: 'APIs' },
                      { id: 'security', label: 'Security & Testing' },
                      { id: 'risks', label: 'Risks & Assumptions' }
                    ].map(tab => (
                      <button
                        key={tab.id}
                        type="button"
                        onClick={() => setActiveSpecTab(tab.id)}
                        className={`btn ${activeSpecTab === tab.id ? 'btn-primary' : 'btn-ghost'}`}
                        style={{ fontSize: '0.85rem', padding: '6px 14px' }}
                      >
                        {tab.label}
                      </button>
                    ))}
                  </div>

                  {/* Tab: Features */}
                  {activeSpecTab === 'features' && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                      {isEditingSpec && editedSpec ? (
                        <>
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                            <span style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
                              Editing Features ({editedSpec.features?.length || 0})
                            </span>
                            <button
                              type="button"
                              onClick={handleAddFeature}
                              className="btn btn-ghost"
                              style={{ fontSize: '0.8rem', padding: '4px 10px', display: 'flex', alignItems: 'center', gap: '4px' }}
                            >
                              <Plus size={14} /> Add Feature
                            </button>
                          </div>
                          {editedSpec.features && editedSpec.features.map((f, idx) => (
                            <div key={idx} style={{ padding: '16px', borderRadius: 'var(--radius-md)', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)', display: 'flex', flexDirection: 'column', gap: '10px' }}>
                              <div style={{ display: 'flex', gap: '10px', alignItems: 'center', flexWrap: 'wrap' }}>
                                <input
                                  type="text"
                                  value={f.name}
                                  onChange={(e) => handleUpdateFeature(idx, 'name', e.target.value)}
                                  placeholder="Feature Name"
                                  className="form-input"
                                  style={{ flex: 2, minWidth: '200px' }}
                                />
                                <input
                                  type="text"
                                  value={f.category}
                                  onChange={(e) => handleUpdateFeature(idx, 'category', e.target.value)}
                                  placeholder="Category (e.g. Core, Auth)"
                                  className="form-input"
                                  style={{ flex: 1, minWidth: '130px' }}
                                />
                                <select
                                  value={f.priority}
                                  onChange={(e) => handleUpdateFeature(idx, 'priority', e.target.value)}
                                  className="form-input"
                                  style={{ width: '130px' }}
                                >
                                  <option value="CRITICAL">CRITICAL</option>
                                  <option value="HIGH">HIGH</option>
                                  <option value="MEDIUM">MEDIUM</option>
                                  <option value="LOW">LOW</option>
                                </select>
                                <button
                                  type="button"
                                  onClick={() => handleRemoveFeature(idx)}
                                  className="btn btn-ghost"
                                  style={{ color: '#ef4444', padding: '6px' }}
                                  title="Remove feature"
                                >
                                  <Trash2 size={16} />
                                </button>
                              </div>
                              <textarea
                                value={f.description}
                                onChange={(e) => handleUpdateFeature(idx, 'description', e.target.value)}
                                placeholder="Feature description..."
                                className="form-input"
                                rows={2}
                              />
                            </div>
                          ))}
                        </>
                      ) : (
                        specification.features && Array.isArray(specification.features) ? (
                          specification.features.map(f => (
                            <div key={f.id || f.name} style={{ padding: '14px', borderRadius: 'var(--radius-md)', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)' }}>
                              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '6px' }}>
                                <span style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{f.id}: {f.name}</span>
                                <div style={{ display: 'flex', gap: '6px' }}>
                                  <span className="badge badge-primary" style={{ fontSize: '0.7rem' }}>{f.category}</span>
                                  <span className="badge badge-ghost" style={{ fontSize: '0.7rem' }}>{f.priority}</span>
                                </div>
                              </div>
                              <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', margin: 0 }}>{f.description}</p>
                            </div>
                          ))
                        ) : <p style={{ color: 'var(--text-muted)' }}>No features extracted.</p>
                      )}
                    </div>
                  )}

              {/* Tab: Modules */}
              {activeSpecTab === 'modules' && (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '14px' }}>
                  {specification.modules && Array.isArray(specification.modules) ? (
                    specification.modules.map(m => (
                      <div key={m.name} style={{ padding: '14px', borderRadius: 'var(--radius-md)', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)' }}>
                        <div style={{ fontWeight: 700, color: 'var(--text-primary)', marginBottom: '6px' }}>{m.name}</div>
                        <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '10px' }}>{m.description}</p>
                        {m.dependencies && m.dependencies.length > 0 && (
                          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                            <strong>Dependencies:</strong> {m.dependencies.join(', ')}
                          </div>
                        )}
                      </div>
                    ))
                  ) : <p style={{ color: 'var(--text-muted)' }}>No modules defined.</p>}
                </div>
              )}

              {/* Tab: Tech Stack */}
              {activeSpecTab === 'tech' && (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '14px' }}>
                  {specification.technology && typeof specification.technology === 'object' && (
                    Object.entries(specification.technology).map(([category, items]) => (
                      <div key={category} style={{ padding: '14px', borderRadius: 'var(--radius-md)', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)' }}>
                        <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '8px' }}>
                          {category}
                        </div>
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                          {Array.isArray(items) && items.map((it, idx) => (
                            <span key={idx} className="badge badge-secondary" style={{ fontFamily: 'var(--font-mono)', fontSize: '0.75rem' }}>
                              {it}
                            </span>
                          ))}
                        </div>
                      </div>
                    ))
                  )}
                </div>
              )}

              {/* Tab: Database */}
              {activeSpecTab === 'database' && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                  {specification.database && specification.database.entities && Array.isArray(specification.database.entities) ? (
                    specification.database.entities.map(e => (
                      <div key={e.name} style={{ padding: '16px', borderRadius: 'var(--radius-md)', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '10px' }}>
                          <Database size={16} color="#34d399" />
                          <span style={{ fontWeight: 700, fontFamily: 'var(--font-mono)', color: 'var(--text-primary)' }}>{e.name}</span>
                          <span className="badge badge-ghost" style={{ fontSize: '0.7rem' }}>PK: {e.primaryKey}</span>
                        </div>
                        <div style={{ fontSize: '0.8rem', fontFamily: 'var(--font-mono)', color: 'var(--text-secondary)' }}>
                          {e.fields && Array.isArray(e.fields) && e.fields.map((f, i) => (
                            <div key={i} style={{ padding: '2px 0' }}>• {f}</div>
                          ))}
                        </div>
                      </div>
                    ))
                  ) : <p style={{ color: 'var(--text-muted)' }}>No database entities defined.</p>}
                </div>
              )}

              {/* Tab: APIs */}
              {activeSpecTab === 'apis' && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  {specification.apis && Array.isArray(specification.apis) ? (
                    specification.apis.map((a, i) => (
                      <div key={i} style={{ padding: '12px 16px', borderRadius: 'var(--radius-md)', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '8px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                          <span className={`badge ${
                            a.method === 'GET' ? 'badge-primary' :
                            a.method === 'POST' ? 'badge-success' :
                            a.method === 'DELETE' ? 'badge-danger' : 'badge-warning'
                          }`} style={{ fontWeight: 800, fontSize: '0.75rem' }}>
                            {a.method}
                          </span>
                          <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.85rem', color: 'var(--text-primary)' }}>
                            {a.path}
                          </span>
                        </div>
                        <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>{a.purpose}</span>
                      </div>
                    ))
                  ) : <p style={{ color: 'var(--text-muted)' }}>No APIs defined.</p>}
                </div>
              )}

              {/* Tab: Security & Testing */}
              {activeSpecTab === 'security' && (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '16px' }}>
                  <div style={{ padding: '16px', borderRadius: 'var(--radius-md)', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)' }}>
                    <div style={{ fontWeight: 700, color: 'var(--text-primary)', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <Shield size={16} color="#6366f1" />
                      <span>Security Requirements</span>
                    </div>
                    {specification.security && specification.security.securityRequirements && (
                      <ul style={{ margin: 0, paddingLeft: '18px', fontSize: '0.85rem', color: 'var(--text-secondary)', lineHeight: 1.6 }}>
                        {specification.security.securityRequirements.map((s, i) => (
                          <li key={i}>{s}</li>
                        ))}
                      </ul>
                    )}
                  </div>

                  <div style={{ padding: '16px', borderRadius: 'var(--radius-md)', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)' }}>
                    <div style={{ fontWeight: 700, color: 'var(--text-primary)', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <Activity size={16} color="#34d399" />
                      <span>Testing Strategy</span>
                    </div>
                    {specification.testing && (
                      <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                        <div><strong>Unit:</strong> {specification.testing.unitTesting ? specification.testing.unitTesting.join(', ') : 'None'}</div>
                        <div><strong>Integration:</strong> {specification.testing.integrationTesting ? specification.testing.integrationTesting.join(', ') : 'None'}</div>
                        <div><strong>Security:</strong> {specification.testing.securityTesting ? specification.testing.securityTesting.join(', ') : 'None'}</div>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* Tab: Risks & Assumptions */}
              {activeSpecTab === 'risks' && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  {specification.risks && Array.isArray(specification.risks) && (
                    specification.risks.map((r, i) => (
                      <div key={i} style={{ padding: '14px', borderRadius: 'var(--radius-md)', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)' }}>
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '6px' }}>
                          <span style={{ fontWeight: 700, color: '#f87171', fontSize: '0.9rem' }}>• {r.risk}</span>
                          <span className="badge badge-warning" style={{ fontSize: '0.7rem' }}>{r.severity}</span>
                        </div>
                        <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                          <strong>Mitigation:</strong> {r.mitigation}
                        </div>
                      </div>
                    ))
                  )}
                </div>
              )}
                </>
              )}
            </div>
          ) : (
            /* Prompt to Analyze */
            <div
              className="card"
              style={{
                padding: '36px 24px',
                textAlign: 'center',
                background: 'rgba(99, 102, 241, 0.04)',
                border: '1px dashed rgba(99, 102, 241, 0.3)',
              }}
            >
              <div
                style={{
                  width: '48px',
                  height: '48px',
                  borderRadius: '50%',
                  background: 'rgba(99, 102, 241, 0.1)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: '#818cf8',
                  margin: '0 auto 16px',
                }}
              >
                <Sparkles size={24} />
              </div>
              <h3 style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-primary)', marginBottom: '8px' }}>
                AI Requirement Analysis Ready
              </h3>
              <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', maxWidth: '540px', margin: '0 auto 20px' }}>
                Transform your requirements intake into a structured, validated specification JSON with modules, features, database entities, and REST APIs.
              </p>
              <button
                onClick={handleRunAnalysis}
                className="btn btn-primary"
                disabled={isAnalyzing}
                style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}
              >
                <Sparkles size={16} />
                <span>{isAnalyzing ? 'Analyzing...' : 'Run AI Requirement Analysis'}</span>
              </button>
            </div>
          )}

        </div>
      )}

      {/* Phase 5: Human-in-the-Loop Explicit Approval Modal */}
      <ApprovalModal
        isOpen={showApprovalModal}
        onClose={() => setShowApprovalModal(false)}
        onConfirm={handleApproveProject}
        loading={isApproving}
        error={approvalError}
        projectName={project?.name}
        qualityScore={qualityScore || 80}
        blockingCount={blockingQuestions.length}
        featuresCount={specification?.features?.length || 0}
      />

      {/* Phase 6: Markdown Document Viewer Modal */}
      <DocumentViewerModal
        isOpen={isViewerOpen}
        onClose={() => setIsViewerOpen(false)}
        document={selectedDoc}
        loading={docLoading}
      />
    </div>
  );
}

export default ProjectDetailsPage;
