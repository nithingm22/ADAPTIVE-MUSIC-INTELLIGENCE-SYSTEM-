import React, { useState, useContext } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import { login } from '../services/api';

/**
 * LoginPage — redesigned with:
 *  • One-click "Quick Login" panel showing all 3 demo accounts
 *  • Each card shows name, role, subscription tier, and what's
 *    special about that account so testers know who to log in as
 *  • Standard email/password form still available for custom accounts
 */
export default function LoginPage() {
  const { setAuth }   = useContext(AuthContext);
  const navigate      = useNavigate();
  const [email, setEmail]       = useState('');
  const [password, setPassword] = useState('');
  const [error, setError]       = useState('');
  const [loading, setLoading]   = useState(false);

  // ── Demo account definitions ─────────────────────────────────────────
  const DEMO_USERS = [
    {
      label:    'Admin User',
      email:    'admin@amis.com',
      password: 'admin123',
      role:     'ADMIN',
      tier:     'FREE',
      color:    '#7c3aed',
      icon:     '🛡️',
      note:     'Can add / edit / delete songs. Access to Admin panel.',
    },
    {
      label:    'Alice (Free)',
      email:    'user1@amis.com',
      password: 'user123',
      role:     'USER',
      tier:     'FREE',
      color:    '#0ea5e9',
      icon:     '🎵',
      note:     '500 MB offline storage. Good for testing BL6 & BL7.',
    },
    {
      label:    'Bob (Premium)',
      email:    'user2@amis.com',
      password: 'user123',
      role:     'USER',
      tier:     'PREMIUM',
      color:    '#f59e0b',
      icon:     '⭐',
      note:     '2048 MB offline storage. Test premium quota in BL7.',
    },
  ];

  // ── Handlers ─────────────────────────────────────────────────────────

  async function handleQuickLogin(user) {
    await doLogin(user.email, user.password);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    await doLogin(email, password);
  }

  async function doLogin(em, pw) {
    setError('');
    setLoading(true);
    try {
      const res = await login({ email: em, password: pw });
      setAuth({
        token:            res.token,
        name:             res.name,
        email:            res.email,
        role:             res.role,
        subscriptionTier: res.subscriptionTier,
      });
      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed. Check your credentials.');
    } finally {
      setLoading(false);
    }
  }

  // ── Render ────────────────────────────────────────────────────────────

  return (
    <div style={styles.page}>
      <div style={styles.container}>

        {/* Header */}
        <div style={styles.header}>
          <div style={styles.logo}>🎵</div>
          <h1 style={styles.title}>AMIS</h1>
          <p style={styles.subtitle}>Adaptive Music Intelligence System</p>
        </div>

        {/* ── Quick Login Panel ─────────────────────────────────────── */}
        <div style={styles.section}>
          <p style={styles.sectionLabel}>⚡ Quick Login — Demo Accounts</p>
          <p style={styles.hint}>
            These three accounts are created automatically when the backend starts.
            No setup needed.
          </p>
          <div style={styles.demoGrid}>
            {DEMO_USERS.map(u => (
              <button
                key={u.email}
                style={{ ...styles.demoCard, borderColor: u.color }}
                onClick={() => handleQuickLogin(u)}
                disabled={loading}
              >
                <div style={{ ...styles.demoIcon, background: u.color }}>
                  {u.icon}
                </div>
                <div style={styles.demoInfo}>
                  <span style={styles.demoName}>{u.label}</span>
                  <span style={styles.demoBadge}>
                    <span style={{ ...styles.tag, background: u.color }}>{u.role}</span>
                    <span style={{ ...styles.tag, background: '#374151' }}>{u.tier}</span>
                  </span>
                  <span style={styles.demoNote}>{u.note}</span>
                </div>
              </button>
            ))}
          </div>
        </div>

        {/* Divider */}
        <div style={styles.divider}><span style={styles.dividerText}>or sign in manually</span></div>

        {/* ── Standard Login Form ───────────────────────────────────── */}
        <form onSubmit={handleSubmit} style={styles.form}>
          <input
            style={styles.input}
            type="email"
            placeholder="Email address"
            value={email}
            onChange={e => setEmail(e.target.value)}
            required
          />
          <input
            style={styles.input}
            type="password"
            placeholder="Password"
            value={password}
            onChange={e => setPassword(e.target.value)}
            required
          />
          {error && <p style={styles.error}>{error}</p>}
          <button style={styles.submitBtn} type="submit" disabled={loading}>
            {loading ? 'Signing in…' : 'Sign In'}
          </button>
        </form>

        <p style={styles.footer}>
          No account?{' '}
          <Link to="/register" style={styles.link}>Create one</Link>
        </p>
      </div>
    </div>
  );
}

