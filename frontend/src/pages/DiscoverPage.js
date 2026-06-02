import React, { useState, useContext } from 'react';
import axios from 'axios';
import { AuthContext } from '../context/AuthContext';

const API = 'http://localhost:8080';

export default function DiscoverPage() {
  const { auth } = useContext(AuthContext);
  const [explorationLevel, setExplorationLevel] = useState(70);
  const [recommendations, setRecommendations] = useState([]);
  const [loading, setLoading] = useState(false);

  const headers = { Authorization: `Bearer ${auth.token}` };

  const handleGetRecommendations = async () => {
    setLoading(true);
    try {
      const res = await axios.post(
        `${API}/recommendations`,
        { explorationLevel, limit: 15 },
        { headers }
      );
      setRecommendations(res.data.data || []);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleSurprise = async () => {
    setLoading(true);
    try {
      const res = await axios.get(`${API}/recommendations/surprise`, { headers });
      setRecommendations(res.data.data || []);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.page}>
      <div style={styles.header}>
        <h1 style={styles.title}>🧭 Discover</h1>
        <p style={styles.subtitle}>Control how adventurous your recommendations are</p>
      </div>

      <div style={styles.card}>
        <h2 style={styles.cardTitle}>Algorithm Control</h2>
        <p style={styles.label}>Exploration Level: {explorationLevel}%</p>

        <input
          type="range"
          min="0"
          max="100"
          value={explorationLevel}
          onChange={e => setExplorationLevel(parseInt(e.target.value))}
          style={styles.slider}
        />

        <div style={styles.sliderLabels}>
          <span>Comfort Zone (Familiar)</span>
          <span>Full Exploration (New)</span>
        </div>

        <p style={styles.hint}>
          {explorationLevel < 30
            ? 'You\'ll hear mostly songs you know and like'
            : explorationLevel < 70
            ? 'A good mix of familiar and new music'
            : 'Mostly new songs outside your comfort zone'}
        </p>

        <div style={styles.buttonGroup}>
          <button onClick={handleGetRecommendations} disabled={loading} style={styles.btnPrimary}>
            {loading ? 'Loading...' : '✨ Get Recommendations'}
          </button>
          <button onClick={handleSurprise} disabled={loading} style={styles.btnSecondary}>
            🎲 Surprise Me
          </button>
        </div>
      </div>

      {recommendations.length > 0 && (
        <div style={styles.section}>
          <h2 style={styles.heading}>Results ({recommendations.length})</h2>
          <div style={styles.list}>
            {recommendations.map((rec, idx) => (
              <div key={rec.song.id} style={styles.listItem}>
                <div style={{ flex: 1 }}>
                  <h3 style={styles.songTitle}>{rec.song.title}</h3>
                  <p style={styles.artist}>{rec.song.artist} • {rec.song.genre}</p>
                  <p style={styles.reason}>💡 {rec.reason}</p>
                </div>
                <div style={styles.badge}>{rec.type || 'Pick'}</div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

const styles = {
  page: { padding: '20px', maxWidth: '800px', margin: '0 auto' },
  header: { marginBottom: '30px' },
  title: { fontSize: '28px', fontWeight: 'bold', margin: 0, color: '#fff' },
  subtitle: { color: '#94a3b8', margin: '8px 0 0 0' },
  card: {
    background: '#1e293b',
    padding: '24px',
    borderRadius: '8px',
    border: '1px solid #334155',
    marginBottom: '30px',
  },
  cardTitle: { fontSize: '18px', fontWeight: '600', margin: '0 0 16px 0', color: '#fff' },
  label: { fontSize: '14px', color: '#e2e8f0', marginBottom: '12px' },
  slider: { width: '100%', marginBottom: '8px' },
  sliderLabels: {
    display: 'flex',
    justifyContent: 'space-between',
    fontSize: '12px',
    color: '#64748b',
    marginBottom: '16px',
  },
  hint: { fontSize: '13px', color: '#cbd5e1', fontStyle: 'italic', margin: '12px 0' },
  buttonGroup: { display: 'flex', gap: '12px', marginTop: '20px' },
  btnPrimary: {
    flex: 1,
    padding: '12px',
    borderRadius: '6px',
    border: 'none',
    background: '#6366f1',
    color: '#fff',
    fontWeight: '600',
    cursor: 'pointer',
  },
  btnSecondary: {
    flex: 1,
    padding: '12px',
    borderRadius: '6px',
    border: '1px solid #334155',
    background: 'transparent',
    color: '#e2e8f0',
    fontWeight: '600',
    cursor: 'pointer',
  },
  section: { marginTop: '30px' },
  heading: { fontSize: '18px', fontWeight: 'bold', color: '#fff', margin: '0 0 16px 0' },
  list: { display: 'flex', flexDirection: 'column', gap: '12px' },
  listItem: {
    display: 'flex',
    alignItems: 'center',
    background: '#0f172a',
    padding: '12px 16px',
    borderRadius: '6px',
    border: '1px solid #334155',
  },
  songTitle: { fontSize: '14px', fontWeight: '600', margin: 0, color: '#fff' },
  artist: { fontSize: '12px', color: '#94a3b8', margin: '4px 0 0 0' },
  reason: { fontSize: '12px', color: '#fbbf24', margin: '6px 0 0 0' },
  badge: {
    background: '#334155',
    color: '#cbd5e1',
    padding: '6px 12px',
    borderRadius: '4px',
    fontSize: '11px',
    fontWeight: '600',
  },
};
