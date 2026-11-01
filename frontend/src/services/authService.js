import { apiClient } from './api';

const TOKEN_KEY = 'projectforge_token';
const USER_KEY = 'projectforge_user';

export const authService = {
  async register(name, email, password) {
    return apiClient('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ name, email, password }),
    });
  },

  async login(email, password) {
    const response = await apiClient('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });

    if (response && response.token) {
      localStorage.setItem(TOKEN_KEY, response.token);
      if (response.user) {
        localStorage.setItem(USER_KEY, JSON.stringify(response.user));
      }
      window.dispatchEvent(new Event('auth-change'));
    }

    return response;
  },

  logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    window.dispatchEvent(new Event('auth-change'));
  },

  getToken() {
    return localStorage.getItem(TOKEN_KEY);
  },

  getCurrentUser() {
    const userStr = localStorage.getItem(USER_KEY);
    if (!userStr) return null;
    try {
      return JSON.parse(userStr);
    } catch {
      return null;
    }
  },

  isAuthenticated() {
    return !!localStorage.getItem(TOKEN_KEY);
  },

  async fetchProfile() {
    const profile = await apiClient('/auth/me');
    if (profile) {
      localStorage.setItem(USER_KEY, JSON.stringify(profile));
      window.dispatchEvent(new Event('auth-change'));
    }
    return profile;
  },
};
