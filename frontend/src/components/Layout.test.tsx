import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { Layout } from './Layout';

const pushMock = jest.fn();
const refreshMock = jest.fn();
const logoutMock = jest.fn();

jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: pushMock,
    refresh: refreshMock,
  }),
  usePathname: () => '/dashboard',
}));

jest.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({
    user: {
      id: 'admin-1',
      name: 'Administrador',
      role: 'ADMIN',
      email: 'admin@example.com',
      isActive: true,
    },
    logout: logoutMock,
  }),
}));

describe('Layout', () => {
  beforeEach(() => {
    pushMock.mockReset();
    refreshMock.mockReset();
    logoutMock.mockReset();
  });

  it('renders navigation for an administrator', () => {
    render(
      <Layout>
        <div>Conteudo</div>
      </Layout>,
    );

    expect(screen.getAllByText('Administrador').length).toBeGreaterThan(0);
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Usuários')).toBeInTheDocument();
    expect(screen.getByText('Conteudo')).toBeInTheDocument();
  });

  it('marks the current route as active', () => {
    render(
      <Layout>
        <div>Conteudo</div>
      </Layout>,
    );

    expect(screen.getByText('Dashboard').closest('a')).toHaveClass('bg-primary-50');
  });

  it('logs out and redirects to login', () => {
    render(
      <Layout>
        <div>Conteudo</div>
      </Layout>,
    );

    fireEvent.click(screen.getByTitle('Sair'));

    expect(logoutMock).toHaveBeenCalledTimes(1);
    expect(pushMock).toHaveBeenCalledWith('/login');
    expect(refreshMock).toHaveBeenCalledTimes(1);
  });

  it('toggles the sidebar state', () => {
    render(
      <Layout>
        <div>Conteudo</div>
      </Layout>,
    );

    const desktopToggle = screen.getAllByRole('button', { name: 'Recolher menu' })[0];
    fireEvent.click(desktopToggle);

    expect(screen.getByRole('button', { name: 'Expandir menu' })).toBeInTheDocument();
  });
});
