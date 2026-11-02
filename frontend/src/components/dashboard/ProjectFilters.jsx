import React from 'react';
import { Search, Filter, ArrowUpDown, X, ArrowUp, ArrowDown } from 'lucide-react';
import { PROJECT_TYPES, DIFFICULTY_LEVELS } from '../../utils/constants';

const STATUS_OPTIONS = [
  { value: 'ALL', label: 'All Statuses' },
  { value: 'DRAFT', label: 'Draft' },
  { value: 'READY_FOR_ANALYSIS', label: 'Ready for Analysis' },
  { value: 'ANALYZING', label: 'Analyzing' },
  { value: 'UNDER_REVIEW', label: 'Under Review' },
  { value: 'REQUIREMENTS_READY', label: 'Requirements Ready' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'GENERATED', label: 'Generated' },
  { value: 'EXPORTED', label: 'Exported' },
];

export function ProjectFilters({
  filters,
  onFilterChange,
  onResetFilters,
  hasActiveFilters,
}) {
  const handleSearchChange = (e) => {
    const val = e.target.value.slice(0, 100);
    onFilterChange('search', val);
  };

  const toggleSortDirection = () => {
    onFilterChange('direction', filters.direction === 'ASC' ? 'DESC' : 'ASC');
  };

  return (
    <div
      className="card"
      style={{
        padding: '16px 20px',
        background: 'var(--bg-secondary)',
        border: '1px solid var(--border-color)',
        borderRadius: 'var(--radius-md)',
        marginBottom: '20px',
        display: 'flex',
        flexDirection: 'column',
        gap: '12px',
      }}
    >
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: '12px',
          alignItems: 'center',
        }}
      >
        {/* Search Input */}
        <div style={{ position: 'relative', minWidth: '220px' }}>
          <Search
            size={16}
            style={{
              position: 'absolute',
              left: '12px',
              top: '50%',
              transform: 'translateY(-50%)',
              color: 'var(--text-muted)',
              pointerEvents: 'none',
            }}
          />
          <input
            type="text"
            value={filters.search || ''}
            onChange={handleSearchChange}
            placeholder="Search projects (name, idea)..."
            className="form-input"
            maxLength={100}
            style={{
              paddingLeft: '36px',
              paddingRight: filters.search ? '32px' : '12px',
              fontSize: '0.875rem',
              height: '38px',
            }}
          />
          {filters.search && (
            <button
              type="button"
              onClick={() => onFilterChange('search', '')}
              style={{
                position: 'absolute',
                right: '8px',
                top: '50%',
                transform: 'translateY(-50%)',
                background: 'transparent',
                border: 'none',
                color: 'var(--text-muted)',
                cursor: 'pointer',
                padding: '4px',
              }}
              title="Clear search"
            >
              <X size={14} />
            </button>
          )}
        </div>

        {/* Status Filter */}
        <div>
          <select
            value={filters.status || 'ALL'}
            onChange={(e) => onFilterChange('status', e.target.value)}
            className="form-input"
            style={{ fontSize: '0.875rem', height: '38px' }}
          >
            {STATUS_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>

        {/* Type Filter */}
        <div>
          <select
            value={filters.type || 'ALL'}
            onChange={(e) => onFilterChange('type', e.target.value)}
            className="form-input"
            style={{ fontSize: '0.875rem', height: '38px' }}
          >
            <option value="ALL">All Project Types</option>
            {PROJECT_TYPES.map((t) => (
              <option key={t.id || t.value} value={t.id || t.value}>
                {t.label}
              </option>
            ))}
          </select>
        </div>

        {/* Difficulty Filter */}
        <div>
          <select
            value={filters.difficulty || 'ALL'}
            onChange={(e) => onFilterChange('difficulty', e.target.value)}
            className="form-input"
            style={{ fontSize: '0.875rem', height: '38px' }}
          >
            <option value="ALL">All Difficulties</option>
            {DIFFICULTY_LEVELS.map((d) => (
              <option key={d.id || d.value} value={d.id || d.value}>
                {d.label}
              </option>
            ))}
          </select>
        </div>

        {/* Sort Field & Direction */}
        <div style={{ display: 'flex', gap: '6px' }}>
          <select
            value={filters.sort || 'updatedAt'}
            onChange={(e) => onFilterChange('sort', e.target.value)}
            className="form-input"
            style={{ fontSize: '0.875rem', height: '38px', flex: 1 }}
          >
            <option value="updatedAt">Sort: Recently Updated</option>
            <option value="createdAt">Sort: Creation Date</option>
            <option value="name">Sort: Project Name</option>
            <option value="status">Sort: Lifecycle Status</option>
          </select>

          <button
            type="button"
            onClick={toggleSortDirection}
            className="btn btn-ghost"
            style={{
              height: '38px',
              padding: '0 12px',
              border: '1px solid var(--border-color)',
              display: 'flex',
              alignItems: 'center',
              gap: '4px',
              fontSize: '0.8125rem',
            }}
            title={`Direction: ${filters.direction === 'ASC' ? 'Ascending' : 'Descending'}`}
          >
            {filters.direction === 'ASC' ? <ArrowUp size={14} /> : <ArrowDown size={14} />}
            <span>{filters.direction}</span>
          </button>
        </div>
      </div>

      {hasActiveFilters && (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', paddingTop: '4px' }}>
          <button
            type="button"
            onClick={onResetFilters}
            className="btn btn-ghost"
            style={{ fontSize: '0.775rem', padding: '4px 8px', color: 'var(--text-muted)' }}
          >
            <X size={12} style={{ marginRight: '4px' }} />
            Reset all filters
          </button>
        </div>
      )}
    </div>
  );
}

export default ProjectFilters;
