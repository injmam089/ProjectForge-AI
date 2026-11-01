/**
 * Global Constants for ProjectForge AI
 */

export const APP_CONFIG = {
  NAME: 'ProjectForge AI',
  TAGLINE: 'Turn rough software ideas into structured, approved specifications and documentation packages.',
  VERSION: '1.0.0-phase3',
  DOCS_URL: '/docs',
};

export const PROJECT_STATES = {
  DRAFT: { label: 'Draft', color: 'badge-primary' },
  READY_FOR_ANALYSIS: { label: 'Ready for Analysis', color: 'badge-warning' },
  ANALYZING: { label: 'Analyzing', color: 'badge-warning' },
  REQUIREMENTS_READY: { label: 'Requirements Ready', color: 'badge-primary' },
  UNDER_REVIEW: { label: 'Under Review', color: 'badge-warning' },
  APPROVED: { label: 'Approved', color: 'badge-success' },
  GENERATING: { label: 'Generating', color: 'badge-warning' },
  GENERATED: { label: 'Generated', color: 'badge-success' },
  EXPORTED: { label: 'Exported', color: 'badge-success' },
};

export const DIFFICULTY_LEVELS = [
  { id: 'BEGINNER', label: 'Beginner / Prototype' },
  { id: 'INTERMEDIATE', label: 'Intermediate / Production MVP' },
  { id: 'ADVANCED', label: 'Advanced / Enterprise Scale' },
  { id: 'EXPERT', label: 'Expert / Mission-Critical' },
];

export const PROJECT_TYPES = [
  { id: 'WEB_APPLICATION', label: 'Web Application' },
  { id: 'MOBILE_APPLICATION', label: 'Mobile Application' },
  { id: 'DESKTOP_APPLICATION', label: 'Desktop Application' },
  { id: 'API_BACKEND', label: 'API Backend' },
  { id: 'AI_ML_PROJECT', label: 'AI & Machine Learning' },
  { id: 'CYBERSECURITY_PROJECT', label: 'Cybersecurity Project' },
  { id: 'IOT_PROJECT', label: 'IoT Project' },
  { id: 'DATA_ANALYTICS', label: 'Data & Analytics' },
  { id: 'FULL_STACK_APPLICATION', label: 'Full Stack Application' },
  { id: 'OTHER', label: 'Other Software System' },
];
