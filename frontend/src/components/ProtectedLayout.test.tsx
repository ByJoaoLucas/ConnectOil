import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { ProtectedLayout } from './ProtectedLayout';

const replaceMock = jest.fn();

jest.mock('next/navigation', () => ({
  useRouter: () => ({
    replace: replaceMock,
  }),
}));

jest.mock('@/contexts/AuthContext', () => ({
  useAuth: jest.fn(),
}));

jest.mock('@/components/Layout', () => ({
  Layout: ({ children }: { children: React.ReactNode }) =>
    React.createElement('div', { 'data-testid': 'authenticated-layout' }, children),
}));

import { useAuth } from '@/contexts/AuthContext';

describe('ProtectedLayout', () => {
  beforeEach(() => {
    replaceMock.mockReset();
  });

  it('redirects unauthenticated users to login', async () => {
    (useAuth as jest.Mock).mockReturnValue({
      isAuthenticated: false,
      isLoading: false,
    });

    render(React.createElement(
      ProtectedLayout,
      null,
      React.createElement('div', null, 'Protected content'),
    ));

    await waitFor(() => {
      expect(replaceMock).toHaveBeenCalledWith('/login');
    });
  });

  it('renders authenticated content inside the application layout', () => {
    (useAuth as jest.Mock).mockReturnValue({
      isAuthenticated: true,
      isLoading: false,
    });

    render(React.createElement(
      ProtectedLayout,
      null,
      React.createElement('div', null, 'Protected content'),
    ));

    expect(screen.getByTestId('authenticated-layout')).toBeInTheDocument();
    expect(screen.getByText('Protected content')).toBeInTheDocument();
  });

  it('shows no redirect while authentication is loading', async () => {
    (useAuth as jest.Mock).mockReturnValue({
      isAuthenticated: false,
      isLoading: true,
    });

    const { container } = render(React.createElement(
      ProtectedLayout,
      null,
      React.createElement('div', null, 'Protected content'),
    ));

    expect(container.querySelector('.spinner')).toBeInTheDocument();
    await waitFor(() => {
      expect(replaceMock).not.toHaveBeenCalled();
    });
  });
});