// ── Styles ────────────────────────────────────────────────────────────────
const styles = {
  page: {
    minHeight: '100vh', display: 'flex', alignItems: 'center',
    justifyContent: 'center', background: '#0f172a', padding: '24px',
  },
  container: {
    width: '100%', maxWidth: '520px', background: '#1e293b',
    borderRadius: '16px', padding: '36px', boxShadow: '0 25px 50px rgba(0,0,0,0.5)',
  },
  header: { textAlign: 'center', marginBottom: '28px' },
  logo: { fontSize: '48px', marginBottom: '8px' },
  title: { color: '#f8fafc', fontSize: '28px', fontWeight: 700, margin: 0 },
  subtitle: { color: '#94a3b8', fontSize: '14px', marginTop: '4px' },

  section: { marginBottom: '20px' },
  sectionLabel: { color: '#e2e8f0', fontWeight: 600, fontSize: '14px', marginBottom: '6px' },
  hint: { color: '#64748b', fontSize: '12px', marginBottom: '12px' },

  demoGrid: { display: 'flex', flexDirection: 'column', gap: '10px' },
  demoCard: {
    display: 'flex', alignItems: 'flex-start', gap: '14px',
    background: '#0f172a', border: '1.5px solid #334155',
    borderRadius: '10px', padding: '12px 14px', cursor: 'pointer',
    textAlign: 'left', transition: 'border-color 0.2s, background 0.2s',
  },
  demoIcon: {
    width: '40px', height: '40px', borderRadius: '10px',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    fontSize: '20px', flexShrink: 0,
  },
  demoInfo: { display: 'flex', flexDirection: 'column', gap: '4px', flex: 1 },
  demoName: { color: '#f1f5f9', fontWeight: 600, fontSize: '14px' },
  demoBadge: { display: 'flex', gap: '6px' },
  tag: {
    padding: '2px 8px', borderRadius: '4px', fontSize: '11px',
    fontWeight: 600, color: '#fff',
  },
  demoNote: { color: '#94a3b8', fontSize: '12px', lineHeight: '1.4' },

  divider: {
    display: 'flex', alignItems: 'center',
    margin: '20px 0',
    '::before': { content: '""', flex: 1, height: '1px', background: '#334155' },
  },
  dividerText: {
    color: '#475569', fontSize: '12px', padding: '0 12px',
    background: '#1e293b',
  },

  form: { display: 'flex', flexDirection: 'column', gap: '12px' },
  input: {
    padding: '12px 14px', borderRadius: '8px', border: '1px solid #334155',
    background: '#0f172a', color: '#f1f5f9', fontSize: '14px', outline: 'none',
  },
  error: { color: '#f87171', fontSize: '13px', margin: 0 },
  submitBtn: {
    padding: '13px', borderRadius: '8px', border: 'none',
    background: '#6366f1', color: '#fff', fontWeight: 600,
    fontSize: '15px', cursor: 'pointer',
  },
  footer: { textAlign: 'center', color: '#64748b', fontSize: '13px', marginTop: '16px' },
  link: { color: '#818cf8', textDecoration: 'none' },
};
