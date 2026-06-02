import React, { useState, useEffect, useContext } from 'react';
import axios from 'axios';
import { AuthContext } from '../context/AuthContext';
import PremiumGate from '../components/PremiumGate';
import AdBanner from '../components/AdBanner';

const API = 'http://localhost:8080';

const TIER_COLORS = { FREE: '#6366f1', PREMIUM: '#f59e0b', FAMILY: '#22c55e' };
const TIER_ICONS  = { FREE: '🎵', PREMIUM: '⭐', FAMILY: '👨‍👩‍👧' };

/**
 * OfflinePage — BL7: Offline Download Manager
 *
 * FIXES vs original:
 *  1. Shows correct quota based on user's subscription tier (not always 500 MB)
 *  2. Evicted songs show a "Re-download" button instead of being stuck forever
 *  3. Song picker uses a dropdown (no manual ID entry)
 *  4. Storage gauge colour changes: green (<60%) → yellow (60-85%) → red (>85%)
 *  5. Tier info banner explains what the current user's plan includes
 */
export default function OfflinePage() {
  const { auth } = useContext(AuthContext);

  const [songs, setSongs]       = useState([]);
  const [tracks, setTracks]     = useState([]);
  const [storage, setStorage]   = useState(null);
  const [selectedSong, setSelectedSong] = useState('');
  const [loading, setLoading]   = useState(false);
  const [msg, setMsg]           = useState('');

  // Create headers safely - check auth exists
  const headers = auth?.token ? { Authorization: `Bearer ${auth.token}` } : {};

  // Fetch initial data when auth is available
  useEffect(() => {
    if (!auth || !auth.token) return;
    
    axios.get(`${API}/songs`, { headers }).then(r => setSongs(r.data.data || []));
    refreshTracks();
    refreshStorage();
  }, [auth, headers]);

  async function refreshTracks() {
    if (!headers.Authorization) return;
    const res = await axios.get(`${API}/offline/tracks`, { headers });
    setTracks(res.data.data || []);
  }

  async function refreshStorage() {
    if (!headers.Authorization) return;
    const res = await axios.get(`${API}/offline/storage`, { headers });
    setStorage(res.data.data);
  }

  function refresh() { refreshTracks(); refreshStorage(); }

  // ── Queue a song ──────────────────────────────────────────────────────
  async function handleQueue() {
    if (!headers.Authorization) { setMsg('❌ Not authenticated'); return; }
    if (!selectedSong) return;
    setLoading(true); setMsg('');
    try {
      await axios.post(`${API}/offline/queue`, { songId: parseInt(selectedSong) }, { headers });
      setMsg('✅ Song added to queue.');
      setSelectedSong('');
      refresh();
    } catch (e) {
      setMsg('❌ ' + (e.response?.data?.message || e.message));
    } finally { setLoading(false); }
  }

  // ── Process queue ─────────────────────────────────────────────────────
  async function handleProcess() {
    if (!headers.Authorization) { setMsg('❌ Not authenticated'); return; }
    setLoading(true); setMsg('');
    try {
      const res = await axios.post(`${API}/offline/process`, {}, { headers });
      const downloaded = res.data.data?.length || 0;
      setMsg(`✅ Processed queue — ${downloaded} track(s) downloaded.`);
      refresh();
    } catch (e) {
      setMsg('❌ ' + (e.response?.data?.message || e.message));
    } finally { setLoading(false); }
  }

  // ── Re-download an evicted song (FIX BUG 1) ──────────────────────────
  async function handleRedownload(songId) {
    if (!headers.Authorization) { setMsg('❌ Not authenticated'); return; }
    setMsg('');
    try {
      await axios.post(`${API}/offline/queue`, { songId }, { headers });
      setMsg(`✅ Re-queued song #${songId}. Click "Process Queue" to download.`);
      refresh();
    } catch (e) {
      setMsg('❌ ' + (e.response?.data?.message || e.message));
    }
  }

  // ── Remove / evict a track ────────────────────────────────────────────
  async function handleRemove(songId) {
    if (!headers.Authorization) { setMsg('❌ Not authenticated'); return; }
    try {
      await axios.delete(`${API}/offline/tracks/${songId}`, { headers });
      setMsg('ℹ️ Track removed from offline storage.');
      refresh();
    } catch (e) {
      setMsg('❌ ' + (e.response?.data?.message || e.message));
    }
  }

  // ── Mark played offline ───────────────────────────────────────────────
  async function handleMarkPlayed(songId) {
    if (!headers.Authorization) { setMsg('❌ Not authenticated'); return; }
    await axios.post(`${API}/offline/played`, { songId }, { headers });
    setMsg('✅ Marked as played offline.');
    refresh();
  }

  // ── Helpers ───────────────────────────────────────────────────────────
  function getSongName(songId) {
    const s = songs.find(s => s.id === songId);
    return s ? `${s.title} — ${s.artist}` : `Song #${songId}`;
  }

  function gaugeColor(pct) {
    if (pct < 60) return '#22c55e';
    if (pct < 85) return '#f59e0b';
    return '#ef4444';
  }

  // Tier from storage response or auth context
  const tier = storage?.subscriptionTier || auth.subscriptionTier || 'FREE';
  const tierColor = TIER_COLORS[tier] || '#6366f1';

  const downloaded = tracks.filter(t => t.status === 'DOWNLOADED');
  const queued     = tracks.filter(t => t.status === 'QUEUED');
  const evicted    = tracks.filter(t => t.status === 'EVICTED');

  // ── Render ────────────────────────────────────────────────────────────
  return (
    <div style={styles.page}>
      <AdBanner />

      {/* Page Header */}
      <div style={styles.header}>
        <h1 style={styles.title}>📱 Offline Download Manager</h1>
        <p style={styles.subtitle}>
          Download songs for offline listening. Your storage quota depends on
          your subscription tier.
        </p>
      </div>

      {/* FREE tier limit notice */}
      {tier === 'FREE' && (
        <div style={styles.freeLimitNotice}>
          <span>⚠️ Free plan: max 3 offline songs &amp; 500 MB storage.</span>
          <button style={styles.upgradeInline} onClick={() => window.location.href = '/payment'}>
            Upgrade for unlimited →
          </button>
        </div>
      )}

      {/* ── Tier + Storage Summary ─────────────────────────────────────── */}
      {storage && (
        <div style={styles.storageSummary}>

          {/* Tier badge */}
          <div style={{ ...styles.tierBadge, background: tierColor + '22', border: `1px solid ${tierColor}` }}>
            <span style={styles.tierIcon}>{TIER_ICONS[tier] || '🎵'}</span>
            <div>
              <div style={{ ...styles.tierName, color: tierColor }}>{tier} Plan</div>
              <div style={styles.tierDesc}>
                {tier === 'FREE'    && '500 MB offline storage'}
                {tier === 'PREMIUM' && '2,048 MB (2 GB) offline storage — 4× more than FREE'}
                {tier === 'FAMILY'  && '5,120 MB (5 GB) offline storage — up to 5 users'}
              </div>
            </div>
          </div>

          {/* Gauge */}
          <div style={styles.gaugeWrap}>
            <div style={styles.gaugeTop}>
              <span style={styles.gaugeLabel}>Storage used</span>
              <span style={{ ...styles.gaugePercent, color: gaugeColor(storage.usedPercent) }}>
                {storage.usedPercent}%
              </span>
            </div>
            <div style={styles.gaugeTrack}>
              <div style={{
                ...styles.gaugeFill,
                width: `${Math.min(storage.usedPercent, 100)}%`,
                background: gaugeColor(storage.usedPercent),
              }} />
            </div>
            <div style={styles.gaugeBottom}>
              <span>{storage.usedMb.toFixed(1)} MB used</span>
              <span>{storage.availableMb.toFixed(1)} MB free of {storage.quotaMb} MB</span>
            </div>
          </div>

          {/* Track counts */}
          <div style={styles.countRow}>
            {[
              { label: 'Downloaded', count: storage.downloadedTracks, color: '#22c55e' },
              { label: 'Queued',     count: storage.queuedTracks,     color: '#6366f1' },
              { label: 'Evicted',    count: storage.evictedTracks || evicted.length, color: '#ef4444' },
            ].map(c => (
              <div key={c.label} style={{ ...styles.countCard, borderColor: c.color }}>
                <div style={{ ...styles.countNum, color: c.color }}>{c.count}</div>
                <div style={styles.countLabel}>{c.label}</div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Feedback message */}
      {msg && <div style={styles.msg}>{msg}</div>}

      {/* ── Queue a new song ───────────────────────────────────────────── */}
      <div style={styles.card}>
        <h2 style={styles.cardTitle}>Queue a Song</h2>
        <div style={styles.queueRow}>
          <select style={styles.select} value={selectedSong}
            onChange={e => setSelectedSong(e.target.value)}>
            <option value="">— pick a song to download —</option>
            {songs.map(s => (
              <option key={s.id} value={s.id}>
                #{s.id} · {s.title} — {s.artist} ({s.genre})
              </option>
            ))}
          </select>
          <button style={styles.btnPrimary} onClick={handleQueue}
            disabled={!selectedSong || loading}>
            ➕ Add to Queue
          </button>
          <button style={styles.btnSuccess} onClick={handleProcess}
            disabled={loading}>
            ▶️ Process Queue
          </button>
        </div>
        <p style={styles.hint}>
          Priority is assigned automatically: playlist songs → recently played → liked → manual.
          Higher priority downloads first when storage is limited.
        </p>
      </div>

      {/* ── Downloaded Tracks ─────────────────────────────────────────── */}
      <div style={styles.card}>
        <h2 style={styles.cardTitle}>✅ Downloaded ({downloaded.length})</h2>
        {downloaded.length === 0
          ? <p style={styles.empty}>No downloaded tracks yet. Queue songs above.</p>
          : downloaded.map(t => (
            <div key={t.id} style={styles.trackRow}>
              <div style={styles.trackInfo}>
                <div style={styles.trackName}>{getSongName(t.songId)}</div>
                <div style={styles.trackMeta}>
                  {t.sizeMb?.toFixed(1)} MB ·
                  Priority {t.priority} ·
                  {t.lastPlayedOffline
                    ? ` Last played ${new Date(t.lastPlayedOffline).toLocaleDateString()}`
                    : ' Never played offline'}
                </div>
              </div>
              <div style={styles.trackActions}>
                <button style={styles.btnSmallBlue}
                  onClick={() => handleMarkPlayed(t.songId)}>▶ Played</button>
                <button style={styles.btnSmallRed}
                  onClick={() => handleRemove(t.songId)}>🗑 Remove</button>
              </div>
            </div>
          ))
        }
      </div>

      {/* ── Queued Tracks ─────────────────────────────────────────────── */}
      {queued.length > 0 && (
        <div style={styles.card}>
          <h2 style={styles.cardTitle}>⏳ Queued ({queued.length})</h2>
          {queued.map(t => (
            <div key={t.id} style={styles.trackRow}>
              <div style={styles.trackInfo}>
                <div style={styles.trackName}>{getSongName(t.songId)}</div>
                <div style={styles.trackMeta}>
                  {t.sizeMb?.toFixed(1)} MB · Priority {t.priority}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* ── Evicted Tracks ─────────────────────────────────────────────── */}
      {evicted.length > 0 && (
        <div style={styles.card}>
          <h2 style={styles.cardTitle}>🔴 Evicted ({evicted.length})</h2>
          <p style={styles.hint}>
            These tracks were automatically removed to free up space.
            Click "Re-download" to add them back to the queue.
          </p>
          {evicted.map(t => (
            <div key={t.id} style={styles.trackRow}>
              <div style={styles.trackInfo}>
                <div style={styles.trackName}>{getSongName(t.songId)}</div>
                <div style={styles.trackMeta}>
                  {t.sizeMb?.toFixed(1)} MB — was evicted to free space
                </div>
              </div>
              <div style={styles.trackActions}>
                {/* FIX BUG 1: This button now works — backend resets EVICTED → QUEUED */}
                <button style={styles.btnSmallGreen}
                  onClick={() => handleRedownload(t.songId)}>
                  ↩ Re-download
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ── Styles ────────────────────────────────────────────────────────────────
const styles = {
  page: { padding: '24px', color: '#f1f5f9', maxWidth: '900px' },
  header: { marginBottom: '24px' },
  title: { fontSize: '24px', fontWeight: 700, margin: 0 },
  subtitle: { color: '#94a3b8', fontSize: '14px', marginTop: '6px' },

  storageSummary: {
    background: '#1e293b', borderRadius: '12px', padding: '20px',
    border: '1px solid #334155', marginBottom: '20px',
    display: 'flex', flexDirection: 'column', gap: '16px',
  },
  tierBadge: {
    display: 'flex', alignItems: 'center', gap: '12px',
    borderRadius: '10px', padding: '12px 16px',
  },
  tierIcon: { fontSize: '28px' },
  tierName: { fontWeight: 700, fontSize: '15px' },
  tierDesc: { color: '#94a3b8', fontSize: '13px' },

  gaugeWrap: {},
  gaugeTop: { display: 'flex', justifyContent: 'space-between', marginBottom: '6px' },
  gaugeLabel: { color: '#94a3b8', fontSize: '13px' },
  gaugePercent: { fontSize: '13px', fontWeight: 700 },
  gaugeTrack: { height: '10px', background: '#334155', borderRadius: '99px', overflow: 'hidden' },
  gaugeFill: { height: '100%', borderRadius: '99px', transition: 'width 0.4s ease' },
  gaugeBottom: {
    display: 'flex', justifyContent: 'space-between',
    color: '#64748b', fontSize: '12px', marginTop: '4px',
  },

  countRow: { display: 'flex', gap: '12px' },
  countCard: {
    flex: 1, background: '#0f172a', borderRadius: '8px',
    padding: '12px', textAlign: 'center', border: '1px solid',
  },
  countNum: { fontSize: '22px', fontWeight: 700 },
  countLabel: { color: '#94a3b8', fontSize: '12px', marginTop: '2px' },

  card: {
    background: '#1e293b', borderRadius: '12px', padding: '20px',
    border: '1px solid #334155', marginBottom: '16px',
  },
  cardTitle: { fontSize: '16px', fontWeight: 600, color: '#f1f5f9', marginBottom: '14px' },
  empty: { color: '#64748b', fontSize: '14px' },
  hint: { color: '#64748b', fontSize: '12px', marginTop: '8px' },

  queueRow: { display: 'flex', gap: '10px', flexWrap: 'wrap' },
  select: {
    flex: 1, minWidth: '200px', padding: '10px 12px', borderRadius: '8px',
    border: '1px solid #475569', background: '#0f172a', color: '#f1f5f9', fontSize: '14px',
  },

  btnPrimary: {
    padding: '10px 16px', borderRadius: '8px', border: 'none',
    background: '#6366f1', color: '#fff', fontWeight: 600, fontSize: '14px', cursor: 'pointer',
  },
  btnSuccess: {
    padding: '10px 16px', borderRadius: '8px', border: 'none',
    background: '#16a34a', color: '#fff', fontWeight: 600, fontSize: '14px', cursor: 'pointer',
  },

  trackRow: {
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    padding: '10px 12px', background: '#0f172a', borderRadius: '8px',
    marginBottom: '8px',
  },
  trackInfo: {},
  trackName: { color: '#e2e8f0', fontSize: '14px', fontWeight: 500 },
  trackMeta: { color: '#64748b', fontSize: '12px', marginTop: '2px' },
  trackActions: { display: 'flex', gap: '8px' },

  btnSmallBlue:  { padding: '5px 10px', borderRadius: '6px', border: 'none', background: '#1e40af', color: '#fff', fontSize: '12px', cursor: 'pointer' },
  btnSmallRed:   { padding: '5px 10px', borderRadius: '6px', border: 'none', background: '#7f1d1d', color: '#fff', fontSize: '12px', cursor: 'pointer' },
  btnSmallGreen: { padding: '5px 10px', borderRadius: '6px', border: 'none', background: '#166534', color: '#fff', fontSize: '12px', cursor: 'pointer' },

  freeLimitNotice: {
    display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap',
    background: '#1c1917', border: '1px solid #78350f', borderRadius: '8px',
    padding: '10px 14px', marginBottom: '16px', color: '#fbbf24', fontSize: '13px',
  },
  upgradeInline: {
    background: 'none', border: 'none', color: '#f59e0b', fontWeight: 600,
    fontSize: '13px', cursor: 'pointer', padding: 0, textDecoration: 'underline',
  },
  storageInfo: {
    padding: '10px 14px', borderRadius: '8px', background: '#0f172a',
    border: '1px solid #334155', color: '#e2e8f0', fontSize: '14px', marginBottom: '16px',
  },
};
