import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import App from '../App';

describe('App Route Navigation', () => {
  it('renders admin layout on /admin route', async () => {
    render(<App initialEntries={['/admin']} />);

    expect(await screen.findByText('Kiosko Retail Admin')).toBeInTheDocument();
    expect(screen.getByText('Panel de Control')).toBeInTheDocument();
    expect(screen.getByText('Core Backend (Spring Modulith)')).toBeInTheDocument();
  });

  it('renders player layout on /player route', async () => {
    render(<App initialEntries={['/player']} />);

    expect(await screen.findByText('Ad Player Kiosk')).toBeInTheDocument();
    expect(screen.getByText('Publicidad Inteligente Kiosko')).toBeInTheDocument();
  });

  it('redirects root route / to /admin', async () => {
    render(<App initialEntries={['/']} />);

    expect(await screen.findByText('Kiosko Retail Admin')).toBeInTheDocument();
  });
});
