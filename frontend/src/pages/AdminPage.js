import React, { useState, useEffect, useContext } from 'react';
import axios from 'axios';
import { AuthContext } from '../context/AuthContext';

const API = 'http://localhost:8080';

export default function AdminPage() {
  const { auth } = useContext(AuthContext);
  const [songs, setSongs] = useState([]);
  const [tab, setTab] = useState('songs');
  const [newTitle, setNewTitle] = useState('');
  const [newArtist, setNewArtist] = useState('');
  const [newGenre, setNewGenre] = useState('');
  const [newDuration, setNewDuration] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!auth || !auth.token) {
      setLoading(false);
      return;
    }
    
    const headers = { Authorization: `Bearer ${auth.token}` };
    
    const fetch = async () => {
      try {
        const res = await axios.get(`${API}/songs`, { headers });
        setSongs(res.data.data || []);
      } catch (e) {
        console.error(e);
      } finally {
        setLoading(false);
      }
    };
    fetch();
  }, [auth]);

  const handleAddSong = async (e) => {
    e.preventDefault();
    try {
      const res = await axios.post(
        `${API}/songs`,
        { title: newTitle, artist: newArtist, genre: newGenre, duration: parseInt(newDuration) },
        { headers }
      );
      setSongs([...songs, res.data.data]);
      setNewTitle('');
      setNewArtist('');
      setNewGenre('');
      setNewDuration('');
    } catch (e) {
      console.error(e);
    }
  };

  const handleDeleteSong = async (songId) => {
    try {
      await axios.delete(`${API}/songs/${songId}`, { headers });
      setSongs(songs.filter(s => s.id !== songId));
    } catch (e) {
      console.error(e);
    }
  };

  if (loading) return <div style={styles.page}>Loading...</div>;

  return (
    <div style={styles.page}>
      <div style={styles.header}>
        <h1 style={styles.title}>🛡️ Admin Panel</h1>
        <p style={styles.subtitle}>Manage songs and users</p>
      </div>

      <div style={styles.tabs}>
        <button onClick={() => setTab('songs')} style={{...styles.tab, borderBottomColor: tab === 'songs' ? '#6366f1' : 'transparent'}}>Songs</button>
        <button onClick={() => setTab('add')} style={{...styles.tab, borderBottomColor: tab === 'add' ? '#6366f1' : 'transparent'}}>+ Add Song</button>
      </div>

      {tab === 'songs' && (
        <div style={styles.table}>
          {songs.map(song => (
            <div key={song.id} style={styles.row}>
              <div style={{ flex: 1 }}>
                <h3 style={styles.songTitle}>{song.title}</h3>
                <p style={styles.meta}>{song.artist} • {song.genre} • {song.duration}s</p>
              </div>
              <button onClick={() => handleDeleteSong(song.id)} style={styles.btnDelete}>Delete</button>
            </div>
          ))}
        </div>
      )}

      {tab === 'add' && (
        <form onSubmit={handleAddSong} style={styles.form}>
          <input type="text" placeholder="Title" value={newTitle} onChange={e => setNewTitle(e.target.value)} required style={styles.input} />
          <input type="text" placeholder="Artist" value={newArtist} onChange={e => setNewArtist(e.target.value)} required style={styles.input} />
          <input type="text" placeholder="Genre" value={newGenre} onChange={e => setNewGenre(e.target.value)} required style={styles.input} />
          <input type="number" placeholder="Duration (seconds)" value={newDuration} onChange={e => setNewDuration(e.target.value)} required style={styles.input} />
          <button type="submit" style={styles.btnSubmit}>Add Song</button>
        </form>
      )}
    </div>
  );
}

const styles = {
  page: { padding: '20px', maxWidth: '1000px', margin: '0 auto' },
  header: { marginBottom: '30px' },
  title: { fontSize: '28px', fontWeight: 'bold', margin: 0, color: '#fff' },
  subtitle: { color: '#94a3b8', margin: '8px 0 0 0' },
  tabs: { display: 'flex', borderBottom: '1px solid #334155', marginBottom: '20px' },
  tab: { padding: '12px 16px', background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer', borderBottom: '2px solid transparent', fontWeight: '600' },
  table: { background: '#1e293b', borderRadius: '8px', overflow: 'hidden' },
  row: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '16px', borderBottom: '1px solid #334155' },
  songTitle: { fontSize: '14px', fontWeight: '600', margin: 0, color: '#fff' },
  meta: { fontSize: '12px', color: '#64748b', margin: '4px 0 0 0' },
  btnDelete: { padding: '8px 12px', borderRadius: '4px', border: 'none', background: '#dc2626', color: '#fff', cursor: 'pointer' },
  form: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', background: '#1e293b', padding: '20px', borderRadius: '8px' },
  input: { padding: '12px', borderRadius: '6px', border: '1px solid #334155', background: '#0f172a', color: '#fff', gridColumn: '1 / 2' },
  btnSubmit: { padding: '12px', borderRadius: '6px', border: 'none', background: '#6366f1', color: '#fff', fontWeight: '600', cursor: 'pointer', gridColumn: '1 / 3' },
};
