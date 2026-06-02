import React, { useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';

/**
 * PremiumGate — wraps any content and shows a paywall overlay for FREE users.
 *
 * Usage:
 *   <PremiumGate feature="Collaborative Editing" reason="Edit shared playlists with friends">
 *     <CollaborativeEditor />
 *   </PremiumGate>
 *
 * Props:
 *   feature  — short name shown in the lock card (e.g. "Offline Downloads")
 *   reason   — one-line description of what they're missing
 *   blur     — if true, blurs children instead of hiding (default: true)
 *   children — the actual premium content
 */
export default function PremiumGate({ feature, reason, blur = true, children }) {
  const { auth } = useContext(AuthContext);
  const navigate = useNavigate();

  const isPremium = auth?.subscriptionTier === 'PREMIUM' || auth?.subscriptionTier === 'FAMILY';
  if (isPremium) return <>{children}</>;

  return (
    <div style={s.wrap}>
      {/* Blurred background preview */}
      {blur && (
        <div style={s.blurred} aria-hidden="true">
          {children}
        </div>
      )}

      {/* Lock overlay */}
      <div style={s.overlay}>
        <div style={s.card}>
          <div style={s.lockIcon}>🔒</div>
          <h3 style={s.featureName}>{feature}</h3>
          <p style={s.reason}>{reason || 'This feature is available on Premium and Family plans.'}</p>

          <div style={s.compareRow}>
            <div style={s.tier}>
              <span style={s.tierIcon}>🎵</span>
              <span style={{ color: '#64748b' }}>FREE</span>
              <span style={{ color: '#ef4444', fontSize: '18px' }}>✗</span>
            </div>
            <div style={{ ...s.tier, background: '#f59e0b15', border: '1px solid #f59e0b55' }}>
              <span style={s.tierIcon}>⭐</span>
              <span style={{ color: '#f59e0b', fontWeight: 600 }}>PREMIUM</span>
              <span style={{ color: '#22c55e', fontSize: '18px' }}>✓</span>
            </div>
          </div>

          <button style={s.upgradeBtn} onClick={() => navigate('/payment')}>
            ⭐ Upgrade to Premium — $9.99/mo
          </button>
          <p style={s.noThanks} onClick={() => navigate(-1)}>Maybe later</p>
        </div>
      </div>
    </div>
  );
}

const s = {
  wrap: { position: 'relative', minHeight: '300px' },
  blurred: { filter: 'blur(4px)', opacity: 0.4, pointerEvents: 'none', userSelect: 'none' },
  overlay: { position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(15,23,42,0.6)', borderRadius: '12px', zIndex: 10, padding: '20px' },
  card: { background: '#1e293b', border: '1px solid #334155', borderRadius: '16px', padding: '32px 28px', textAlign: 'center', maxWidth: '380px', width: '100%', boxShadow: '0 20px 60px rgba(0,0,0,0.5)' },
  lockIcon: { fontSize: '44px', marginBottom: '12px' },
  featureName: { color: '#f1f5f9', fontSize: '20px', fontWeight: 700, margin: '0 0 8px' },
  reason: { color: '#94a3b8', fontSize: '14px', marginBottom: '20px', lineHeight: '1.6' },
  compareRow: { display: 'flex', gap: '10px', marginBottom: '20px' },
  tier: { flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'space-between', background: '#0f172a', border: '1px solid #334155', borderRadius: '8px', padding: '10px 12px' },
  tierIcon: { fontSize: '18px' },
  upgradeBtn: { width: '100%', padding: '13px', borderRadius: '10px', border: 'none', background: '#f59e0b', color: '#fff', fontWeight: 700, fontSize: '14px', cursor: 'pointer', marginBottom: '10px' },
  noThanks: { color: '#475569', fontSize: '12px', cursor: 'pointer', textDecoration: 'underline', margin: 0 },
};
