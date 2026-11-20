import React from 'react';
import { render, screen } from '@testing-library/react';
import CollectionPointDetailPage from './page';

const pushMock = jest.fn();
const backMock = jest.fn();

let pointLoading = false;
let pointError: Error | null = null;
let pointData: any = {
  data: {
    id: 'point-1',
    name: 'Posto Central',
    address: 'Rua A, 100',
    neighborhood: 'Centro',
    city: 'João Pessoa',
    state: 'PB',
    zipCode: '58000000',
    latitude: -7.115,
    longitude: -34.864,
    status: 'ACTIVE',
    capacityLiters: 1000,
    currentVolumeLiters: 750,
    fillPercentage: 75,
    collectorName: 'João',
    createdAt: '2026-11-20T09:00:00',
    updatedAt: '2026-11-20T10:00:00',
  },
};

jest.mock('next/navigation', () => ({
  useParams: () => ({ id: 'point-1' }),
  useRouter: () => ({
    push: pushMock,
    back: backMock,
  }),
}));

jest.mock('@tanstack/react-query', () => ({
  useQuery: ({ queryKey }: { queryKey: string[] }) => {
    if (queryKey[0] === 'collectionPoint') {
      return { data: pointData, isLoading: pointLoading, error: pointError };
    }
    return { data: { data: [] }, isLoading: false };
  },
  useMutation: () => ({ mutate: jest.fn(), isPending: false }),
  useQueryClient: () => ({ invalidateQueries: jest.fn() }),
}));

jest.mock('@/lib/api', () => ({
  collectionPointsApi: {
    getById: jest.fn(),
    delete: jest.fn(),
  },
  schedulesApi: { getByPoint: jest.fn() },
  recordsApi: { getByPoint: jest.fn() },
}));

jest.mock('react-hot-toast', () => ({
  success: jest.fn(),
  error: jest.fn(),
}));

describe('CollectionPointDetailPage', () => {
  beforeEach(() => {
    pointLoading = false;
    pointError = null;
    pointData = {
      data: {
        id: 'point-1',
        name: 'Posto Central',
        address: 'Rua A, 100',
        neighborhood: 'Centro',
        city: 'João Pessoa',
        state: 'PB',
        zipCode: '58000000',
        latitude: -7.115,
        longitude: -34.864,
        status: 'ACTIVE',
        capacityLiters: 1000,
        currentVolumeLiters: 750,
        fillPercentage: 75,
        collectorName: 'João',
        createdAt: '2026-11-20T09:00:00',
        updatedAt: '2026-11-20T10:00:00',
      },
    };
  });

  it('renders collection point details', () => {
    render(<CollectionPointDetailPage />);

    expect(screen.getByText('Posto Central')).toBeInTheDocument();
    expect(screen.getByText('Rua A, 100')).toBeInTheDocument();
    expect(screen.getByText('João Pessoa/PB')).toBeInTheDocument();
  });

  it('renders the loading state', () => {
    pointLoading = true;

    render(<CollectionPointDetailPage />);

    expect(document.querySelector('.spinner')).toBeInTheDocument();
  });

  it('renders the not-found state', () => {
    pointData = null;

    render(<CollectionPointDetailPage />);

    expect(screen.getByText('Ponto de coleta não encontrado')).toBeInTheDocument();
  });

  it('renders the API error state', () => {
    pointError = new Error('Falha');

    render(<CollectionPointDetailPage />);

    expect(screen.getByText('Ponto de coleta não encontrado')).toBeInTheDocument();
  });
});
