import React, { useContext } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';

const TIER_COLORS = { FREE: '#6366f1', PREMIUM: '#f59e0b', FAMILY: '#22c55e' };

/**
 * Layout — sidebar navigation + main content wrapper.
 *
 * Changes:
 *  • Added "Songs Reference" link (/songs-reference) so users
 *    can always look up song IDs without leaving the app
 *  • User panel at bottom now shows subscription tier badge
 *  • Tier badge color matches FREE/PREMIUM/FAMILY
 */
export default function Layout({ children }) {
  const { auth, logout } = useContext(AuthContext);
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login');
  }

  const navItems = [
    { to: '/dashboard',      icon: '🏠', label: 'Dashboard' },
    { to: '/discover',       icon: '🔭', label: 'Discover' },
    { to: '/playlists',      icon: '📚', label: 'Playlists' },
    { to: '/analytics',      icon: '📊', label: 'Analytics' },
    { to: '/smart-playlist', icon: '🧠', label: 'Smart Mix',      badge: 'BL5' },
    { to: '/collaborative',  icon: '🤝', label: 'Collaborative',  badge: 'BL6' },
    { to: '/offline',        icon: '📱', label: 'Offline',         badge: 'BL7' },
    { to: '/play-analytics', icon: '📈', label: 'Play Analytics', badge: 'BL8' },
    { to: '/songs-reference',icon: '🎵', label: 'Song Catalog' },
    { to: '/invoices',       icon: '🧾', label: 'Invoices' },
    ...(auth?.role === 'ADMIN' ? [{ to: '/admin', icon: '🛡️', label: 'Admin' }] : []),
  ];

  const tier = auth?.subscriptionTier || 'FREE';
  const tierColor = TIER_COLORS[tier] || '#6366f1';

  return (
    <div style={styles.shell}>
      {/* Sidebar */}
      <nav style={styles.sidebar}>
        <div style={styles.brand}>
          <span style={styles.brandIcon}>🎵</span>
          <span style={styles.brandName}>AMIS</span>
        </div>

        <div style={styles.navList}>
          {navItems.map(item => (
            <NavLink
              key={item.to}
              to={item.to}
              style={({ isActive }) => ({
                ...styles.navItem,
                background: isActive ? '#312e81' : 'transparent',
                color: isActive ? '#c7d2fe' : '#94a3b8',
              })}
            >
              <span style={styles.navIcon}>{item.icon}</span>
              <span style={styles.navLabel}>{item.label}</span>
              {item.badge && (
                <span style={styles.navBadge}>{item.badge}</span>
              )}
            </NavLink>
          ))}
        </div>

        {/* Upgrade CTA for FREE users */}
        {auth?.subscriptionTier === 'FREE' && (
          <NavLink to="/payment" style={styles.upgradeCta}>
            <span>⭐</span>
            <span>Upgrade to Premium</span>
          </NavLink>
        )}

        {/* User card at bottom */}
        {auth && (
          <div style={styles.userCard}>
            <div style={styles.userAvatar}>
              {auth.name?.charAt(0)?.toUpperCase() || '?'}
            </div>
            <div style={styles.userInfo}>
              <div style={styles.userName}>{auth.name}</div>
              <div style={styles.userMeta}>
                <span style={styles.roleTag}>{auth.role}</span>
                <span style={{ ...styles.tierTag, background: tierColor + '33', color: tierColor }}>
                  {tier}
                </span>
              </div>
            </div>
            <button style={styles.logoutBtn} onClick={handleLogout} title="Log out">
              ⏏
            </button>
          </div>
        )}
      </nav>

      {/* Main content */}
      <main style={styles.main}>
        {children}
      </main>
    </div>
  );
}

const styles = {
  shell: { display: 'flex', height: '100vh', background: '#0f172a', overflow: 'hidden' },
  sidebar: {
    width: '220px', flexShrink: 0, background: '#1e293b',
    borderRight: '1px solid #334155', display: 'flex',
    flexDirection: 'column', padding: '16px 0',
  },
  brand: {
    display: 'flex', alignItems: 'center', gap: '10px',
    padding: '8px 18px 20px', borderBottom: '1px solid #334155', marginBottom: '8px',
  },
  brandIcon: { fontSize: '22px' },
  brandName: { color: '#f1f5f9', fontWeight: 700, fontSize: '18px' },

  navList: { flex: 1, overflowY: 'auto', padding: '0 8px' },
  navItem: {
    display: 'flex', alignItems: 'center', gap: '10px',
    padding: '9px 10px', borderRadius: '8px', textDecoration: 'none',
    fontSize: '14px', fontWeight: 500, margin: '1px 0', transition: 'all 0.15s',
  },
  navIcon: { fontSize: '16px', width: '20px', textAlign: 'center' },
  navLabel: { flex: 1 },
  navBadge: {
    background: '#1e3a5f', color: '#93c5fd', padding: '1px 6px',
    borderRadius: '4px', fontSize: '10px', fontWeight: 700,
  },

  userCard: {
    margin: '8px', padding: '10px', background: '#0f172a',
    borderRadius: '10px', border: '1px solid #334155',
    display: 'flex', alignItems: 'center', gap: '10px',
  },
  userAvatar: {
    width: '32px', height: '32px', background: '#6366f1',
    borderRadius: '50%', display: 'flex', alignItems: 'center',
    justifyContent: 'center', color: '#fff', fontWeight: 700,
    fontSize: '14px', flexShrink: 0,
  },
  userInfo: { flex: 1, minWidth: 0 },
  userName: { color: '#f1f5f9', fontSize: '13px', fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' },
  userMeta: { display: 'flex', gap: '4px', marginTop: '3px' },
  roleTag: { background: '#334155', color: '#94a3b8', padding: '1px 5px', borderRadius: '3px', fontSize: '10px', fontWeight: 600 },
  tierTag: { padding: '1px 5px', borderRadius: '3px', fontSize: '10px', fontWeight: 600 },
  upgradeCta: {
    display: 'flex', alignItems: 'center', gap: '8px',
    margin: '6px 8px', padding: '9px 10px', borderRadius: '8px',
    background: '#f59e0b22', border: '1px solid #f59e0b55',
    color: '#f59e0b', fontWeight: 600, fontSize: '13px',
    textDecoration: 'none', cursor: 'pointer',
  },
  logoutBtn: {
    background: 'transparent', border: 'none', color: '#64748b',
    cursor: 'pointer', fontSize: '16px', padding: '4px', flexShrink: 0,
  },

  main: { flex: 1, overflowY: 'auto' },
};
