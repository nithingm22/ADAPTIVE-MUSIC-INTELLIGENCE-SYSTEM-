import React, { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';

/**
 * AdBanner — shown only to FREE-tier users.
 * Simulates the ad experience. Dismissible per session but reappears after 30s.
 * Place it near the top of any page where you want it visible.
 */
export default function AdBanner() {
  const { auth } = useContext(AuthContext);
  const navigate = useNavigate();
  const [dismissed, setDismissed] = useState(false);

  if (auth?.subscriptionTier !== 'FREE' || dismissed) return null;

  return (
    <div style={s.banner}>
      <div style={s.left}>
        <span style={s.adLabel}>Ad</span>
        <div style={s.adContent}>
          <span style={s.adTitle}>🎸 Gear up with SoundGear Pro</span>
          <span style={s.adSub}>Premium headphones starting at $49. Limited time offer.</span>
        </div>
      </div>
      <div style={s.right}>
        <button style={s.upgradeBtn} onClick={() => navigate('/payment')}>
          Remove Ads — Upgrade ⭐
        </button>
        <button style={s.dismissBtn} onClick={() => setDismissed(true)} title="Dismiss">✕</button>
      </div>
    </div>
  );
}

const s = {
  banner: {
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    background: '#1e293b', borderBottom: '1px solid #334155',
    padding: '8px 20px', gap: '12px', flexWrap: 'wrap',
  },
  left: { display: 'flex', alignItems: 'center', gap: '10px' },
  adLabel: { background: '#334155', color: '#64748b', fontSize: '10px', fontWeight: 700, padding: '2px 6px', borderRadius: '3px', letterSpacing: '0.05em' },
  adContent: { display: 'flex', gap: '10px', alignItems: 'center', flexWrap: 'wrap' },
  adTitle: { color: '#cbd5e1', fontSize: '13px', fontWeight: 500 },
  adSub: { color: '#64748b', fontSize: '12px' },
  right: { display: 'flex', alignItems: 'center', gap: '8px' },
  upgradeBtn: { padding: '6px 12px', borderRadius: '6px', border: 'none', background: '#f59e0b', color: '#fff', fontWeight: 600, fontSize: '12px', cursor: 'pointer' },
  dismissBtn: { background: 'none', border: 'none', color: '#475569', cursor: 'pointer', fontSize: '14px', padding: '4px 6px' },
};
