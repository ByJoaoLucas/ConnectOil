import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import SchedulesPage from './page';

jest.mock('@tanstack/react-query', () => ({
  useQuery: jest.fn(({ queryKey }: { queryKey: string[] }) => {
    // Default mock for ADMIN/COLLECTOR roles
    if (queryKey[0] === 'schedules' && (!queryKey[1] || queryKey[1] !== 'my')) {
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

    if (queryKey[0] === 'collectionPoints' && (!queryKey[1] || queryKey[1] !== 'my')) {
      return { data: { data: [{ id: 'point-1', name: 'Posto Central', city: 'São Paulo', state: 'SP', status: 'ACTIVE' }] } };
    }

    // Mock for establishment
    if (queryKey[0] === 'schedules' && queryKey[1] === 'my') {
      return {
        data: {
          data: [{
            id: 'schedule-1',
            collectionPointId: 'point-1',
            collectionPointName: 'Meu Posto',
            collectorId: null,
            collectorName: null,
            scheduledDate: '2026-11-20',
            startTime: '08:00',
            endTime: '10:00',
            status: 'PENDING',
            estimatedVolumeLiters: 100,
            isOverdue: false,
            isToday: true,
            saleType: 'SALE',
            timeWindow: 'MORNING',
          }],
        },
        isLoading: false,
      };
    }

    if (queryKey[0] === 'collectionPoints' && queryKey[1] === 'my') {
      return {
        data: { data: [{ id: 'point-1', name: 'Meu Posto', city: 'São Paulo', state: 'SP', status: 'ACTIVE' }] },
        isLoading: false,
      };
    }

    return { data: { data: [{ id: 'collector-1', name: 'João', email: 'joao@test.com', isActive: true }] }, isLoading: false };
  }),
  useMutation: () => ({ mutate: jest.fn(), isPending: false }),
  useQueryClient: () => ({ invalidateQueries: jest.fn() }),
}));

jest.mock('@/lib/api', () => ({
  schedulesApi: {
    getAllList: jest.fn(),
    getMySchedulesList: jest.fn(),
    create: jest.fn(),
    update: jest.fn(),
    delete: jest.fn(),
    confirm: jest.fn(),
    start: jest.fn(),
    complete: jest.fn(),
    cancel: jest.fn(),
  },
  collectionPointsApi: {
    getAllList: jest.fn(),
    getMyPointsList: jest.fn(),
  },
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

describe('SchedulesPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('for ADMIN/COLLECTOR roles', () => {
    beforeEach(() => {
      useAuth.mockReturnValue({ user: mockCollectorUser });
    });

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

    it('hides collector field for COLLECTOR role (only ADMIN can choose collector)', () => {
      render(<SchedulesPage />);

      fireEvent.click(screen.getAllByText('Novo Agendamento')[0]);

      expect(screen.queryByLabelText('Coletor *')).not.toBeInTheDocument();
    });

    it('does not show saleType field for non-establishment', () => {
      render(<SchedulesPage />);

      fireEvent.click(screen.getAllByText('Novo Agendamento')[0]);

      expect(screen.queryByLabelText('Tipo de Operação *')).not.toBeInTheDocument();
    });
  });

  describe('for ESTABLISHMENT role', () => {
    beforeEach(() => {
      useAuth.mockReturnValue({ user: mockEstablishmentUser });
    });

    it('renders "Meus agendamentos" subtitle for establishment', () => {
      render(<SchedulesPage />);

      expect(screen.getByText('Gerencie seus agendamentos de coleta')).toBeInTheDocument();
    });

    it('shows only own collection points in the form', () => {
      render(<SchedulesPage />);

      fireEvent.click(screen.getAllByText('Novo Agendamento')[0]);

      expect(screen.getByLabelText('Ponto de Coleta *')).toBeInTheDocument();
      // Should have the establishment's point
      const select = screen.getByLabelText('Ponto de Coleta *');
      expect(select).toHaveValue('');
      fireEvent.click(select);
      // Wait for options to render
      expect(screen.getByText('Meu Posto - São Paulo/SP')).toBeInTheDocument();
    });

    it('hides collector field for establishment', () => {
      render(<SchedulesPage />);

      fireEvent.click(screen.getAllByText('Novo Agendamento')[0]);

      expect(screen.queryByLabelText('Coletor *')).not.toBeInTheDocument();
    });

    it('shows saleType field for establishment', () => {
      render(<SchedulesPage />);

      fireEvent.click(screen.getAllByText('Novo Agendamento')[0]);

      expect(screen.getByLabelText('Tipo de Operação *')).toBeInTheDocument();
      expect(screen.getByLabelText('Tipo de Operação *')).toHaveValue('SALE');
    });

    it('shows timeWindow field for establishment', () => {
      render(<SchedulesPage />);

      fireEvent.click(screen.getAllByText('Novo Agendamento')[0]);

      expect(screen.getByLabelText('Janela de Horário')).toBeInTheDocument();
    });

    it('shows saleType and timeWindow columns in table for establishment', () => {
      render(<SchedulesPage />);

      expect(screen.getByText('Tipo')).toBeInTheDocument();
      expect(screen.getByText('Janela')).toBeInTheDocument();
    });

    it('does not show collector column in table for establishment', () => {
      render(<SchedulesPage />);

      // Should not have "Coletor" column header
      expect(screen.queryByText('Coletor')).not.toBeInTheDocument();
    });

    it('shows Venda badge for SALE type', () => {
      render(<SchedulesPage />);

      expect(screen.getByText('Venda')).toBeInTheDocument();
    });

    it('shows time window label in table', () => {
      render(<SchedulesPage />);

      expect(screen.getByText('Manhã (08:00-12:00)')).toBeInTheDocument();
    });

    it('opens cancel modal with 2-hour warning for establishment', () => {
      render(<SchedulesPage />);

      // Click cancel button for the schedule
      fireEvent.click(screen.getAllByTitle('Cancelar')[0]);

      expect(screen.getByText('Cancelar Agendamento')).toBeInTheDocument();
      expect(screen.getByText(/pelo menos 2 horas de antecedência/i)).toBeInTheDocument();
    });

    it('disables cancel confirm button without reason', () => {
      render(<SchedulesPage />);

      fireEvent.click(screen.getAllByTitle('Cancelar')[0]);

      const confirmButton = screen.getByText('Confirmar Cancelamento');
      expect(confirmButton).toBeDisabled();
    });

    it('enables cancel confirm button with reason', () => {
      render(<SchedulesPage />);

      fireEvent.click(screen.getAllByTitle('Cancelar')[0]);

      fireEvent.change(screen.getByPlaceholderText(/Emergência no estabelecimento/), {
        target: { value: 'Teste de cancelamento' },
      });

      const confirmButton = screen.getByText('Confirmar Cancelamento');
      expect(confirmButton).not.toBeDisabled();
    });
  });
});