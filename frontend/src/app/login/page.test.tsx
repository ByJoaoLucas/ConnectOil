import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import LoginPage from './page';

const loginMock = jest.fn();
const pushMock = jest.fn();
const refreshMock = jest.fn();

jest.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ login: loginMock }),
}));

jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: pushMock,
    refresh: refreshMock,
  }),
}));

jest.mock('next/link', () => ({
  __esModule: true,
  default: ({ href, children, ...props }: any) =>
    React.createElement('a', { href, ...props }, children),
}));

jest.mock('react-hot-toast', () => ({
  success: jest.fn(),
  error: jest.fn(),
}));

describe('LoginPage', () => {
  beforeEach(() => {
    loginMock.mockReset();
    pushMock.mockReset();
    refreshMock.mockReset();
  });

  it('renders the login form', () => {
    render(<LoginPage />);

    expect(screen.getByText('Sistema de Coleta de Óleo')).toBeInTheDocument();
    expect(screen.getByLabelText('E-mail')).toBeInTheDocument();
    expect(screen.getByLabelText('Senha')).toBeInTheDocument();
  });

  it('toggles password visibility', () => {
    render(<LoginPage />);

    const password = screen.getByLabelText('Senha');
    expect(password).toHaveAttribute('type', 'password');

    fireEvent.click(screen.getAllByRole('button')[0]);

    expect(password).toHaveAttribute('type', 'text');
  });

  it('submits credentials and redirects after login', async () => {
    loginMock.mockResolvedValue(undefined);
    render(<LoginPage />);

    fireEvent.change(screen.getByLabelText('E-mail'), {
      target: { value: 'admin@example.com' },
    });
    fireEvent.change(screen.getByLabelText('Senha'), {
      target: { value: 'admin123' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    await waitFor(() => {
      expect(loginMock).toHaveBeenCalledWith('admin@example.com', 'admin123');
      expect(pushMock).toHaveBeenCalledWith('/dashboard');
      expect(refreshMock).toHaveBeenCalled();
    });
  });

  it('shows validation errors for invalid input', async () => {
    render(<LoginPage />);

    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    await waitFor(() => {
      expect(screen.getByText('E-mail inválido')).toBeInTheDocument();
      expect(screen.getByText('Senha deve ter pelo menos 6 caracteres')).toBeInTheDocument();
    });
  });
});
