import React from 'react';
import './GlassCard.css';

export interface GlassCardProps {
  title?: string;
  subtitle?: string;
  className?: string;
  children: React.ReactNode;
  action?: React.ReactNode;
}

export const GlassCard: React.FC<GlassCardProps> = ({
  title,
  subtitle,
  className = '',
  children,
  action,
}) => {
  return (
    <div className={`glass-card glass-panel ${className}`}>
      {(title || action) && (
        <div className="glass-card__header">
          <div>
            {title && <h3 className="glass-card__title">{title}</h3>}
            {subtitle && <p className="glass-card__subtitle">{subtitle}</p>}
          </div>
          {action && <div className="glass-card__action">{action}</div>}
        </div>
      )}
      <div className="glass-card__content">{children}</div>
    </div>
  );
};
