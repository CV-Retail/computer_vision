import React from 'react';
import { GlassCard } from '../../shared/components/GlassCard';
import { StatusBadge } from '../../shared/components/StatusBadge';
import './AdminDashboardView.css';

export const AdminDashboardView: React.FC = () => {
  return (
    <div className="admin-dashboard">
      <div className="admin-dashboard__header">
        <h2>Panel de Control</h2>
        <p>Estado del kiosko local y resumen de audiencia</p>
      </div>

      <div className="admin-dashboard__grid">
        <GlassCard title="Estado del Servicio" subtitle="Módulos locales del kiosko">
          <div className="status-list">
            <div className="status-item">
              <span>Core Backend (Spring Modulith)</span>
              <StatusBadge status="online" label="Conectado" />
            </div>
            <div className="status-item">
              <span>Servicio de Visión (Python ONNX)</span>
              <StatusBadge status="online" label="Activo (CPU)" />
            </div>
            <div className="status-item">
              <span>Ad Player (Chromium Kiosko)</span>
              <StatusBadge status="playing" label="Reproduciendo" />
            </div>
          </div>
        </GlassCard>

        <GlassCard title="Métricas de Audiencia (Hoy)" subtitle="Conteo anónimo acumulado">
          <div className="metrics-grid">
            <div className="metric-box">
              <span className="metric-box__value">1,240</span>
              <span className="metric-box__label">Personas Detectadas</span>
            </div>
            <div className="metric-box">
              <span className="metric-box__value">14.2s</span>
              <span className="metric-box__label">Dwell Time Promedio</span>
            </div>
            <div className="metric-box">
              <span className="metric-box__value">86%</span>
              <span className="metric-box__label">Efectividad de Reglas</span>
            </div>
          </div>
        </GlassCard>

        <GlassCard title="Campañas Activas" subtitle="Reglas de reproducción cargadas">
          <div className="campaign-list-placeholder">
            <div className="campaign-chip">Pañales (Jóvenes con Bebés)</div>
            <div className="campaign-chip">Snacks & Bebidas (Grupo Jóvenes)</div>
            <div className="campaign-chip">Playlist por Defecto (General)</div>
          </div>
        </GlassCard>
      </div>
    </div>
  );
};
