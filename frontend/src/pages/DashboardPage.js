import React, { useState, useEffect, useContext } from 'react';
import axios from 'axios';
import { AuthContext } from '../context/AuthContext';

const API = 'http://localhost:8080';

export default function DashboardPage() {
  const { auth } = useContext(AuthContext);
  const [songs, setSongs] = useState([]);
  const [recommendations, setRecommendations] = useState([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Don't fetch until auth is loaded
    if (!auth || !auth.token) {
      setLoading(false);
      return;
    }

    const headers = { Authorization: `Bearer ${auth.token}` };

    const fetch = async () => {
      try {
        console.log('🔍 Fetching songs and recommendations with token:', auth.token.substring(0, 20) + '...');
        const [songsRes, recsRes] = await Promise.all([
          axios.get(`${API}/songs`, { headers }),
          axios.post(`${API}/recommendations`, { explorationLevel: 70, limit: 10 }, { headers })
        ]);
        
        console.log('📊 Songs Response:', songsRes.data);
        console.log('📊 Recommendations Response:', recsRes.data);
        
        const songsData = songsRes.data.data || [];
        const recsData = recsRes.data.data || [];
        
        console.log('✅ Parsed Songs:', songsData);
        console.log('✅ Parsed Recommendations:', recsData);
        
        setSongs(songsData);
        setRecommendations(recsData);
      } catch (e) {
        console.error('❌ Error fetching data:', e);
        console.error('Response:', e.response?.data);
      } finally {
        setLoading(false);
      }
    };
    fetch();
  }, [auth]);

  const filtered = songs.filter(s =>
    s.title.toLowerCase().includes(search.toLowerCase()) ||
    s.artist.toLowerCase().includes(search.toLowerCase())
  );

  if (loading) return <div style={styles.page}>Loading...</div>;

  return (
    <div style={styles.page}>
      <div style={styles.header}>
        <h1 style={styles.title}>Welcome, {auth.name}!</h1>
        <p style={styles.subtitle}>Discover and play music</p>
      </div>

      <input
        type="text"
        placeholder="Search songs or artists..."
        value={search}
        onChange={e => setSearch(e.target.value)}
        style={styles.searchInput}
      />

      <div style={styles.section}>
        <h2 style={styles.heading}>🎵 For You</h2>
        <div style={styles.grid}>
          {recommendations.map(rec => (
            <div key={rec.song.id} style={styles.card}>
              <div style={styles.cardContent}>
                <h3 style={styles.songTitle}>{rec.song.title}</h3>
                <p style={styles.artist}>{rec.song.artist}</p>
                <p style={styles.reason}>💡 {rec.reason}</p>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div style={styles.section}>
        <h2 style={styles.heading}>🔥 All Songs ({filtered.length})</h2>
        <div style={styles.grid}>
          {filtered.map(song => (
            <div key={song.id} style={styles.card}>
              <h3 style={styles.songTitle}>{song.title}</h3>
              <p style={styles.artist}>{song.artist}</p>
              <p style={styles.genre}>{song.genre}</p>
              <p style={styles.plays}>▶️ {song.playCount} plays</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

const styles = {
  page: { padding: '20px', maxWidth: '1200px', margin: '0 auto' },
  header: { marginBottom: '30px' },
  title: { fontSize: '32px', fontWeight: 'bold', margin: 0, color: '#fff' },
  subtitle: { color: '#94a3b8', margin: '8px 0 0 0' },
  searchInput: {
    width: '100%',
    padding: '12px 16px',
    borderRadius: '8px',
    border: '1px solid #334155',
    background: '#0f172a',
    color: '#fff',
    fontSize: '14px',
    marginBottom: '30px',
  },
  section: { marginBottom: '40px' },
  heading: { fontSize: '20px', fontWeight: 'bold', color: '#fff', margin: '0 0 16px 0' },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(250px, 1fr))',
    gap: '16px',
  },
  card: {
    background: '#1e293b',
    padding: '16px',
    borderRadius: '8px',
    border: '1px solid #334155',
    cursor: 'pointer',
    transition: 'all 0.2s',
  },
  cardContent: { marginBottom: '12px' },
  songTitle: { fontSize: '16px', fontWeight: '600', margin: 0, color: '#fff' },
  artist: { fontSize: '14px', color: '#cbd5e1', margin: '4px 0' },
  genre: { fontSize: '12px', color: '#64748b', margin: '4px 0', textTransform: 'uppercase' },
  plays: { fontSize: '12px', color: '#94a3b8', margin: '4px 0' },
  reason: { fontSize: '12px', color: '#fbbf24', fontStyle: 'italic', margin: '8px 0 0 0' },
};
