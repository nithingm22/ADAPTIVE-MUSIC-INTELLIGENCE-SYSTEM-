import React, { createContext, useState } from 'react';

export const PlayerContext = createContext();

export function PlayerProvider({ children }) {
  const [currentSong, setCurrentSong] = useState(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [progress, setProgress] = useState(0);
  const [duration, setDuration] = useState(0);

  return (
    <PlayerContext.Provider value={{ currentSong, setCurrentSong, isPlaying, setIsPlaying, progress, setProgress, duration, setDuration }}>
      {children}
    </PlayerContext.Provider>
  );
}
