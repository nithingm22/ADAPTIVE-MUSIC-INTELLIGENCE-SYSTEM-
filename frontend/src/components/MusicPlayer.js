import React, { useContext } from 'react';
import { PlayerContext } from '../context/PlayerContext';

function MusicPlayer() {
  const { currentSong, isPlaying } = useContext(PlayerContext);

  return (
    <div style={{ padding: '20px', background: '#1a1a1a', borderTop: '1px solid #333' }}>
      {currentSong ? (
        <div>
          Now Playing: <strong>{currentSong.title}</strong> by {currentSong.artist}
        </div>
      ) : (
        <div>Select a song to play</div>
      )}
    </div>
  );
}

export default MusicPlayer;
