import React from 'react';
import { render, screen } from '@testing-library/react';
import DashboardPage from './page';

jest.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({
    user: {
      id: 'collector-1',
      name: 'João',
      role: 'COLLECTOR',
      email: 'joao@example.com',
      isActive: true,
    },
  }),
}));

jest.mock('@/lib/api', () => ({
  collectionPointsApi: { getAllList: jest.fn() },
  schedulesApi: { getAllList: jest.fn() },
  recordsApi: { getAllList: jest.fn() },
}));

jest.mock('@tanstack/react-query', () => ({
  useQuery: ({ queryKey }: { queryKey: string[] }) => {
    if (queryKey[0] === 'collectionPoints') {
      return {
        data: {
          data: [
            { id: 'point-1', status: 'ACTIVE' },
            { id: 'point-2', status: 'FULL' },
          ],
        },
      };
    }

    if (queryKey[0] === 'schedules') {
      return {
        data: {
          data: [{
            id: 'schedule-1',
            collectionPointId: 'point-1',
            collectionPointName: 'Posto Central',
            collectorId: 'collector-1',
            scheduledDate: '2026-11-20',
            startTime: '08:00',
            endTime: '10:00',
            status: 'CONFIRMED',
            isToday: true,
            isOverdue: false,
          }],
        },
      };
    }

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
        }],
      },
    };
  },
}));

describe('DashboardPage', () => {
  it('shows summary cards for collection data', () => {
    render(<DashboardPage />);

    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Total de Pontos')).toBeInTheDocument();
    expect(screen.getByText('Pontos Ativos')).toBeInTheDocument();
    expect(screen.getByText('Pontos Cheios')).toBeInTheDocument();
    expect(screen.getByText('Agendamentos Hoje')).toBeInTheDocument();
  });

  it('renders recent schedules and collection records', () => {
    render(<DashboardPage />);

    expect(screen.getByText('Próximos Agendamentos')).toBeInTheDocument();
    expect(screen.getByText('Últimas Coletas')).toBeInTheDocument();
    expect(screen.getAllByText('Posto Central').length).toBeGreaterThan(0);
    expect(screen.getByText('Pendente')).toBeInTheDocument();
  });

  it('applies collector filters to schedules and records', () => {
    render(<DashboardPage />);

    expect(screen.getByText('120')).toBeInTheDocument();
  });
});
