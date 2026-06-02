import React, { useState, useEffect, useContext } from 'react';
import axios from 'axios';
import { AuthContext } from '../context/AuthContext';

const API = 'http://localhost:8080';

export default function PlaylistsPage() {
  const { auth } = useContext(AuthContext);
  const [playlists, setPlaylists] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [newName, setNewName] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!auth || !auth.token) {
      setLoading(false);
      return;
    }
    
    const headers = { Authorization: `Bearer ${auth.token}` };
    
    const fetch = async () => {
      try {
        const res = await axios.get(`${API}/playlists`, { headers });
        setPlaylists(res.data.data || []);
      } catch (e) {
        console.error(e);
      } finally {
        setLoading(false);
      }
    };
    fetch();
  }, [auth]);

  const handleCreate = async (e) => {
    e.preventDefault();
    try {
      const res = await axios.post(`${API}/playlists`, { name: newName }, { headers });
      setPlaylists([...playlists, res.data.data]);
      setNewName('');
      setShowForm(false);
    } catch (e) {
      console.error(e);
    }
  };

  if (loading) return <div style={styles.page}>Loading...</div>;

  return (
    <div style={styles.page}>
      <div style={styles.header}>
        <h1 style={styles.title}>📋 My Playlists</h1>
        <button onClick={() => setShowForm(!showForm)} style={styles.btnCreate}>
          {showForm ? '✕ Cancel' : '+ Create Playlist'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleCreate} style={styles.form}>
          <input
            type="text"
            placeholder="Playlist name..."
            value={newName}
            onChange={e => setNewName(e.target.value)}
            required
            style={styles.input}
          />
          <button type="submit" style={styles.btnSubmit}>Create</button>
        </form>
      )}

      <div style={styles.grid}>
        {playlists.map(pl => (
          <div key={pl.id} style={styles.card}>
            <h3 style={styles.playlistName}>{pl.name}</h3>
            <p style={styles.songCount}>{pl.songCount || 0} songs</p>
            <button style={styles.btnView}>View →</button>
          </div>
        ))}
      </div>

      {playlists.length === 0 && !showForm && (
        <div style={styles.empty}>
          <p>No playlists yet. Create one to get started!</p>
        </div>
      )}
    </div>
  );
}

const styles = {
  page: { padding: '20px', maxWidth: '1000px', margin: '0 auto' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '30px' },
  title: { fontSize: '28px', fontWeight: 'bold', margin: 0, color: '#fff' },
  btnCreate: {
    padding: '10px 16px',
    borderRadius: '6px',
    border: '1px solid #6366f1',
    background: '#6366f1',
    color: '#fff',
    fontWeight: '600',
    cursor: 'pointer',
  },
  form: { display: 'flex', gap: '8px', marginBottom: '30px' },
  input: {
    flex: 1,
    padding: '12px',
    borderRadius: '6px',
    border: '1px solid #334155',
    background: '#0f172a',
    color: '#fff',
  },
  btnSubmit: {
    padding: '12px 24px',
    borderRadius: '6px',
    border: 'none',
    background: '#6366f1',
    color: '#fff',
    fontWeight: '600',
    cursor: 'pointer',
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))',
    gap: '16px',
  },
  card: {
    background: '#1e293b',
    padding: '16px',
    borderRadius: '8px',
    border: '1px solid #334155',
  },
  playlistName: { fontSize: '16px', fontWeight: '600', margin: 0, color: '#fff' },
  songCount: { fontSize: '12px', color: '#94a3b8', margin: '8px 0' },
  btnView: {
    width: '100%',
    padding: '8px',
    borderRadius: '4px',
    border: '1px solid #334155',
    background: 'transparent',
    color: '#6366f1',
    cursor: 'pointer',
    fontWeight: '600',
  },
  empty: {
    textAlign: 'center',
    padding: '40px 20px',
    color: '#64748b',
  },
};
