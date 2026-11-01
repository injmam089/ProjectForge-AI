import React, { useState } from 'react';
import { Sparkles, ArrowRight, CheckCircle, AlertCircle, Info } from 'lucide-react';
import { DIFFICULTY_LEVELS, PROJECT_TYPES } from '../utils/constants';
import { projectService } from '../services/projectService';
import { authService } from '../services/authService';

export function ProjectCreationCard({ onCreated }) {
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

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [submittedMessage, setSubmittedMessage] = useState(null);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSubmittedMessage(null);

    if (!authService.isAuthenticated()) {
      window.dispatchEvent(new Event('open-auth-modal'));
      setError('Please sign in or create an account to save your project.');
      return;
    }

    if (formData.name.trim().length < 2) {
      setError('Project name must be at least 2 characters.');
      return;
    }

    if (formData.description.trim().length < 10) {
      setError('Project description must be at least 10 characters.');
      return;
    }

    setLoading(true);
    try {
      const payload = {
        name: formData.name.trim(),
        description: formData.description.trim(),
        problemStatement: formData.problemStatement.trim() || null,
        targetUsers: formData.targetUsers.trim() || null,
        projectType: formData.projectType,
        difficulty: formData.difficulty,
        preferredTechStack: formData.preferredTechStack.trim() || null,
        specialRequirements: formData.specialRequirements.trim() || null,
      };

      const created = await projectService.createProject(payload);

      setSubmittedMessage(`Project "${created.name}" created successfully in DRAFT status!`);
      setFormData({
        name: '',
        description: '',
        problemStatement: '',
        targetUsers: '',
        projectType: 'WEB_APPLICATION',
        difficulty: 'INTERMEDIATE',
        preferredTechStack: '',
        specialRequirements: '',
      });

      if (onCreated) {
        onCreated(created);
      }
    } catch (err) {
      setError(err.message || 'Failed to create project. Please verify input fields.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card card-interactive" style={{ padding: '28px', position: 'relative' }}>
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: '20px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div
            style={{
              width: '32px',
              height: '32px',
              borderRadius: 'var(--radius-sm)',
              background: 'var(--primary-light)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--primary)',
            }}
          >
            <Sparkles size={18} />
          </div>
          <div>
            <h3 style={{ fontSize: '1.125rem', fontWeight: 600 }}>Create New Project</h3>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
              Input structured project requirements and architectural parameters for future AI analysis.
            </p>
          </div>
        </div>

        <span className="badge badge-primary">Phase 3 Requirements Intake</span>
      </div>

      {submittedMessage && (
        <div
          className="animate-fade-in"
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            padding: '12px 16px',
            borderRadius: 'var(--radius-md)',
            background: 'var(--accent-emerald-light)',
            border: '1px solid var(--accent-emerald-border)',
            color: '#34d399',
            fontSize: '0.8125rem',
            marginBottom: '20px',
          }}
        >
          <CheckCircle size={16} />
          <span>{submittedMessage}</span>
        </div>
      )}

      {error && (
        <div
          className="animate-fade-in"
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            padding: '12px 16px',
            borderRadius: 'var(--radius-md)',
            background: 'var(--accent-rose-light)',
            border: '1px solid var(--accent-rose-border)',
            color: '#f87171',
            fontSize: '0.8125rem',
            marginBottom: '20px',
          }}
        >
          <AlertCircle size={16} />
          <span>{error}</span>
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '16px' }}>
          <div className="input-group">
            <label className="input-label" htmlFor="name">
              Project Name *
            </label>
            <input
              id="name"
              name="name"
              type="text"
              className="input-field"
              placeholder="e.g., CyberShield, InventoryHub, EcoRoute..."
              value={formData.name}
              onChange={handleChange}
              required
              minLength={2}
              maxLength={150}
            />
          </div>

          <div className="input-group">
            <label className="input-label" htmlFor="projectType">
              Project Type *
            </label>
            <select
              id="projectType"
              name="projectType"
              className="input-field"
              value={formData.projectType}
              onChange={handleChange}
              required
            >
              {PROJECT_TYPES.map((type) => (
                <option key={type.id} value={type.id} style={{ background: '#0f172a' }}>
                  {type.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="input-group">
          <label className="input-label" htmlFor="description">
            Project Description * (Min 10 characters)
          </label>
          <textarea
            id="description"
            name="description"
            className="input-field"
            placeholder="Describe what you want to build, the core concept, and key functionality..."
            rows={3}
            value={formData.description}
            onChange={handleChange}
            required
            minLength={10}
            maxLength={5000}
          />
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '16px' }}>
          <div className="input-group">
            <label className="input-label" htmlFor="problemStatement">
              Problem Statement (Optional)
            </label>
            <input
              id="problemStatement"
              name="problemStatement"
              type="text"
              className="input-field"
              placeholder="e.g., Teams suffer from high latencies and lack observability"
              value={formData.problemStatement}
              onChange={handleChange}
              maxLength={5000}
            />
          </div>

          <div className="input-group">
            <label className="input-label" htmlFor="targetUsers">
              Target Users (Optional)
            </label>
            <input
              id="targetUsers"
              name="targetUsers"
              type="text"
              className="input-field"
              placeholder="e.g., DevOps Engineers, Clinicians, Students"
              value={formData.targetUsers}
              onChange={handleChange}
              maxLength={2000}
            />
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '16px' }}>
          <div className="input-group">
            <label className="input-label" htmlFor="difficulty">
              Architecture Complexity Target *
            </label>
            <select
              id="difficulty"
              name="difficulty"
              className="input-field"
              value={formData.difficulty}
              onChange={handleChange}
              required
            >
              {DIFFICULTY_LEVELS.map((level) => (
                <option key={level.id} value={level.id} style={{ background: '#0f172a' }}>
                  {level.label}
                </option>
              ))}
            </select>
          </div>

          <div className="input-group">
            <label className="input-label" htmlFor="preferredTechStack">
              Preferred Tech Stack (Optional)
            </label>
            <input
              id="preferredTechStack"
              name="preferredTechStack"
              type="text"
              className="input-field"
              placeholder="e.g., React, Spring Boot, PostgreSQL, Docker"
              value={formData.preferredTechStack}
              onChange={handleChange}
              maxLength={2000}
            />
          </div>
        </div>

        <div className="input-group">
          <label className="input-label" htmlFor="specialRequirements">
            Special Requirements / Constraints (Optional)
          </label>
          <textarea
            id="specialRequirements"
            name="specialRequirements"
            className="input-field"
            placeholder="e.g., Strict HIPAA compliance, OAuth2 authentication, sub-50ms query SLAs"
            rows={2}
            value={formData.specialRequirements}
            onChange={handleChange}
            maxLength={5000}
          />
        </div>

        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            marginTop: '8px',
            paddingTop: '16px',
            borderTop: '1px solid var(--border-color)',
            flexWrap: 'wrap',
            gap: '12px',
          }}
        >
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: '0.75rem',
              color: 'var(--text-muted)',
            }}
          >
            <Info size={14} />
            <span>Strict server-side ownership. AI Requirement Analysis unlocks in Phase 4.</span>
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading || formData.name.trim().length < 2 || formData.description.trim().length < 10}
            style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
          >
            {loading ? (
              <span>Saving Project...</span>
            ) : (
              <>
                <span>Save Project</span>
                <ArrowRight size={16} />
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
}
