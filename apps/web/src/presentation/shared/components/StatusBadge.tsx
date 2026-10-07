import React from 'react';
import './StatusBadge.css';

export interface StatusBadgeProps {
  status: 'online' | 'offline' | 'idle' | 'playing' | 'warning';
  label?: string;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, label }) => {
  const displayLabel = label || status.toUpperCase();

  return (
    <span className={`status-badge status-badge--${status}`}>
      <span className="status-badge__dot" />
      <span className="status-badge__label">{displayLabel}</span>
    </span>
  );
};
