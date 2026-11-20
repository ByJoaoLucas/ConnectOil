import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import RecordsPage from './page';

jest.mock('@tanstack/react-query', () => ({
  useQuery: ({ queryKey }: { queryKey: string[] }) => {
    if (queryKey[0] === 'records') {
      return {
        data: {
          data: [{
            id: 'record-1',
            collectionPointId: 'point-1',
            collectionPointName: 'Posto Central',
            collectorId: 'collector-1',
            collectorName: 'João',
            collectionDate: '2026-11-20T09:00:00',
            volumeCollectedLiters: 120,
            verifiedByManager: false,
            notes: 'Coleta normal',
          }],
        },
        isLoading: false,
      };
    }

    if (queryKey[0] === 'collectionPoints') {
      return { data: { data: [{ id: 'point-1', name: 'Posto Central' }] } };
    }

    return { data: { data: [{ id: 'collector-1', name: 'João' }] } };
  },
  useMutation: () => ({ mutate: jest.fn(), isPending: false }),
  useQueryClient: () => ({ invalidateQueries: jest.fn() }),
}));

jest.mock('@/lib/api', () => ({
  recordsApi: {
    getAllList: jest.fn(),
    create: jest.fn(),
    update: jest.fn(),
    delete: jest.fn(),
    verify: jest.fn(),
  },
  collectionPointsApi: { getAllList: jest.fn() },
  usersApi: { getCollectors: jest.fn() },
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

describe('RecordsPage', () => {
  it('renders records and filters by search', () => {
    render(<RecordsPage />);

    expect(screen.getByText('Registros de Coleta')).toBeInTheDocument();
    expect(screen.getAllByText('Posto Central').length).toBeGreaterThan(0);

    fireEvent.change(
      screen.getByPlaceholderText('Buscar por ponto, coletor, observações...'),
      { target: { value: 'inexistente' } },
    );

    expect(screen.getByText('Nenhum registro encontrado')).toBeInTheDocument();
  });

  it('opens the create record modal', () => {
    render(<RecordsPage />);

    fireEvent.click(screen.getByText('Novo Registro'));

    expect(screen.getByText('Novo Registro de Coleta')).toBeInTheDocument();
  });
});
