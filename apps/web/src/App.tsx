import React from 'react';
import { BrowserRouter, MemoryRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AdminLayout } from './presentation/admin/layouts/AdminLayout';
import { AdminDashboardView } from './presentation/admin/views/AdminDashboardView';
import { PlayerLayout } from './presentation/player/layouts/PlayerLayout';
import { PlayerDisplayView } from './presentation/player/views/PlayerDisplayView';
import { NotFoundView } from './presentation/shared/views/NotFoundView';
import './presentation/shared/index.css';

export interface AppProps {
  initialEntries?: string[];
}

export const AppRoutes: React.FC = () => {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/admin" replace />} />
      <Route path="/admin" element={<AdminLayout />}>
        <Route index element={<AdminDashboardView />} />
        <Route path="*" element={<AdminDashboardView />} />
      </Route>
      <Route path="/player" element={<PlayerLayout />}>
        <Route index element={<PlayerDisplayView />} />
        <Route path="*" element={<PlayerDisplayView />} />
      </Route>
      <Route path="*" element={<NotFoundView />} />
    </Routes>
  );
};

export const App: React.FC<AppProps> = ({ initialEntries }) => {
  if (initialEntries && initialEntries.length > 0) {
    return (
      <MemoryRouter initialEntries={initialEntries}>
        <AppRoutes />
      </MemoryRouter>
    );
  }

  return (
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  );
};

export default App;
