import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Layers, Sparkles, BookOpen, GitBranch, User, LogIn, LogOut } from 'lucide-react';
import { SystemStatusBadge } from './SystemStatusBadge';
import { AuthModal } from './AuthModal';
import { authService } from '../services/authService';
import { APP_CONFIG } from '../utils/constants';

export function Header() {
  const location = useLocation();
  const [currentUser, setCurrentUser] = useState(authService.getCurrentUser());
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);

  useEffect(() => {
    const handleAuthChange = () => {
      setCurrentUser(authService.getCurrentUser());
    };
    const handleOpenAuth = () => {
      setIsAuthModalOpen(true);
    };

    window.addEventListener('auth-change', handleAuthChange);
    window.addEventListener('open-auth-modal', handleOpenAuth);

    return () => {
      window.removeEventListener('auth-change', handleAuthChange);
      window.removeEventListener('open-auth-modal', handleOpenAuth);
    };
  }, []);

  const handleLogout = () => {
    authService.logout();
  };

  const navItems = [
    { label: 'Dashboard', path: '/', icon: Layers },
    { label: 'Documentation', path: '/docs', icon: BookOpen },
  ];

  return (
    <>
      <header
        style={{
          height: 'var(--header-height)',
          borderBottom: '1px solid var(--border-color)',
          background: 'rgba(10, 14, 23, 0.85)',
          backdropFilter: 'blur(16px)',
          WebkitBackdropFilter: 'blur(16px)',
          position: 'sticky',
          top: 0,
          zIndex: 50,
        }}
      >
        <div
          className="container"
          style={{
            height: '100%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          {/* Brand */}
          <Link
            to="/"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '12px',
            }}
          >
            <div
              style={{
                width: '38px',
                height: '38px',
                borderRadius: 'var(--radius-md)',
                background: 'linear-gradient(135deg, #6366f1 0%, #06b6d4 100%)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: 'var(--shadow-sm)',
              }}
            >
              <Sparkles size={20} color="#ffffff" />
            </div>
            <div>
              <div
                style={{
                  fontSize: '1.125rem',
                  fontWeight: 700,
                  letterSpacing: '-0.02em',
                  color: 'var(--text-primary)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                }}
              >
                {APP_CONFIG.NAME}
                <span className="badge badge-primary" style={{ fontSize: '0.65rem', padding: '2px 6px' }}>
                  Phase 3
                </span>
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                Requirements & Docs Engine
              </div>
            </div>
          </Link>

          {/* Navigation */}
          <nav
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}
          >
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = location.pathname === item.path;
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  className="btn btn-ghost"
                  style={{
                    color: isActive ? 'var(--text-primary)' : 'var(--text-secondary)',
                    backgroundColor: isActive ? 'var(--bg-tertiary)' : 'transparent',
                    fontWeight: isActive ? 600 : 500,
                    fontSize: '0.875rem',
                  }}
                >
                  <Icon size={16} />
                  <span>{item.label}</span>
                </Link>
              );
            })}
          </nav>

          {/* Right Section */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '14px',
            }}
          >
            <SystemStatusBadge />

            <div
              style={{
                height: '24px',
                width: '1px',
                background: 'var(--border-color)',
              }}
            />

            {/* User Authentication Status */}
            {currentUser ? (
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    padding: '6px 12px',
                    borderRadius: 'var(--radius-full)',
                    background: 'var(--bg-secondary)',
                    border: '1px solid var(--border-color)',
                    fontSize: '0.8125rem',
                  }}
                >
                  <div
                    style={{
                      width: '22px',
                      height: '22px',
                      borderRadius: '50%',
                      background: 'var(--primary-light)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: 'var(--primary)',
                    }}
                  >
                    <User size={12} />
                  </div>
                  <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                    {currentUser.name}
                  </span>
                </div>

                <button
                  onClick={handleLogout}
                  className="btn btn-ghost"
                  title="Sign Out"
                  style={{
                    padding: '6px 10px',
                    fontSize: '0.8125rem',
                    color: 'var(--text-muted)',
                  }}
                >
                  <LogOut size={15} />
                  <span>Sign Out</span>
                </button>
              </div>
            ) : (
              <button
                onClick={() => setIsAuthModalOpen(true)}
                className="btn btn-primary"
                style={{
                  padding: '7px 14px',
                  fontSize: '0.8125rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                }}
              >
                <LogIn size={15} />
                <span>Sign In / Register</span>
              </button>
            )}

            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                fontSize: '0.75rem',
                color: 'var(--text-muted)',
                fontFamily: 'var(--font-mono)',
              }}
            >
              <GitBranch size={13} />
              <span>v1.0.0</span>
            </div>
          </div>
        </div>
      </header>

      <AuthModal
        isOpen={isAuthModalOpen}
        onClose={() => setIsAuthModalOpen(false)}
        onAuthSuccess={() => setIsAuthModalOpen(false)}
      />
    </>
  );
}
