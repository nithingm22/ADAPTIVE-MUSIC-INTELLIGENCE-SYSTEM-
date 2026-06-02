import React, { useState, useEffect, useContext } from 'react';
import axios from 'axios';
import { AuthContext } from '../context/AuthContext';
const API = 'http://localhost:8080';

export default function PlayAnalyticsPage() {
  const { auth } = useContext(AuthContext);
  const [trending, setTrending] = useState([]);
  const [artists, setArtists] = useState({});
  const [genres, setGenres] = useState({});
  const [streak, setStreak] = useState(0);
  const [hours, setHours] = useState(0);
  const [tab, setTab] = useState('trending');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!auth || !auth.token) {
      setLoading(false);
      return;
    }
    
    const headers = { Authorization: `Bearer ${auth.token}` };
    
    const fetch = async () => {
      try {
        const [trendingRes, artistsRes, genresRes, streakRes, hoursRes] = await Promise.all([
          axios.get(`${API}/analytics/trending`, { headers }),
          axios.get(`${API}/analytics/artists`, { headers }),
          axios.get(`${API}/analytics/genres`, { headers }),
          axios.get(`${API}/analytics/streak`, { headers }),
          axios.get(`${API}/analytics/weekly-hours`, { headers }),
        ]);
        setTrending(trendingRes.data.data || []);
        setArtists(artistsRes.data.data || {});
        setGenres(genresRes.data.data || {});
        setStreak(streakRes.data.data || 0);
        setHours(hoursRes.data.data || 0);
      } catch (e) {
        console.error(e);
      } finally {
        setLoading(false);
      }
    };
    fetch();
  }, [auth]);

  if (loading) return <div style={styles.page}>Loading...</div>;

  return (
    <div style={styles.page}>
      <h1 style={styles.title}>📊 Play Analytics</h1>

      <div style={styles.metrics}>
        <div style={styles.metricCard}>
          <div style={styles.metricValue}>{streak}</div>
          <div style={styles.metricLabel}>Day Streak 🔥</div>
        </div>
        <div style={styles.metricCard}>
          <div style={styles.metricValue}>{hours.toFixed(1)}</div>
          <div style={styles.metricLabel}>Hours This Week ⏱️</div>
        </div>
      </div>

      <div style={styles.tabs}>
        <button onClick={() => setTab('trending')} style={{...styles.tab, borderBottomColor: tab === 'trending' ? '#6366f1' : 'transparent'}}>🚀 Trending</button>
        <button onClick={() => setTab('artists')} style={{...styles.tab, borderBottomColor: tab === 'artists' ? '#6366f1' : 'transparent'}}>🎤 Artists</button>
        <button onClick={() => setTab('genres')} style={{...styles.tab, borderBottomColor: tab === 'genres' ? '#6366f1' : 'transparent'}}>🎵 Genres</button>
      </div>

      {tab === 'trending' && (
        <div style={styles.list}>
          {trending.length === 0 ? <p style={styles.empty}>No data yet. Record some plays!</p> : trending.map((song, idx) => (
            <div key={song.id} style={styles.item}>
              <span>{idx + 1}. {song.title} - {song.artist}</span>
              <span style={styles.delta}>{song.trendingDelta > 0 ? '📈' : '📉'} {song.trendingDelta}</span>
            </div>
          ))}
        </div>
      )}
      {tab === 'artists' && (
        <div style={styles.list}>
          {Object.entries(artists).map(([artist, count], idx) => (
            <div key={artist} style={styles.item}>
              <span>{idx + 1}. {artist}</span>
              <span style={styles.count}>{count} plays</span>
            </div>
          ))}
        </div>
      )}
      {tab === 'genres' && (
        <div style={styles.list}>
          {Object.entries(genres).map(([genre, count], idx) => (
            <div key={genre} style={styles.item}>
              <span>{idx + 1}. {genre}</span>
              <span style={styles.count}>{count} plays</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

const styles = {
  page: { padding: '20px', maxWidth: '900px', margin: '0 auto' },
  title: { fontSize: '28px', fontWeight: 'bold', margin: '0 0 30px 0', color: '#fff' },
  metrics: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '30px' },
  metricCard: { background: '#1e293b', padding: '20px', borderRadius: '8px', textAlign: 'center', border: '1px solid #334155' },
  metricValue: { fontSize: '32px', fontWeight: 'bold', color: '#fbbf24', margin: '0 0 8px 0' },
  metricLabel: { color: '#94a3b8', fontSize: '14px' },
  tabs: { display: 'flex', borderBottom: '1px solid #334155', marginBottom: '20px' },
  tab: { padding: '12px 16px', background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer', borderBottom: '2px solid transparent', fontWeight: '600' },
  list: { background: '#1e293b', borderRadius: '8px', overflow: 'hidden' },
  item: { display: 'flex', justifyContent: 'space-between', padding: '12px 16px', borderBottom: '1px solid #334155', color: '#e2e8f0' },
  delta: { color: '#fbbf24', fontWeight: '600' },
  count: { color: '#94a3b8' },
  empty: { padding: '20px', textAlign: 'center', color: '#64748b' },
};
