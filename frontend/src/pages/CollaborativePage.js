import React, { useState, useEffect, useContext } from 'react';
import axios from 'axios';
import { AuthContext } from '../context/AuthContext';
import PremiumGate from '../components/PremiumGate';

const API = 'http://localhost:8080';

/**
 * CollaborativePage — BL6: Collaborative Playlist Conflict Resolver
 *
 * SIMPLIFIED vs the old version:
 *  • Songs picked from a dropdown (no need to memorise IDs)
 *  • Step-by-step wizard: Setup → Record Edits → Merge
 *  • Plain-English conflict messages use song titles, not numbers
 *  • "Who is editing" shown clearly at the top
 *  • Songs reference table visible at all times on the right
 */
export default function CollaborativePage() {
  const { auth } = useContext(AuthContext);

  // ── State ─────────────────────────────────────────────────────────────
  const [step, setStep]           = useState(1); // 1=setup, 2=edit, 3=result
  const [songs, setSongs]         = useState([]);
  const [playlists, setPlaylists] = useState([]);

  // Setup inputs
  const [playlistId, setPlaylistId]   = useState('');
  const [collabIds, setCollabIds]     = useState('');

  // Edit inputs
  const [selectedSong, setSelectedSong] = useState('');
  const [editType, setEditType]         = useState('ADD');
  const [position, setPosition]         = useState('');
  const [editLog, setEditLog]           = useState([]);
  const [editError, setEditError]       = useState('');

  // Result
  const [mergeResult, setMergeResult] = useState(null);
  const [mergeError, setMergeError]   = useState('');

  // ── Load songs and playlists on mount ─────────────────────────────
  const headers = auth?.token ? { Authorization: `Bearer ${auth.token}` } : {};

  useEffect(() => {
    if (!auth || !auth.token) return;
    
    axios.get(`${API}/songs`, { headers }).then(r => setSongs(r.data.data || []));
    axios.get(`${API}/playlists`, { headers }).then(r => setPlaylists(r.data.data || []));
  }, [auth, headers]);

  // ── Step 1: Make playlist collaborative ──────────────────────────────
  async function handleSetup() {
    if (!headers.Authorization) {
      alert('Not authenticated. Please log in again.');
      return;
    }
    try {
      const ids = collabIds.split(',').map(s => parseInt(s.trim())).filter(Boolean);
      await axios.post(`${API}/collaborative/${playlistId}/make-shared`,
        { collaboratorIds: ids }, { headers });
      setStep(2);
    } catch (e) {
      alert('Setup failed: ' + (e.response?.data?.message || e.message));
    }
  }

  // ── Step 2: Record an edit ────────────────────────────────────────────
  async function handleRecordEdit() {
    if (!headers.Authorization) {
      setEditError('Not authenticated. Please log in again.');
      return;
    }
    setEditError('');
    if (!selectedSong) { setEditError('Please select a song first.'); return; }
    try {
      const body = {
        songId:   parseInt(selectedSong),
        editType: editType,
        position: position !== '' ? parseInt(position) : undefined,
      };
      const res = await axios.post(
        `${API}/collaborative/${playlistId}/edit`, body, { headers });

      const song = songs.find(s => s.id === parseInt(selectedSong));
      setEditLog(prev => [...prev, {
        user:     auth.name,
        action:   editType,
        song:     song?.title || `Song #${selectedSong}`,
        position: position || '(end)',
        time:     new Date().toLocaleTimeString(),
      }]);
      setSelectedSong('');
      setPosition('');
    } catch (e) {
      setEditError(e.response?.data?.message || e.message);
    }
  }

  // ── Step 3: Merge & Resolve ───────────────────────────────────────────
  async function handleMerge() {
    if (!headers.Authorization) {
      setMergeError('Not authenticated. Please log in again.');
      return;
    }
    setMergeError('');
    try {
      const res = await axios.post(
        `${API}/collaborative/${playlistId}/merge`, {}, { headers });
      setMergeResult(res.data.data);
      setStep(3);
    } catch (e) {
      setMergeError(e.response?.data?.message || e.message);
    }
  }

  // ── Render ────────────────────────────────────────────────────────────

  return (
    <PremiumGate
      feature="Collaborative Playlists"
      reason="Edit shared playlists with friends in real-time. Premium and Family users can record edits, resolve conflicts, and merge changes together.">
    <div style={styles.page}>
      <div style={styles.main}>

        {/* Page Header */}
        <div style={styles.pageHeader}>
          <h1 style={styles.pageTitle}>🤝 Collaborative Playlist</h1>
          <p style={styles.pageDesc}>
            Multiple users can edit the same playlist simultaneously. AMIS
            automatically detects and resolves conflicts when you click "Merge".
          </p>
        </div>

        {/* Step progress bar */}
        <div style={styles.steps}>
          {['1. Setup', '2. Record Edits', '3. Merge Result'].map((label, i) => (
            <div key={i} style={styles.stepItem}>
              <div style={{
                ...styles.stepCircle,
                background: step > i + 1 ? '#22c55e' : step === i + 1 ? '#6366f1' : '#334155',
              }}>
                {step > i + 1 ? '✓' : i + 1}
              </div>
              <span style={{ color: step === i + 1 ? '#e2e8f0' : '#64748b', fontSize: '13px' }}>
                {label}
              </span>
            </div>
          ))}
        </div>

        {/* ── STEP 1: Setup ──────────────────────────────────────────── */}
        {step === 1 && (
          <div style={styles.card}>
            <h2 style={styles.cardTitle}>Step 1 — Choose a playlist to share</h2>
            <p style={styles.cardDesc}>
              Pick any of your playlists and enter the IDs of users you want to
              collaborate with. Both users need to be logged in and take turns
              recording their edits in Step 2.
            </p>

            <label style={styles.label}>Your Playlist</label>
            {playlists.length > 0 ? (
              <select style={styles.select} value={playlistId}
                onChange={e => setPlaylistId(e.target.value)}>
                <option value="">— pick a playlist —</option>
                {playlists.map(p => (
                  <option key={p.id} value={p.id}>#{p.id} — {p.name}</option>
                ))}
              </select>
            ) : (
              <input style={styles.input} placeholder="Playlist ID (number)"
                value={playlistId} onChange={e => setPlaylistId(e.target.value)} />
            )}

            <label style={styles.label}>Collaborator User IDs</label>
            <input style={styles.input} placeholder="e.g. 2, 3  (comma-separated)"
              value={collabIds} onChange={e => setCollabIds(e.target.value)} />
            <p style={styles.hint}>
              Tip: to simulate two users, open this page in two browser tabs
              and log in as different accounts.
            </p>

            <button style={styles.btnPrimary}
              onClick={handleSetup} disabled={!playlistId}>
              Make Collaborative →
            </button>
          </div>
        )}

        {/* ── STEP 2: Record Edits ───────────────────────────────────── */}
        {step === 2 && (
          <div style={styles.card}>
            <h2 style={styles.cardTitle}>Step 2 — Record edits</h2>
            <p style={styles.cardDesc}>
              Logged in as <strong style={{ color: '#818cf8' }}>{auth.name}</strong>.
              Pick a song and action, then click "Record Edit". Switch users
              (log out → log in as someone else) to simulate concurrent edits.
              When done, click "Merge & Resolve" to see conflict resolution.
            </p>

            {/* Edit form */}
            <div style={styles.editForm}>
              <div style={styles.formRow}>
                <div style={styles.formGroup}>
                  <label style={styles.label}>Song</label>
                  <select style={styles.select} value={selectedSong}
                    onChange={e => setSelectedSong(e.target.value)}>
                    <option value="">— pick a song —</option>
                    {songs.map(s => (
                      <option key={s.id} value={s.id}>
                        #{s.id} · {s.title} — {s.artist}
                      </option>
                    ))}
                  </select>
                </div>

                <div style={styles.formGroup}>
                  <label style={styles.label}>Action</label>
                  <select style={styles.select} value={editType}
                    onChange={e => setEditType(e.target.value)}>
                    <option value="ADD">➕ ADD</option>
                    <option value="REMOVE">➖ REMOVE</option>
                    <option value="REORDER">🔀 REORDER</option>
                  </select>
                </div>

                <div style={{ ...styles.formGroup, flex: '0 0 100px' }}>
                  <label style={styles.label}>Position (opt)</label>
                  <input style={styles.input} type="number" min="0"
                    placeholder="0, 1, 2…" value={position}
                    onChange={e => setPosition(e.target.value)} />
                </div>
              </div>

              {editError && <p style={styles.error}>{editError}</p>}
              <button style={styles.btnPrimary} onClick={handleRecordEdit}>
                Record Edit
              </button>
            </div>

            {/* Edit log */}
            {editLog.length > 0 && (
              <div style={styles.logBox}>
                <p style={styles.logTitle}>📋 Recorded edits this session</p>
                {editLog.map((e, i) => (
                  <div key={i} style={styles.logRow}>
                    <span style={styles.logTime}>{e.time}</span>
                    <span style={styles.logUser}>{e.user}</span>
                    <span style={{
                      ...styles.logBadge,
                      background: e.action === 'ADD' ? '#166534' :
                                  e.action === 'REMOVE' ? '#7f1d1d' : '#1e3a5f',
                    }}>
                      {e.action}
                    </span>
                    <span style={styles.logSong}>"{e.song}"</span>
                    <span style={styles.logPos}>pos {e.position}</span>
                  </div>
                ))}
              </div>
            )}

            {/* Conflict scenario hints */}
            <div style={styles.hintBox}>
              <p style={styles.hintTitle}>💡 Try these conflict scenarios</p>
              <ul style={styles.hintList}>
                <li><strong>Duplicate ADD:</strong> Two users both add the same song → only kept once</li>
                <li><strong>ADD vs REMOVE:</strong> One adds a song, another removes it within 1 min → ADD wins</li>
                <li><strong>Position clash:</strong> Both add different songs at position 0 → both kept, one shifted</li>
              </ul>
            </div>

            <button style={styles.btnSuccess} onClick={handleMerge}>
              ⚡ Merge &amp; Resolve Conflicts
            </button>
            {mergeError && <p style={styles.error}>{mergeError}</p>}
          </div>
        )}

        {/* ── STEP 3: Result ─────────────────────────────────────────── */}
        {step === 3 && mergeResult && (
          <div style={styles.card}>
            <h2 style={styles.cardTitle}>
              ✅ Merge Complete — {mergeResult.conflictsResolved} conflict(s) resolved
            </h2>

            {/* Conflict notifications */}
            {mergeResult.notifications?.length > 0 && (
              <div style={styles.notifBox}>
                <p style={styles.notifTitle}>What was fixed:</p>
                {mergeResult.notifications.map((n, i) => (
                  <p key={i} style={styles.notifRow}>{n}</p>
                ))}
              </div>
            )}

            {/* Resolved playlist */}
            <p style={styles.label}>Resolved Playlist</p>
            <div style={styles.resolvedList}>
              {mergeResult.playlist?.songs?.map((s, i) => (
                <div key={s.id} style={styles.resolvedSong}>
                  <span style={styles.resolvedNum}>{i + 1}</span>
                  <div>
                    <div style={{ color: '#e2e8f0', fontSize: '14px' }}>{s.title}</div>
                    <div style={{ color: '#94a3b8', fontSize: '12px' }}>{s.artist} · {s.genre}</div>
                  </div>
                </div>
              ))}
            </div>

            <button style={styles.btnPrimary} onClick={() => {
              setStep(2); setMergeResult(null); setEditLog([]);
            }}>
              ← Record More Edits
            </button>
          </div>
        )}
      </div>

      {/* ── Song Reference Sidebar ──────────────────────────────────────── */}
      <div style={styles.sidebar}>
        <p style={styles.sidebarTitle}>📋 All Songs &amp; IDs</p>
        <p style={styles.sidebarHint}>Use these IDs when editing via the API directly.</p>
        <div style={styles.songList}>
          {songs.map(s => (
            <div key={s.id} style={styles.songRow}>
              <span style={styles.songId}>#{s.id}</span>
              <div>
                <div style={styles.songTitle}>{s.title}</div>
                <div style={styles.songMeta}>{s.artist} · {s.genre}</div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
    </PremiumGate>
  );
}

// ── Styles ────────────────────────────────────────────────────────────────
const styles = {
  page: { display: 'flex', gap: '24px', padding: '24px', color: '#f1f5f9' },
  main: { flex: 1, minWidth: 0 },
  pageHeader: { marginBottom: '24px' },
  pageTitle: { fontSize: '24px', fontWeight: 700, margin: 0 },
  pageDesc: { color: '#94a3b8', fontSize: '14px', marginTop: '6px' },

  steps: { display: 'flex', gap: '24px', marginBottom: '24px' },
  stepItem: { display: 'flex', alignItems: 'center', gap: '8px' },
  stepCircle: {
    width: '28px', height: '28px', borderRadius: '50%',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    fontSize: '13px', fontWeight: 600, color: '#fff',
  },

  card: {
    background: '#1e293b', borderRadius: '12px', padding: '24px',
    border: '1px solid #334155',
  },
  cardTitle: { fontSize: '18px', fontWeight: 600, marginBottom: '8px', color: '#f1f5f9' },
  cardDesc: { color: '#94a3b8', fontSize: '14px', marginBottom: '20px', lineHeight: '1.6' },

  label: { display: 'block', color: '#cbd5e1', fontSize: '13px', fontWeight: 500, marginBottom: '6px', marginTop: '14px' },
  select: {
    width: '100%', padding: '10px 12px', borderRadius: '8px',
    border: '1px solid #475569', background: '#0f172a', color: '#f1f5f9',
    fontSize: '14px', cursor: 'pointer',
  },
  input: {
    width: '100%', padding: '10px 12px', borderRadius: '8px', boxSizing: 'border-box',
    border: '1px solid #475569', background: '#0f172a', color: '#f1f5f9', fontSize: '14px',
  },
  hint: { color: '#64748b', fontSize: '12px', marginTop: '6px' },
  error: { color: '#f87171', fontSize: '13px', marginTop: '8px' },

  editForm: { marginBottom: '16px' },
  formRow: { display: 'flex', gap: '12px', flexWrap: 'wrap' },
  formGroup: { flex: 1, minWidth: '160px' },

  btnPrimary: {
    marginTop: '16px', padding: '11px 20px', borderRadius: '8px', border: 'none',
    background: '#6366f1', color: '#fff', fontWeight: 600, fontSize: '14px',
    cursor: 'pointer',
  },
  btnSuccess: {
    marginTop: '16px', padding: '11px 20px', borderRadius: '8px', border: 'none',
    background: '#16a34a', color: '#fff', fontWeight: 600, fontSize: '14px',
    cursor: 'pointer',
  },

  logBox: {
    background: '#0f172a', borderRadius: '8px', padding: '14px',
    marginTop: '16px', maxHeight: '200px', overflowY: 'auto',
  },
  logTitle: { color: '#94a3b8', fontSize: '13px', marginBottom: '10px', fontWeight: 600 },
  logRow: { display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px', flexWrap: 'wrap' },
  logTime: { color: '#475569', fontSize: '11px', minWidth: '60px' },
  logUser: { color: '#818cf8', fontSize: '12px', fontWeight: 600, minWidth: '80px' },
  logBadge: { padding: '2px 8px', borderRadius: '4px', fontSize: '11px', fontWeight: 700, color: '#fff' },
  logSong: { color: '#e2e8f0', fontSize: '13px' },
  logPos: { color: '#64748b', fontSize: '11px' },

  hintBox: {
    background: '#172554', border: '1px solid #1e3a8a', borderRadius: '8px',
    padding: '14px', marginTop: '16px',
  },
  hintTitle: { color: '#93c5fd', fontWeight: 600, fontSize: '13px', marginBottom: '8px' },
  hintList: { color: '#bfdbfe', fontSize: '13px', lineHeight: '1.8', paddingLeft: '18px', margin: 0 },

  notifBox: {
    background: '#052e16', border: '1px solid #166534', borderRadius: '8px',
    padding: '14px', marginBottom: '16px',
  },
  notifTitle: { color: '#86efac', fontWeight: 600, fontSize: '13px', marginBottom: '8px' },
  notifRow: { color: '#dcfce7', fontSize: '13px', lineHeight: '1.7', marginBottom: '4px' },

  resolvedList: { display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '16px' },
  resolvedSong: {
    display: 'flex', alignItems: 'center', gap: '12px',
    background: '#0f172a', borderRadius: '8px', padding: '10px 14px',
  },
  resolvedNum: {
    width: '28px', height: '28px', background: '#334155', borderRadius: '50%',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    fontSize: '12px', color: '#94a3b8', flexShrink: 0,
  },

  // Sidebar
  sidebar: {
    width: '260px', flexShrink: 0, background: '#1e293b',
    borderRadius: '12px', padding: '18px', border: '1px solid #334155',
    alignSelf: 'flex-start', position: 'sticky', top: '20px',
  },
  sidebarTitle: { color: '#f1f5f9', fontWeight: 600, fontSize: '14px', marginBottom: '4px' },
  sidebarHint: { color: '#64748b', fontSize: '11px', marginBottom: '12px' },
  songList: { display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '70vh', overflowY: 'auto' },
  songRow: { display: 'flex', alignItems: 'flex-start', gap: '10px' },
  songId: {
    background: '#334155', color: '#94a3b8', padding: '2px 7px',
    borderRadius: '4px', fontSize: '11px', fontWeight: 600, flexShrink: 0, marginTop: '2px',
  },
  songTitle: { color: '#e2e8f0', fontSize: '13px' },
  songMeta: { color: '#64748b', fontSize: '11px' },
};
