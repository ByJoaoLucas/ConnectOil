import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import RegisterPage from './page';

const registerMock = jest.fn();
const pushMock = jest.fn();
const refreshMock = jest.fn();

jest.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ register: registerMock }),
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

describe('RegisterPage', () => {
  beforeEach(() => {
    registerMock.mockReset();
    pushMock.mockReset();
    refreshMock.mockReset();
  });

  it('renders collector registration fields', () => {
    render(<RegisterPage />);

    expect(screen.getByText('Crie sua conta para começar')).toBeInTheDocument();
    expect(screen.getByText('Sou coletor')).toBeInTheDocument();
    expect(screen.getByLabelText('CPF')).toBeInTheDocument();
  });

  it('switches to establishment fields', () => {
    render(<RegisterPage />);

    fireEvent.click(screen.getByText('Sou estabelecimento'));

    expect(screen.getByLabelText('CNPJ')).toBeInTheDocument();
    expect(screen.getByLabelText('Nome do estabelecimento')).toBeInTheDocument();
  });

  it('shows validation errors for invalid collector data', async () => {
    render(<RegisterPage />);

    fireEvent.click(screen.getByText('Criar conta'));

    await waitFor(() => {
      expect(screen.getByText('Nome deve ter pelo menos 2 caracteres')).toBeInTheDocument();
      expect(screen.getByText('E-mail inválido')).toBeInTheDocument();
      expect(screen.getByText('CPF deve conter exatamente 11 dígitos')).toBeInTheDocument();
    });
  });

  it('submits collector registration and redirects', async () => {
    registerMock.mockResolvedValue(undefined);
    render(<RegisterPage />);

    fireEvent.change(screen.getByLabelText('Nome completo'), { target: { value: 'Maria Silva' } });
    fireEvent.change(screen.getByLabelText('E-mail'), { target: { value: 'maria@example.com' } });
    fireEvent.change(screen.getByLabelText('CPF'), { target: { value: '12345678901' } });
    fireEvent.change(screen.getByLabelText('Senha'), { target: { value: 'Senha@123' } });
    fireEvent.change(screen.getByLabelText('Confirmar senha'), { target: { value: 'Senha@123' } });

    fireEvent.click(screen.getByText('Criar conta'));

    await waitFor(() => {
      expect(registerMock).toHaveBeenCalledWith(expect.objectContaining({
        name: 'Maria Silva',
        email: 'maria@example.com',
        cpf: '12345678901',
        role: 'COLLECTOR',
      }));
      expect(pushMock).toHaveBeenCalledWith('/dashboard');
      expect(refreshMock).toHaveBeenCalled();
    });
  });
});
