import React from 'react';
import { StatusBadge } from '../../shared/components/StatusBadge';
import './PlayerDisplayView.css';

export const PlayerDisplayView: React.FC = () => {
  return (
    <div className="player-display">
      <div className="player-display__overlay">
        <StatusBadge status="playing" label="Ad Player Kiosk" />
      </div>

      <div className="player-display__content glass-panel">
        <div className="player-display__preview-badge">MODO REPRODUCCIÓN</div>
        <h1 className="player-display__title">Publicidad Inteligente Kiosko</h1>
        <p className="player-display__subtitle">
          Esperando eventos de audiencia en tiempo real via WebSocket
        </p>
        <div className="player-display__status-card">
          <span>Playlist activa: <strong>Playlist por Defecto</strong></span>
        </div>
      </div>
    </div>
  );
};
