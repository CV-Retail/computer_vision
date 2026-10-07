import React from 'react';
import { Outlet, Link, useLocation } from 'react-router-dom';
import { StatusBadge } from '../../shared/components/StatusBadge';
import './AdminLayout.css';

export const AdminLayout: React.FC = () => {
  const location = useLocation();

  const navItems = [
    { path: '/admin', label: 'Dashboard' },
    { path: '/admin/campaigns', label: 'Campañas' },
    { path: '/admin/rules', label: 'Reglas' },
    { path: '/admin/reports', label: 'Reportes' },
  ];

  return (
    <div className="admin-layout">
      <header className="admin-header glass-panel">
        <div className="admin-header__brand">
          <div className="admin-header__logo">CV</div>
          <div>
            <h1 className="admin-header__title">Kiosko Retail Admin</h1>
            <span className="admin-header__subtitle">Portal de Gestión Local</span>
          </div>
        </div>
        <div className="admin-header__status">
          <StatusBadge status="online" label="Sistema Operativo" />
        </div>
      </header>

      <div className="admin-body">
        <aside className="admin-sidebar glass-panel">
          <nav className="admin-nav">
            {navItems.map((item) => {
              const isActive = location.pathname === item.path;
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  className={`admin-nav__link ${isActive ? 'admin-nav__link--active' : ''}`}
                >
                  {item.label}
                </Link>
              );
            })}
          </nav>
          <div className="admin-sidebar__footer">
            <Link to="/player" className="btn-glass btn-glass-primary" target="_blank">
              Abrir Player ↗
            </Link>
          </div>
        </aside>

        <main className="admin-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
