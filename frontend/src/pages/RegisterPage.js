import React, { useState, useContext } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import { register } from '../services/api';

/**
 * RegisterPage — redesigned with subscription tier selection.
 *
 * Users can now choose FREE / PREMIUM / FAMILY at registration so
 * their offline quota (BL7) is set correctly from the start.
 */
export default function RegisterPage() {
  const { setAuth } = useContext(AuthContext);
  const navigate    = useNavigate();

  const [name,     setName]     = useState('');
  const [email,    setEmail]    = useState('');
  const [password, setPassword] = useState('');
  const [tier,     setTier]     = useState('FREE');
  const [error,    setError]    = useState('');
  const [loading,  setLoading]  = useState(false);

  const TIERS = [
    { id: 'FREE',    label: 'Free',    icon: '🎵', storage: '500 MB',  price: 'Free forever' },
    { id: 'PREMIUM', label: 'Premium', icon: '⭐', storage: '2 GB',    price: 'Demo tier' },
    { id: 'FAMILY',  label: 'Family',  icon: '👨‍👩‍👧', storage: '5 GB',    price: 'Demo tier' },
  ];

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await register({ name, email, password, subscriptionTier: tier });
      setAuth({
        token:            res.token,
        name:             res.name,
        email:            res.email,
        role:             res.role,
        subscriptionTier: res.subscriptionTier,
      });
      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={styles.page}>
      <div style={styles.container}>

        <div style={styles.header}>
          <div style={styles.logo}>🎵</div>
          <h1 style={styles.title}>Create Account</h1>
          <p style={styles.subtitle}>Join AMIS and start discovering your music</p>
        </div>

        <form onSubmit={handleSubmit} style={styles.form}>

          <input style={styles.input} placeholder="Full name"
            value={name} onChange={e => setName(e.target.value)} required />
          <input style={styles.input} type="email" placeholder="Email address"
            value={email} onChange={e => setEmail(e.target.value)} required />
          <input style={styles.input} type="password" placeholder="Password (min 6 chars)"
            value={password} onChange={e => setPassword(e.target.value)} required minLength={6} />

          {/* Tier selector */}
          <div>
            <p style={styles.tierLabel}>Choose your storage plan</p>
            <div style={styles.tierGrid}>
              {TIERS.map(t => (
                <button
                  key={t.id} type="button"
                  style={{
                    ...styles.tierCard,
                    border: tier === t.id ? '2px solid #6366f1' : '2px solid #334155',
                    background: tier === t.id ? '#312e81' : '#0f172a',
                  }}
                  onClick={() => setTier(t.id)}
                >
                  <span style={styles.tierIcon}>{t.icon}</span>
                  <span style={styles.tierName}>{t.label}</span>
                  <span style={styles.tierStorage}>{t.storage}</span>
                  <span style={styles.tierPrice}>{t.price}</span>
                </button>
              ))}
            </div>
          </div>

          {error && <p style={styles.error}>{error}</p>}
          <button style={styles.submitBtn} type="submit" disabled={loading}>
            {loading ? 'Creating account…' : 'Create Account'}
          </button>
        </form>

        <p style={styles.footer}>
          Already have an account?{' '}
          <Link to="/login" style={styles.link}>Sign in</Link>
        </p>
      </div>
    </div>
  );
}

const styles = {
  page: {
    minHeight: '100vh', display: 'flex', alignItems: 'center',
    justifyContent: 'center', background: '#0f172a', padding: '24px',
  },
  container: {
    width: '100%', maxWidth: '480px', background: '#1e293b',
    borderRadius: '16px', padding: '36px', boxShadow: '0 25px 50px rgba(0,0,0,0.5)',
  },
  header: { textAlign: 'center', marginBottom: '24px' },
  logo: { fontSize: '40px', marginBottom: '8px' },
  title: { color: '#f8fafc', fontSize: '24px', fontWeight: 700, margin: 0 },
  subtitle: { color: '#94a3b8', fontSize: '14px', marginTop: '4px' },
  form: { display: 'flex', flexDirection: 'column', gap: '12px' },
  input: {
    padding: '12px 14px', borderRadius: '8px', border: '1px solid #334155',
    background: '#0f172a', color: '#f1f5f9', fontSize: '14px', outline: 'none',
  },
  tierLabel: { color: '#cbd5e1', fontSize: '13px', fontWeight: 500, marginBottom: '8px' },
  tierGrid: { display: 'flex', gap: '8px' },
  tierCard: {
    flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center',
    padding: '12px 8px', borderRadius: '10px', cursor: 'pointer', gap: '4px',
  },
  tierIcon: { fontSize: '20px' },
  tierName: { color: '#e2e8f0', fontSize: '13px', fontWeight: 600 },
  tierStorage: { color: '#6366f1', fontSize: '12px', fontWeight: 600 },
  tierPrice: { color: '#64748b', fontSize: '11px' },
  error: { color: '#f87171', fontSize: '13px', margin: 0 },
  submitBtn: {
    padding: '13px', borderRadius: '8px', border: 'none',
    background: '#6366f1', color: '#fff', fontWeight: 600, fontSize: '15px', cursor: 'pointer',
  },
  footer: { textAlign: 'center', color: '#64748b', fontSize: '13px', marginTop: '16px' },
  link: { color: '#818cf8', textDecoration: 'none' },
};
