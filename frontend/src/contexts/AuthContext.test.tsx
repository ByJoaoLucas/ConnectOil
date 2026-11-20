import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { AuthProvider, useAuth } from './AuthContext';
import { api } from '@/lib/api';

jest.mock('@/lib/api', () => ({
  api: {
    get: jest.fn(),
    post: jest.fn(),
    defaults: { headers: { common: {} } },
  },
}));

const testUser = {
  id: 'user-1',
  name: 'Maria',
  email: 'maria@example.com',
  role: 'COLLECTOR',
  isActive: true,
};

function AuthConsumer() {
  const auth = useAuth();

  return (
    <div>
      <span data-testid="loading">{String(auth.isLoading)}</span>
      <span data-testid="authenticated">{String(auth.isAuthenticated)}</span>
      <span data-testid="user">{auth.user?.name || 'none'}</span>
      <button onClick={() => auth.login('maria@example.com', 'Senha123')}>login</button>
      <button onClick={() => auth.register({
        name: 'Maria',
        email: 'maria@example.com',
        password: 'Senha123',
        cpf: '12345678901',
        role: 'COLLECTOR',
      })}>register</button>
      <button onClick={auth.logout}>logout</button>
      <button onClick={() => { void auth.refreshAccessToken().catch(() => undefined); }}>refresh</button>
      <button onClick={() => auth.updateUser({ name: 'Maria Atualizada' })}>update</button>
    </div>
  );
}

function renderAuth() {
  return render(
    <AuthProvider>
      <AuthConsumer />
    </AuthProvider>,
  );
}

describe('AuthProvider', () => {
  beforeEach(() => {
    localStorage.clear();
    jest.clearAllMocks();
  });

  it('starts unauthenticated and finishes loading', async () => {
    renderAuth();

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });
    expect(screen.getByTestId('authenticated')).toHaveTextContent('false');
  });

  it('logs in and persists the session', async () => {
    (api.post as jest.Mock).mockResolvedValue({
      data: {
        accessToken: 'access',
        refreshToken: 'refresh',
        tokenType: 'Bearer',
        expiresIn: 3600,
        user: testUser,
      },
    });

    renderAuth();
    await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'));

    fireEvent.click(screen.getByText('login'));

    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('true');
      expect(screen.getByTestId('user')).toHaveTextContent('Maria');
    });
    expect(localStorage.getItem('accessToken')).toBe('access');
    expect(localStorage.getItem('refreshToken')).toBe('refresh');
  });

  it('registers a user and stores the returned session', async () => {
    (api.post as jest.Mock).mockResolvedValue({
      data: {
        accessToken: 'registered-access',
        refreshToken: 'registered-refresh',
        tokenType: 'Bearer',
        expiresIn: 3600,
        user: testUser,
      },
    });

    renderAuth();
    await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'));

    fireEvent.click(screen.getByText('register'));

    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('true');
    });
    expect(api.post).toHaveBeenCalledWith('/auth/register', expect.objectContaining({
      role: 'COLLECTOR',
    }));
  });

  it('recovers a stored session', async () => {
    localStorage.setItem('accessToken', 'stored-access');
    localStorage.setItem('refreshToken', 'stored-refresh');
    localStorage.setItem('user', JSON.stringify(testUser));
    (api.get as jest.Mock).mockResolvedValue({ data: testUser });

    renderAuth();

    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('true');
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });
    expect(api.get).toHaveBeenCalledWith('/auth/me');
  });

  it('refreshes an expired session', async () => {
    localStorage.setItem('refreshToken', 'old-refresh');
    (api.post as jest.Mock).mockResolvedValue({
      data: {
        accessToken: 'new-access',
        refreshToken: 'new-refresh',
        tokenType: 'Bearer',
        expiresIn: 3600,
        user: testUser,
      },
    });

    renderAuth();
    await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'));

    fireEvent.click(screen.getByText('refresh'));

    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('true');
    });
    expect(api.post).toHaveBeenCalledWith('/auth/refresh', null, {
      params: { refreshToken: 'old-refresh' },
    });
  });

  it('throws when refreshing without a refresh token', async () => {
    renderAuth();
    await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'));

    fireEvent.click(screen.getByText('refresh'));

    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('false');
    });
  });

  it('logs out and removes persisted session', async () => {
    localStorage.setItem('accessToken', 'access');
    localStorage.setItem('refreshToken', 'refresh');
    localStorage.setItem('user', JSON.stringify(testUser));
    (api.get as jest.Mock).mockResolvedValue({ data: testUser });

    renderAuth();
    await waitFor(() => expect(screen.getByTestId('authenticated')).toHaveTextContent('true'));

    fireEvent.click(screen.getByText('logout'));

    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('false');
      expect(screen.getByTestId('user')).toHaveTextContent('none');
    });
    expect(localStorage.getItem('accessToken')).toBeNull();
  });

  it('updates the in-memory and persisted user', async () => {
    localStorage.setItem('accessToken', 'access');
    localStorage.setItem('refreshToken', 'refresh');
    localStorage.setItem('user', JSON.stringify(testUser));
    (api.get as jest.Mock).mockResolvedValue({ data: testUser });

    renderAuth();
    await waitFor(() => expect(screen.getByTestId('authenticated')).toHaveTextContent('true'));

    fireEvent.click(screen.getByText('update'));

    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('Maria Atualizada');
    });
    expect(JSON.parse(localStorage.getItem('user') || '{}').name).toBe('Maria Atualizada');
  });
});
