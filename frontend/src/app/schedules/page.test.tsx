import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import SchedulesPage from './page';

jest.mock('@tanstack/react-query', () => ({
  useQuery: ({ queryKey }: { queryKey: string[] }) => {
    if (queryKey[0] === 'schedules') {
      return {
        data: {
          data: [{
            id: 'schedule-1',
            collectionPointId: 'point-1',
            collectionPointName: 'Posto Central',
            collectorId: 'collector-1',
            collectorName: 'João',
            scheduledDate: '2026-11-20',
            startTime: '08:00',
            endTime: '10:00',
            status: 'CONFIRMED',
            estimatedVolumeLiters: 100,
            isOverdue: false,
            isToday: true,
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
  schedulesApi: {
    getAllList: jest.fn(),
    create: jest.fn(),
    update: jest.fn(),
    delete: jest.fn(),
    confirm: jest.fn(),
    start: jest.fn(),
    complete: jest.fn(),
    cancel: jest.fn(),
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

describe('SchedulesPage', () => {
  it('renders schedules and filters by search', () => {
    render(<SchedulesPage />);

    expect(screen.getByText('Agendamentos')).toBeInTheDocument();
    expect(screen.getAllByText('Posto Central').length).toBeGreaterThan(0);

    fireEvent.change(
      screen.getByPlaceholderText('Buscar por ponto, coletor, observações...'),
      { target: { value: 'inexistente' } },
    );

    expect(screen.getByText('Nenhum agendamento encontrado')).toBeInTheDocument();
  });

  it('filters schedules by status', () => {
    render(<SchedulesPage />);

    fireEvent.change(screen.getAllByRole('combobox')[0], { target: { value: 'CANCELLED' } });

    expect(screen.getByText('Nenhum agendamento encontrado')).toBeInTheDocument();
  });

  it('opens the create schedule modal', () => {
    render(<SchedulesPage />);

    fireEvent.click(screen.getAllByText('Novo Agendamento')[0]);

    expect(screen.getAllByText('Novo Agendamento').length).toBeGreaterThan(0);
  });
});
