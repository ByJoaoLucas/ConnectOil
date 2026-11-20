import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import CollectionPointsPage from './page';

const invalidateMock = jest.fn();

jest.mock('@tanstack/react-query', () => ({
  useQuery: () => ({
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
  }),
  useMutation: () => ({ mutate: jest.fn(), isPending: false }),
  useQueryClient: () => ({ invalidateQueries: invalidateMock }),
}));

jest.mock('@/lib/api', () => ({
  collectionPointsApi: {
    getAllList: jest.fn(),
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

describe('CollectionPointsPage', () => {
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
});
