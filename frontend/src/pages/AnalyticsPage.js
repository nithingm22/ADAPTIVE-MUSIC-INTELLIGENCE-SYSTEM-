import React, { useState, useEffect, useContext } from 'react';
import axios from 'axios';
import { AuthContext } from '../context/AuthContext';

const API = 'http://localhost:8080';

export default function AnalyticsPage() {
  const { auth } = useContext(AuthContext);
  const [history, setHistory] = useState([]);
  const [likes, setLikes] = useState([]);
  const [tab, setTab] = useState('history');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!auth || !auth.token) {
      setLoading(false);
      return;
    }
    
    const headers = { Authorization: `Bearer ${auth.token}` };
    
    const fetch = async () => {
      try {
        const [histRes, likesRes] = await Promise.all([
          axios.get(`${API}/user/history`, { headers }),
          axios.get(`${API}/user/likes`, { headers }),
        ]);
        setHistory(histRes.data.data || []);
        setLikes(likesRes.data.data || []);
      } catch (e) {
        console.error(e);
      } finally {
        setLoading(false);
      }
    };
    fetch();
  }, [auth]);

  if (loading) return <div style={styles.page}>Loading...</div>;

  const displayList = tab === 'history' ? history : likes;

  return (
    <div style={styles.page}>
      <h1 style={styles.title}>📊 Analytics</h1>

      <div style={styles.tabs}>
        <button
          onClick={() => setTab('history')}
          style={{ ...styles.tab, borderBottomColor: tab === 'history' ? '#6366f1' : 'transparent' }}
        >
          🔍 Recently Played ({history.length})
        </button>
        <button
          onClick={() => setTab('likes')}
          style={{ ...styles.tab, borderBottomColor: tab === 'likes' ? '#6366f1' : 'transparent' }}
        >
          ♥️ Liked Songs ({likes.length})
        </button>
      </div>

      <div style={styles.list}>
        {displayList.length === 0 ? (
          <p style={styles.empty}>No {tab} yet</p>
        ) : (
          displayList.map((song, idx) => (
            <div key={song.id} style={styles.item}>
              <span>{idx + 1}. {song.title} - {song.artist}</span>
              <span style={styles.genre}>{song.genre}</span>
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
  tabs: {
    display: 'flex',
    borderBottom: '1px solid #334155',
    marginBottom: '20px',
  },
  tab: {
    padding: '12px 16px',
    background: 'none',
    border: 'none',
    color: '#94a3b8',
    cursor: 'pointer',
    borderBottom: '2px solid transparent',
    fontWeight: '600',
  },
  list: { background: '#1e293b', borderRadius: '8px', overflow: 'hidden' },
  item: {
    display: 'flex',
    justifyContent: 'space-between',
    padding: '12px 16px',
    borderBottom: '1px solid #334155',
    color: '#e2e8f0',
  },
  genre: { fontSize: '12px', color: '#64748b', textTransform: 'uppercase' },
  empty: { padding: '20px', textAlign: 'center', color: '#64748b' },
};
