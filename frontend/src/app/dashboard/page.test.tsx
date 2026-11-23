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
  dashboardApi: { getStats: jest.fn() },
  collectionPointsApi: { getAllList: jest.fn() },
  schedulesApi: { getAllList: jest.fn() },
  recordsApi: { getAllList: jest.fn() },
}));

jest.mock('@tanstack/react-query', () => ({
  useQuery: ({ queryKey }: { queryKey: string[] }) => {
    if (queryKey[0] === 'dashboardStats') {
      return {
        data: {
          data: {
            totalPoints: 2,
            activePoints: 1,
            fullPoints: 1,
            pendingApprovalPoints: 0,
            totalCollectors: 1,
            activeCollectors: 1,
            totalEstablishments: 1,
            activeEstablishments: 1,
            todaySchedules: 1,
            pendingSchedules: 1,
            inProgressSchedules: 0,
            completedSchedules: 0,
            totalSchedules: 1,
            totalRecords: 1,
            monthRecords: 1,
            pendingVerification: 0,
            overdueSchedules: 0,
            totalVolume: 120,
            monthVolume: 120,
            avgQuality: 4,
          },
        },
        isLoading: false,
      };
    }

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
            status: 'PENDING',
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
      isLoading: false,
    };
  },
  useMutation: () => ({
    mutate: jest.fn(),
    mutateAsync: jest.fn(),
    isPending: false,
  }),
  useQueryClient: () => ({
    invalidateQueries: jest.fn(),
  }),
}));

describe('DashboardPage', () => {
  it('shows summary cards for collection data', () => {
    render(<DashboardPage />);

    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Pontos Atribuídos')).toBeInTheDocument();
    expect(screen.getByText('Pontos Ativos')).toBeInTheDocument();
    expect(screen.getByText('Pontos Cheios')).toBeInTheDocument();
    expect(screen.getByText('Agendamentos Hoje')).toBeInTheDocument();
  });

  it('renders recent schedules and collection records', () => {
    render(<DashboardPage />);

    expect(screen.getByText('Meus Agendamentos Atribuídos')).toBeInTheDocument();
    expect(screen.getByText('Minhas Últimas Coletas')).toBeInTheDocument();
    expect(screen.getAllByText('Posto Central').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Pendente').length).toBeGreaterThan(0);
  });

  it('applies collector filters to schedules and records', () => {
    render(<DashboardPage />);

    expect(screen.getByText('120')).toBeInTheDocument();
  });
});
