import React, { useState, useContext } from 'react';
import axios from 'axios';
import { AuthContext } from '../context/AuthContext';
const API = 'http://localhost:8080';

export default function SmartPlaylistPage() {
  const { auth } = useContext(AuthContext);
  const [mood, setMood] = useState('energetic');
  const [songs, setSongs] = useState([]);
  const [loading, setLoading] = useState(false);

  const headers = { Authorization: `Bearer ${auth.token}` };
  const moods = ['energetic', 'calm', 'focus', 'party', 'sleep'];

  const handleGenerate = async () => {
    setLoading(true);
    try {
      const res = await axios.post(`${API}/smart-playlist/generate`, { mood, limit: 15 }, { headers });
      setSongs(res.data.data || []);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.page}>
      <h1 style={styles.title}>✨ Smart Playlist</h1>
      <p style={styles.subtitle}>Generate playlists based on your mood</p>

      <div style={styles.moodSelector}>
        {moods.map(m => (
          <button
            key={m}
            onClick={() => setMood(m)}
            style={{
              ...styles.moodBtn,
              background: mood === m ? '#6366f1' : '#1e293b',
              borderColor: mood === m ? '#6366f1' : '#334155',
            }}
          >
            {m.charAt(0).toUpperCase() + m.slice(1)}
          </button>
        ))}
      </div>

      <button onClick={handleGenerate} disabled={loading} style={styles.btnGenerate}>
        {loading ? 'Generating...' : '🎵 Generate'}
      </button>

      {songs.length > 0 && (
        <div style={styles.list}>
          {songs.map((song, idx) => (
            <div key={song.id} style={styles.item}>
              {idx + 1}. {song.title} - {song.artist}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

const styles = {
  page: { padding: '20px', maxWidth: '600px', margin: '0 auto' },
  title: { fontSize: '28px', fontWeight: 'bold', margin: 0, color: '#fff' },
  subtitle: { color: '#94a3b8', marginBottom: '30px' },
  moodSelector: { display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: '8px', marginBottom: '20px' },
  moodBtn: { padding: '12px', borderRadius: '6px', border: '2px solid', background: '#1e293b', color: '#fff', cursor: 'pointer', fontWeight: '600' },
  btnGenerate: { width: '100%', padding: '12px', borderRadius: '6px', border: 'none', background: '#6366f1', color: '#fff', fontWeight: '600', cursor: 'pointer', marginBottom: '20px' },
  list: { background: '#1e293b', borderRadius: '8px', overflow: 'hidden' },
  item: { padding: '12px 16px', borderBottom: '1px solid #334155', color: '#e2e8f0' },
};
