import React from 'react';
import { Link } from 'react-router-dom';
import { GlassCard } from '../components/GlassCard';

export const NotFoundView: React.FC = () => {
  return (
    <div style={{ padding: '3rem', display: 'flex', justifyContent: 'center' }}>
      <GlassCard title="404 - Página no encontrada" subtitle="La ruta solicitada no existe">
        <p style={{ marginBottom: '1.5rem' }}>
          No pudimos encontrar la sección a la que intentas acceder.
        </p>
        <Link to="/admin" className="btn-glass btn-glass-primary">
          Ir al Admin Portal
        </Link>
      </GlassCard>
    </div>
  );
};
