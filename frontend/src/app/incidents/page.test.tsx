import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import IncidentsPage from './page';

jest.mock('@tanstack/react-query', () => ({
  useQuery: ({ queryKey }: { queryKey: string[] }) => {
    if (queryKey[0] === 'incidents') {
      return {
        data: {
          data: [{
            id: 'incident-1',
            collectionPointId: 'point-1',
            collectionPointName: 'Posto Central',
            incidentType: 'Vazamento',
            description: 'Vazamento no compartimento',
            severity: 'HIGH',
            status: 'OPEN',
            reportedAt: '2026-11-20T09:00:00',
          }],
        },
        isLoading: false,
      };
    }

    return { data: { data: [{ id: 'point-1', name: 'Posto Central' }] } };
  },
  useMutation: () => ({ mutate: jest.fn(), isPending: false }),
  useQueryClient: () => ({ invalidateQueries: jest.fn() }),
}));

jest.mock('@/lib/api', () => ({
  incidentsApi: {
    getAllList: jest.fn(),
    create: jest.fn(),
    update: jest.fn(),
    resolve: jest.fn(),
    delete: jest.fn(),
  },
  collectionPointsApi: { getAllList: jest.fn() },
  usersApi: { getCollectors: jest.fn() },
}));

jest.mock('react-hot-toast', () => ({
  success: jest.fn(),
  error: jest.fn(),
}));

describe('IncidentsPage', () => {
  it('renders incidents and filters by search', () => {
    render(<IncidentsPage />);

    expect(screen.getByText('Incidentes')).toBeInTheDocument();
    expect(screen.getAllByText('Posto Central').length).toBeGreaterThan(0);

    fireEvent.change(
      screen.getByPlaceholderText('Buscar por tipo, descrição, ponto...'),
      { target: { value: 'inexistente' } },
    );

    expect(screen.getByText('Nenhum incidente encontrado')).toBeInTheDocument();
  });

  it('filters incidents by status and severity', () => {
    render(<IncidentsPage />);

    const selects = screen.getAllByRole('combobox');
    fireEvent.change(selects[0], { target: { value: 'RESOLVED' } });
    fireEvent.change(selects[1], { target: { value: 'CRITICAL' } });

    expect(screen.getByText('Nenhum incidente encontrado')).toBeInTheDocument();
  });

  it('opens the incident report modal', () => {
    render(<IncidentsPage />);

    fireEvent.click(screen.getByText('Reportar Incidente'));

    expect(screen.getByText('Reportar Novo Incidente')).toBeInTheDocument();
  });
});
