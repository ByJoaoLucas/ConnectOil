import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import CollectionPointsPage from './page';

const invalidateMock = jest.fn();

jest.mock('@tanstack/react-query', () => ({
  useQuery: jest.fn(({ queryKey }: { queryKey: string[] }) => {
    // Default mock for ADMIN/COLLECTOR roles
    if (queryKey[0] === 'collectionPoints' && (!queryKey[1] || queryKey[1] !== 'my')) {
      return {
        data: {
          data: [{
            id: 'point-1',
            name: 'Posto Central',
            description: 'Ponto principal',
            address: 'Rua A, 100',
            city: 'João Pessoa',
            state: 'PB',
            status: 'ACTIVE',
            capacityLiters: 1000,
            currentVolumeLiters: 750,
            fillPercentage: 75,
            collectorName: 'João',
          }],
        },
        isLoading: false,
      };
    }
    // Mock for establishment - default with data
    if (queryKey[0] === 'collectionPoints' && queryKey[1] === 'my') {
      return {
        data: {
          data: [{
            id: 'point-1',
            name: 'Meu Posto',
            description: 'Meu ponto',
            address: 'Rua B, 200',
            city: 'São Paulo',
            state: 'SP',
            status: 'ACTIVE',
            capacityLiters: 500,
            currentVolumeLiters: 250,
            fillPercentage: 50,
            collectorName: null,
          }],
        },
        isLoading: false,
      };
    }
    return { data: { data: [] }, isLoading: false };
  }),
  useMutation: () => ({ mutate: jest.fn(), isPending: false }),
  useQueryClient: () => ({ invalidateQueries: invalidateMock }),
}));

jest.mock('@/lib/api', () => ({
  collectionPointsApi: {
    getAllList: jest.fn(),
    getMyPointsList: jest.fn(),
    create: jest.fn(),
    update: jest.fn(),
    delete: jest.fn(),
  },
}));

jest.mock('react-hot-toast', () => ({
  success: jest.fn(),
  error: jest.fn(),
}));

jest.mock('next/link', () => ({
  __esModule: true,
  default: ({ href, children, ...props }: any) =>
    React.createElement('a', { href, ...props }, children),
}));

jest.mock('@/contexts/AuthContext', () => ({
  useAuth: jest.fn(),
  AuthProvider: ({ children }: { children: React.ReactNode }) => children,
}));

const { useAuth } = require('@/contexts/AuthContext');

const mockEstablishmentUser = {
  id: 'user-1',
  name: 'Estabelecimento Teste',
  email: 'estab@test.com',
  role: 'ESTABLISHMENT',
  isActive: true,
};

const mockCollectorUser = {
  id: 'user-2',
  name: 'Coletor Teste',
  email: 'collector@test.com',
  role: 'COLLECTOR',
  isActive: true,
};

describe('CollectionPointsPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('for ADMIN/COLLECTOR roles', () => {
    beforeEach(() => {
      useAuth.mockReturnValue({ user: mockCollectorUser });
    });

    it('renders points and filters by search text', () => {
      render(<CollectionPointsPage />);

      expect(screen.getByText('Pontos de Coleta')).toBeInTheDocument();
      expect(screen.getByText('Posto Central')).toBeInTheDocument();

      fireEvent.change(
        screen.getByPlaceholderText('Buscar por nome, cidade, endereço...'),
        { target: { value: 'inexistente' } },
      );

      expect(screen.getByText('Nenhum ponto de coleta encontrado')).toBeInTheDocument();
    });

    it('filters by status', () => {
      render(<CollectionPointsPage />);

      fireEvent.change(screen.getByRole('combobox'), { target: { value: 'FULL' } });

      expect(screen.getByText('Nenhum ponto de coleta encontrado')).toBeInTheDocument();
    });

    it('opens the create point modal', () => {
      render(<CollectionPointsPage />);

      fireEvent.click(screen.getByText('Novo Ponto'));

      expect(screen.getByText('Novo Ponto de Coleta')).toBeInTheDocument();
    });

    it('shows delete button for admin/collector', () => {
      render(<CollectionPointsPage />);

      expect(screen.getByTitle('Excluir')).toBeInTheDocument();
    });
  });

  describe('for ESTABLISHMENT role', () => {
    beforeEach(() => {
      useAuth.mockReturnValue({ user: mockEstablishmentUser });
    });

    it('renders "Gerencie seus pontos" subtitle for establishment', () => {
      render(<CollectionPointsPage />);

      expect(screen.getByText('Gerencie seus pontos de coleta de óleo')).toBeInTheDocument();
    });

    it('shows only own collection points', () => {
      render(<CollectionPointsPage />);

      expect(screen.getByText('Meu Posto')).toBeInTheDocument();
      expect(screen.getByText('São Paulo/SP')).toBeInTheDocument();
    });

    it('shows "Nenhum ponto" message when no points exist for establishment', () => {
      // Override mock for empty data
      require('@tanstack/react-query').useQuery.mockImplementation(({ queryKey }: { queryKey: string[] }) => {
        if (queryKey[0] === 'collectionPoints' && queryKey[1] === 'my') {
          return { data: { data: [] }, isLoading: false };
        }
        return { data: { data: [] }, isLoading: false };
      });

      render(<CollectionPointsPage />);

      expect(screen.getByText('Você ainda não possui pontos de coleta cadastrados')).toBeInTheDocument();
      expect(screen.getByText('Cadastre seu primeiro ponto de coleta para começar a agendar coletas.')).toBeInTheDocument();
    });

    it('enables "Novo Ponto" button for establishment', () => {
      render(<CollectionPointsPage />);

      const newPointButton = screen.getByText('Novo Ponto');
      expect(newPointButton).not.toBeDisabled();
    });

    it('opens the create point modal for establishment', () => {
      render(<CollectionPointsPage />);

      fireEvent.click(screen.getByText('Novo Ponto'));

      expect(screen.getByText('Novo Ponto de Coleta')).toBeInTheDocument();
    });

    it('does not show delete button for establishment', () => {
      render(<CollectionPointsPage />);

      expect(screen.queryByTitle('Excluir')).not.toBeInTheDocument();
    });

    it('shows edit button disabled for PENDING_APPROVAL status', () => {
      // Override mock for pending approval point
      require('@tanstack/react-query').useQuery.mockImplementation(({ queryKey }: { queryKey: string[] }) => {
        if (queryKey[0] === 'collectionPoints' && queryKey[1] === 'my') {
          return {
            data: {
              data: [{
                id: 'point-1',
                name: 'Ponto Pendente',
                address: 'Rua C, 300',
                city: 'Rio de Janeiro',
                state: 'RJ',
                status: 'PENDING_APPROVAL',
                capacityLiters: 500,
                currentVolumeLiters: 0,
                fillPercentage: 0,
                collectorName: null,
              }],
            },
            isLoading: false,
          };
        }
        return { data: { data: [] }, isLoading: false };
      });

      render(<CollectionPointsPage />);

      // Should show disabled edit button
      const editButton = screen.getByTitle('Aguardando aprovação');
      expect(editButton).toBeDisabled();
    });
  });
});