import React, { useState, useEffect, useContext } from 'react';
import axios from 'axios';
import { AuthContext } from '../context/AuthContext';
import { useParams } from 'react-router-dom';

const API = 'http://localhost:8080';

export default function PlaylistView() {
  const { auth } = useContext(AuthContext);
  const { id } = useParams();
  const [playlist, setPlaylist] = useState(null);
  const [songs, setSongs] = useState([]);
  const [allSongs, setAllSongs] = useState([]);
  const [selectedSongId, setSelectedSongId] = useState('');
  const [loading, setLoading] = useState(true);

  const headers = { Authorization: `Bearer ${auth.token}` };

  useEffect(() => {
    const fetch = async () => {
      try {
        const [plRes, songsRes] = await Promise.all([
          axios.get(`${API}/playlists/${id}`, { headers }),
          axios.get(`${API}/songs`, { headers }),
        ]);
        setPlaylist(plRes.data.data);
        setSongs(plRes.data.data?.songs || []);
        setAllSongs(songsRes.data.data || []);
      } catch (e) {
        console.error(e);
      } finally {
        setLoading(false);
      }
    };
    fetch();
  }, [id]);

  const handleAddSong = async (e) => {
    e.preventDefault();
    try {
      await axios.post(`${API}/playlists/${id}/songs`, { songId: parseInt(selectedSongId) }, { headers });
      const updatedRes = await axios.get(`${API}/playlists/${id}`, { headers });
      setSongs(updatedRes.data.data?.songs || []);
      setSelectedSongId('');
    } catch (e) {
      console.error(e);
    }
  };

  const handleRemoveSong = async (songId) => {
    try {
      await axios.delete(`${API}/playlists/${id}/songs/${songId}`, { headers });
      setSongs(songs.filter(s => s.id !== songId));
    } catch (e) {
      console.error(e);
    }
  };

  if (loading || !playlist) return <div style={styles.page}>Loading...</div>;

  return (
    <div style={styles.page}>
      <h1 style={styles.title}>{playlist.name}</h1>

      <form onSubmit={handleAddSong} style={styles.form}>
        <select value={selectedSongId} onChange={e => setSelectedSongId(e.target.value)} required style={styles.select}>
          <option value="">Select a song to add...</option>
          {allSongs.map(s => (
            <option key={s.id} value={s.id}>{s.title} - {s.artist}</option>
          ))}
        </select>
        <button type="submit" style={styles.btnAdd}>+ Add</button>
      </form>

      <div style={styles.songsList}>
        {songs.length === 0 ? (
          <p style={styles.empty}>No songs in this playlist yet</p>
        ) : (
          songs.map((song, idx) => (
            <div key={song.id} style={styles.songItem}>
              <span>{idx + 1}. {song.title} - {song.artist}</span>
              <button onClick={() => handleRemoveSong(song.id)} style={styles.btnRemove}>×</button>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

const styles = {
  page: { padding: '20px', maxWidth: '800px', margin: '0 auto' },
  title: { fontSize: '28px', fontWeight: 'bold', margin: '0 0 30px 0', color: '#fff' },
  form: { display: 'flex', gap: '8px', marginBottom: '30px' },
  select: {
    flex: 1,
    padding: '12px',
    borderRadius: '6px',
    border: '1px solid #334155',
    background: '#0f172a',
    color: '#fff',
  },
  btnAdd: {
    padding: '12px 24px',
    borderRadius: '6px',
    border: 'none',
    background: '#6366f1',
    color: '#fff',
    fontWeight: '600',
    cursor: 'pointer',
  },
  songsList: { background: '#1e293b', borderRadius: '8px', overflow: 'hidden' },
  songItem: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '12px 16px',
    borderBottom: '1px solid #334155',
    color: '#e2e8f0',
  },
  btnRemove: {
    background: 'none',
    border: 'none',
    color: '#ef4444',
    fontWeight: 'bold',
    cursor: 'pointer',
    fontSize: '18px',
  },
  empty: { padding: '20px', textAlign: 'center', color: '#64748b' },
};
