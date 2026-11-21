import React from 'react';
import { fireEvent, render, screen, waitFor, act } from '@testing-library/react';
import ProfilePage from './page';
import { AuthProvider } from '@/contexts/AuthContext';

const updateUserMock = jest.fn();
const deleteAccountMock = jest.fn();
const logoutMock = jest.fn();

jest.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({
    user: {
      id: '1',
      name: 'João Silva',
      email: 'joao@example.com',
      phoneNumber: '(11) 99999-9999',
      cpf: '12345678901',
      role: 'ESTABLISHMENT',
      isActive: true,
      createdAt: '2024-01-01T00:00:00.000Z',
    },
    updateUser: updateUserMock,
    deleteAccount: deleteAccountMock,
    logout: logoutMock,
  }),
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
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

describe('ProfilePage', () => {
  beforeEach(() => {
    updateUserMock.mockReset();
    deleteAccountMock.mockReset();
    logoutMock.mockReset();
  });

  const renderProfile = () => {
    return render(
      <AuthProvider>
        <ProfilePage />
      </AuthProvider>
    );
  };

  it('renders profile tab with user info', () => {
    renderProfile();

    expect(screen.getByText('Meu Perfil')).toBeInTheDocument();
    expect(screen.getByText('João Silva')).toBeInTheDocument();
    expect(screen.getByText('joao@example.com')).toBeInTheDocument();
    // Check for role badge (has bg-gray-100 class) - not the dropdown option
    const badges = screen.getAllByText('Estabelecimento');
    expect(badges.length).toBeGreaterThanOrEqual(1);
    const badge = badges.find(el => el.className.includes('bg-gray-100'));
    expect(badge).toBeInTheDocument();
  });

  it('renders Zona de Perigo for ESTABLISHMENT role', () => {
    renderProfile();

    expect(screen.getByText('Zona de Perigo')).toBeInTheDocument();
    expect(screen.getByText('Excluir minha conta')).toBeInTheDocument();
  });

  it('opens delete confirmation modal when clicking delete button', () => {
    renderProfile();

    fireEvent.click(screen.getByText('Excluir conta'));

    expect(screen.getByText('Confirmar exclusão da conta')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('DIGITE EXCLUIR')).toBeInTheDocument();
  });

  it('keeps delete button disabled until EXCLUIR is typed', () => {
    renderProfile();

    fireEvent.click(screen.getByText('Excluir conta'));

    const deleteButton = screen.getByText('Excluir definitivamente');
    expect(deleteButton).toBeDisabled();

    fireEvent.change(screen.getByPlaceholderText('DIGITE EXCLUIR'), {
      target: { value: 'EXCLUI' },
    });
    expect(deleteButton).toBeDisabled();

    fireEvent.change(screen.getByPlaceholderText('DIGITE EXCLUIR'), {
      target: { value: 'EXCLUIR' },
    });
    expect(deleteButton).not.toBeDisabled();
  });

  it('calls deleteAccount when EXCLUIR is typed and confirmed', async () => {
    deleteAccountMock.mockResolvedValue(undefined);
    renderProfile();

    fireEvent.click(screen.getByText('Excluir conta'));
    fireEvent.change(screen.getByPlaceholderText('DIGITE EXCLUIR'), {
      target: { value: 'EXCLUIR' },
    });
    fireEvent.click(screen.getByText('Excluir definitivamente'));

    await waitFor(() => {
      expect(deleteAccountMock).toHaveBeenCalled();
    });
  });

  it('does not call deleteAccount if confirmation text is wrong', () => {
    renderProfile();

    fireEvent.click(screen.getByText('Excluir conta'));
    fireEvent.change(screen.getByPlaceholderText('DIGITE EXCLUIR'), {
      target: { value: 'EXCLUI' },
    });
    fireEvent.click(screen.getByText('Excluir definitivamente'));

    expect(deleteAccountMock).not.toHaveBeenCalled();
  });

  it('closes modal when clicking cancel', () => {
    renderProfile();

    fireEvent.click(screen.getByText('Excluir conta'));
    expect(screen.getByText('Confirmar exclusão da conta')).toBeInTheDocument();

    fireEvent.click(screen.getByText('Cancelar'));
    expect(screen.queryByText('Confirmar exclusão da conta')).not.toBeInTheDocument();
  });

  it('closes modal when clicking overlay', () => {
    renderProfile();

    fireEvent.click(screen.getByText('Excluir conta'));
    expect(screen.getByText('Confirmar exclusão da conta')).toBeInTheDocument();

    // Click on the overlay (the fixed div)
    fireEvent.click(document.querySelector('.fixed.inset-0') as HTMLElement);
    expect(screen.queryByText('Confirmar exclusão da conta')).not.toBeInTheDocument();
  });

  it('shows password tab when clicked', () => {
    renderProfile();

    fireEvent.click(screen.getByText('Segurança'));
    expect(screen.getByText(/Senha Atual/)).toBeInTheDocument();
    expect(screen.getByText(/Confirmar Nova Senha/)).toBeInTheDocument();
  });
});