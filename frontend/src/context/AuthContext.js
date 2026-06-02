import React, { createContext, useState, useContext } from 'react';

/**
 * AuthContext — stores the current user's session.
 *
 * FIX: Added subscriptionTier so every page (especially BL7 Offline)
 *      knows the user's plan without an extra API call.
 *
 * Stored fields:
 *   token            — JWT bearer token
 *   name             — display name
 *   email            — used to identify the user on the backend
 *   role             — ADMIN or USER
 *   subscriptionTier — FREE, PREMIUM, or FAMILY
 */
export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [auth, setAuthState] = useState(() => {
    try {
      const saved = localStorage.getItem('amis_auth');
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });

  function setAuth(data) {
    if (data) {
      localStorage.setItem('amis_auth', JSON.stringify(data));
    } else {
      localStorage.removeItem('amis_auth');
    }
    setAuthState(data);
  }

  /** Call after a successful payment to sync subscriptionTier without re-login. */
  async function refreshUser() {
    if (!auth?.token) return;
    try {
      const res = await fetch('http://localhost:8080/user/me', {
        headers: { Authorization: `Bearer ${auth.token}` }
      });
      const json = await res.json();
      if (json?.data) {
        const updated = { ...auth, subscriptionTier: json.data.subscriptionTier };
        setAuth(updated);
      }
    } catch {
      // silently ignore — user can re-login if needed
    }
  }

  function logout() {
    setAuth(null);
  }

  return (
    <AuthContext.Provider value={{ auth, setAuth, logout, refreshUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
