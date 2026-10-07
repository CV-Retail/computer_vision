import React from 'react';
import { Outlet } from 'react-router-dom';
import './PlayerLayout.css';

export const PlayerLayout: React.FC = () => {
  return (
    <div className="player-layout">
      <main className="player-container">
        <Outlet />
      </main>
    </div>
  );
};
