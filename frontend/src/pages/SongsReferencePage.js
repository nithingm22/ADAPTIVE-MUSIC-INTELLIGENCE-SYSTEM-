import React, { useState, useEffect, useContext } from 'react';
import axios from 'axios';
import { AuthContext } from '../context/AuthContext';

const API = 'http://localhost:8080';

const GENRE_COLORS = {
  rock: '#ef4444', pop: '#ec4899', jazz: '#8b5cf6', classical: '#3b82f6',
  'hip-hop': '#f59e0b', electronic: '#06b6d4', ambient: '#10b981',
};

/**
 * SongsReferencePage — a searchable, filterable catalog of all songs.
 *
 * This solves the "I don't know what song IDs to use" problem for
 * BL6 (Collaborative), BL7 (Offline), BL5 (Smart Playlist), and
 * the analytics endpoints.
 *
 * Features:
 *  • Shows every song with its numeric ID
 *  • Filter by genre
 *  • Search by title or artist
 *  • Click "Copy ID" to copy the song ID to clipboard
 */
export default function SongsReferencePage() {
  const { auth }     = useContext(AuthContext);
  const [songs, setSongs]   = useState([]);
  const [query, setQuery]   = useState('');
  const [genre, setGenre]   = useState('');
  const [copied, setCopied] = useState(null);

  useEffect(() => {
    if (!auth || !auth.token) return;
    
    const headers = { Authorization: `Bearer ${auth.token}` };
    
    axios.get(`${API}/songs`, { headers }).then(r => setSongs(r.data.data || []));
  }, [auth]);

  const genres = [...new Set(songs.map(s => s.genre))].sort();

  const filtered = songs.filter(s => {
    const q = query.toLowerCase();
    const matchesText = !q || s.title.toLowerCase().includes(q)
                              || s.artist.toLowerCase().includes(q);
    const matchesGenre = !genre || s.genre === genre;
    return matchesText && matchesGenre;
  });

  function copyId(id) {
    navigator.clipboard.writeText(String(id));
    setCopied(id);
    setTimeout(() => setCopied(null), 1500);
  }

  return (
    <div style={styles.page}>
      <div style={styles.header}>
        <h1 style={styles.title}>🎵 Song Catalog</h1>
        <p style={styles.subtitle}>
          All songs and their IDs. Use these IDs with the Collaborative, Offline,
          and Smart Playlist features.
        </p>
      </div>

      {/* Filter bar */}
      <div style={styles.filterBar}>
        <input
          style={styles.searchInput}
          placeholder="Search by title or artist…"
          value={query}
          onChange={e => setQuery(e.target.value)}
        />
        <select style={styles.genreSelect} value={genre}
          onChange={e => setGenre(e.target.value)}>
          <option value="">All genres</option>
          {genres.map(g => (
            <option key={g} value={g}>{g.charAt(0).toUpperCase() + g.slice(1)}</option>
          ))}
        </select>
        <span style={styles.count}>{filtered.length} songs</span>
      </div>

      {/* Song grid */}
      <div style={styles.grid}>
        {filtered.map(s => {
          const gc = GENRE_COLORS[s.genre] || '#6366f1';
          return (
            <div key={s.id} style={styles.card}>
              <div style={{ ...styles.genreStripe, background: gc }} />
              <div style={styles.cardBody}>
                <div style={styles.idRow}>
                  <span style={styles.idBadge}>ID: {s.id}</span>
                  <button
                    style={styles.copyBtn}
                    onClick={() => copyId(s.id)}
                  >
                    {copied === s.id ? '✓ Copied' : 'Copy ID'}
                  </button>
                </div>
                <div style={styles.songTitle}>{s.title}</div>
                <div style={styles.songArtist}>{s.artist}</div>
                <div style={styles.metaRow}>
                  <span style={{ ...styles.genreBadge, background: gc + '33', color: gc }}>
                    {s.genre}
                  </span>
                  <span style={styles.duration}>
                    {s.duration ? `${Math.floor(s.duration / 60)}:${String(s.duration % 60).padStart(2, '0')}` : '—'}
                  </span>
                  <span style={styles.plays}>
                    {(s.playCount || 0).toLocaleString()} plays
                  </span>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {filtered.length === 0 && (
        <p style={styles.empty}>No songs match your search.</p>
      )}

      {/* SQL seed reminder */}
      <div style={styles.sqlHint}>
        <p style={styles.sqlTitle}>⚠️ No songs showing?</p>
        <p style={styles.sqlText}>
          Songs are not auto-seeded — run the SQL INSERT block from the Setup Guide
          in your PostgreSQL client (<code>psql -d amis_db</code>) after starting
          the backend for the first time.
        </p>
      </div>
    </div>
  );
}

const styles = {
  page: { padding: '24px', color: '#f1f5f9', maxWidth: '1100px' },
  header: { marginBottom: '20px' },
  title: { fontSize: '24px', fontWeight: 700, margin: 0 },
  subtitle: { color: '#94a3b8', fontSize: '14px', marginTop: '6px' },

  filterBar: {
    display: 'flex', gap: '10px', alignItems: 'center',
    marginBottom: '20px', flexWrap: 'wrap',
  },
  searchInput: {
    flex: 1, minWidth: '200px', padding: '10px 14px', borderRadius: '8px',
    border: '1px solid #334155', background: '#1e293b', color: '#f1f5f9',
    fontSize: '14px',
  },
  genreSelect: {
    padding: '10px 12px', borderRadius: '8px',
    border: '1px solid #334155', background: '#1e293b', color: '#f1f5f9',
    fontSize: '14px', cursor: 'pointer',
  },
  count: { color: '#64748b', fontSize: '13px', whiteSpace: 'nowrap' },

  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))',
    gap: '14px',
  },
  card: {
    background: '#1e293b', borderRadius: '10px', overflow: 'hidden',
    border: '1px solid #334155', display: 'flex', flexDirection: 'column',
  },
  genreStripe: { height: '4px' },
  cardBody: { padding: '14px' },
  idRow: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' },
  idBadge: {
    background: '#334155', color: '#94a3b8', padding: '2px 8px',
    borderRadius: '4px', fontSize: '12px', fontWeight: 700,
  },
  copyBtn: {
    background: 'transparent', border: '1px solid #475569', color: '#94a3b8',
    padding: '2px 8px', borderRadius: '4px', fontSize: '11px', cursor: 'pointer',
  },
  songTitle: { color: '#f1f5f9', fontWeight: 600, fontSize: '14px', marginBottom: '2px' },
  songArtist: { color: '#94a3b8', fontSize: '13px', marginBottom: '8px' },
  metaRow: { display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' },
  genreBadge: { padding: '2px 8px', borderRadius: '4px', fontSize: '11px', fontWeight: 600 },
  duration: { color: '#64748b', fontSize: '11px' },
  plays: { color: '#64748b', fontSize: '11px' },

  empty: { color: '#64748b', textAlign: 'center', padding: '40px', fontSize: '15px' },
  sqlHint: {
    marginTop: '24px', background: '#1e293b', border: '1px solid #92400e',
    borderRadius: '10px', padding: '16px',
  },
  sqlTitle: { color: '#fbbf24', fontWeight: 600, fontSize: '14px', marginBottom: '6px' },
  sqlText: { color: '#94a3b8', fontSize: '13px', lineHeight: '1.6' },
};
