import { createBrowserRouter, createMemoryRouter, Navigate, RouteObject } from 'react-router-dom';
import { AdminLayout } from './presentation/admin/layouts/AdminLayout';
import { AdminDashboardView } from './presentation/admin/views/AdminDashboardView';
import { PlayerLayout } from './presentation/player/layouts/PlayerLayout';
import { PlayerDisplayView } from './presentation/player/views/PlayerDisplayView';
import { NotFoundView } from './presentation/shared/views/NotFoundView';

export const routes: RouteObject[] = [
  {
    path: '/',
    element: <Navigate to="/admin" replace />,
  },
  {
    path: '/admin',
    element: <AdminLayout />,
    children: [
      {
        index: true,
        element: <AdminDashboardView />,
      },
      {
        path: '*',
        element: <AdminDashboardView />,
      },
    ],
  },
  {
    path: '/player',
    element: <PlayerLayout />,
    children: [
      {
        index: true,
        element: <PlayerDisplayView />,
      },
      {
        path: '*',
        element: <PlayerDisplayView />,
      },
    ],
  },
  {
    path: '*',
    element: <NotFoundView />,
  },
];

export const createRouter = (initialEntries?: string[]) => {
  if (initialEntries && initialEntries.length > 0) {
    return createMemoryRouter(routes, { initialEntries });
  }
  return createBrowserRouter(routes);
};
