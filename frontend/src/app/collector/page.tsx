'use client';

import { useState, useEffect, useCallback } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { schedulesApi, collectionPointsApi, usersApi } from '@/lib/api';
import { CollectionSchedule, CollectionPoint, User } from '@/types';
import { useAuth } from '@/contexts/AuthContext';
import { useRouter } from 'next/navigation';
import toast from 'react-hot-toast';
import {
  MapPin,
  Calendar,
  Clock,
  Truck,
  Filter,
  RefreshCw,
  Map,
  List,
  CheckCircle,
  XCircle,
  AlertCircle,
  Navigation,
  MapPin as MapPinIcon,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { SCHEDULE_STATUS_LABELS, SCHEDULE_STATUS_COLORS, ScheduleStatus } from '@/types';

interface CollectorDashboardProps {
  autoRefreshInterval?: number;
}

const COLLECTOR_DASHBOARD_REFRESH_INTERVAL = 30000; // 30 seconds

export default function CollectorDashboardPage() {
  const { user } = useAuth();
  const router = useRouter();
  const queryClient = useQueryClient();
  const [viewMode, setViewMode] = useState<'list' | 'map'>('list');
  const [statusFilter, setStatusFilter] = useState<ScheduleStatus | 'ALL'>('ALL');
  const [dateFilter, setDateFilter] = useState<string>(format(new Date(), 'yyyy-MM-dd'));
  const [selectedPoint, setSelectedPoint] = useState<CollectionPoint | null>(null);
  const [showPointDetail, setShowPointDetail] = useState(false);
  const [mapCenter, setMapCenter] = useState<{ lat: number; lng: number } | null>(null);

  // Fetch schedules for current collector
  const { data: schedulesData, isLoading: schedulesLoading, refetch: refetchSchedules } = useQuery({
    queryKey: ['collectorSchedules', user?.id, statusFilter, dateFilter],
    queryFn: () => {
      if (statusFilter === 'ALL') {
        return schedulesApi.getByCollector(user?.id || '', { start: dateFilter, end: dateFilter });
      }
      return schedulesApi.getByCollectorAndStatus(user?.id || '', statusFilter);
    },
    enabled: !!user?.id && user.role === 'COLLECTOR',
    refetchInterval: COLLECTOR_DASHBOARD_REFRESH_INTERVAL,
  });

  // Fetch all active collection points
  const { data: pointsData, isLoading: pointsLoading } = useQuery({
    queryKey: ['collectorPoints', user?.id],
    queryFn: () => collectionPointsApi.getActiveByCollector(user?.id || ''),
    enabled: !!user?.id && user.role === 'COLLECTOR',
  });

  const schedules = schedulesData?.data || [];
  const points = pointsData?.data || [];

  // Filter points by status if needed
  const filteredPoints = points.filter(point => {
    const hasScheduleToday = schedules.some(s => s.collectionPointId === point.id && s.scheduledDate === dateFilter);
    return statusFilter === 'ALL' || hasScheduleToday;
  });

  // Group schedules by status
  const schedulesByStatus = {
    PENDING: schedules.filter(s => s.status === 'PENDING'),
    CONFIRMED: schedules.filter(s => s.status === 'CONFIRMED'),
    IN_PROGRESS: schedules.filter(s => s.status === 'IN_PROGRESS'),
    COMPLETED: schedules.filter(s => s.status === 'COMPLETED'),
    CANCELLED: schedules.filter(s => s.status === 'CANCELLED'),
  };

  // Mutations for schedule status changes
  const confirmMutation = useMutation({
    mutationFn: (id: string) => schedulesApi.confirm(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['collectorSchedules'] });
      toast.success('Agendamento confirmado!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao confirmar');
    },
  });

  const startMutation = useMutation({
    mutationFn: (id: string) => schedulesApi.start(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['collectorSchedules'] });
      toast.success('Coleta iniciada!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao iniciar');
    },
  });

  const cancelMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => schedulesApi.cancel(id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['collectorSchedules'] });
      toast.success('Agendamento cancelado!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao cancelar');
    },
  });

  // Manual refresh
  const handleRefresh = useCallback(() => {
    refetchSchedules();
    toast.success('Atualizado');
  }, [refetchSchedules]);

  // Handle schedule actions
  const handleConfirm = (id: string) => {
    if (confirm('Confirmar este agendamento?')) {
      confirmMutation.mutate(id);
    }
  };

  const handleStart = (id: string) => {
    if (confirm('Iniciar esta coleta?')) {
      startMutation.mutate(id);
    }
  };

  const handleComplete = (id: string) => {
    if (confirm('Finalizar esta coleta? Você será redirecionado para registrar o volume coletado e detalhes.')) {
      router.push(`/collector/complete/${id}`);
    }
  };

  const handleCancel = (id: string) => {
    const reason = prompt('Motivo do cancelamento:');
    if (reason) {
      cancelMutation.mutate({ id, reason });
    }
  };

  // Get point for schedule
  const getPointForSchedule = (schedule: CollectionSchedule) => {
    return points.find(p => p.id === schedule.collectionPointId);
  };

  // Calculate stats
  const stats = {
    totalToday: schedules.length,
    pending: schedulesByStatus.PENDING.length,
    confirmed: schedulesByStatus.CONFIRMED.length,
    inProgress: schedulesByStatus.IN_PROGRESS.length,
    completed: schedulesByStatus.COMPLETED.length,
    totalPoints: points.length,
  };

  if (!user || user.role !== 'COLLECTOR') {
    return (
      <div className="card">
        <div className="card-body text-center py-12">
          <AlertCircle className="w-12 h-12 mx-auto mb-4 text-yellow-500" />
          <h3 className="text-lg font-semibold text-gray-900">Acesso Restrito</h3>
          <p className="text-gray-500 mt-2">Esta página é exclusiva para coletores.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="page-title">Painel do Coletor</h1>
          <p className="page-subtitle">Gerencie suas coletas do dia - {format(new Date(dateFilter), 'EEEE, dd/MM/yyyy', { locale: ptBR })}</p>
        </div>
        <div className="flex items-center space-x-3">
          <div className="relative">
            <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
            <input
              type="date"
              value={dateFilter}
              onChange={(e) => setDateFilter(e.target.value)}
              className="input pl-10 w-48"
            />
          </div>
          <button onClick={handleRefresh} disabled={schedulesLoading} className="btn-outline">
            <RefreshCw className={`w-4 h-4 mr-2 ${schedulesLoading ? 'animate-spin' : ''}`} />
            Atualizar
          </button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-2 md:grid-cols-6 gap-4">
        <StatCard title="Total Hoje" value={stats.totalToday} icon={<Calendar className="w-5 h-5" />} color="text-blue-600" bgColor="bg-blue-100" />
        <StatCard title="Pendentes" value={stats.pending} icon={<Clock className="w-5 h-5" />} color="text-gray-600" bgColor="bg-gray-100" />
        <StatCard title="Confirmados" value={stats.confirmed} icon={<CheckCircle className="w-5 h-5" />} color="text-blue-600" bgColor="bg-blue-100" />
        <StatCard title="Em Andamento" value={stats.inProgress} icon={<Truck className="w-5 h-5" />} color="text-yellow-600" bgColor="bg-yellow-100" />
        <StatCard title="Concluídos" value={stats.completed} icon={<CheckCircle className="w-5 h-5" />} color="text-green-600" bgColor="bg-green-100" />
        <StatCard title="Pontos Ativos" value={stats.totalPoints} icon={<MapPin className="w-5 h-5" />} color="text-purple-600" bgColor="bg-purple-100" />
      </div>

      {/* Filters */}
      <div className="card">
        <div className="card-body">
          <div className="flex flex-col sm:flex-row gap-4">
            <div className="relative flex-1">
              <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value as ScheduleStatus | 'ALL')}
                className="input pl-10 pr-10 appearance-none"
              >
                <option value="ALL">Todos os status</option>
                <option value="PENDING">Pendente</option>
                <option value="CONFIRMED">Confirmado</option>
                <option value="IN_PROGRESS">Em Andamento</option>
                <option value="COMPLETED">Concluído</option>
                <option value="CANCELLED">Cancelado</option>
              </select>
            </div>
            <div className="flex space-x-2">
              <button
                onClick={() => setViewMode('list')}
                className={`btn ${viewMode === 'list' ? 'btn-primary' : 'btn-outline'}`}
              >
                <List className="w-4 h-4 mr-1" />
                Lista
              </button>
              <button
                onClick={() => setViewMode('map')}
                className={`btn ${viewMode === 'map' ? 'btn-primary' : 'btn-outline'}`}
              >
                <Map className="w-4 h-4 mr-1" />
                Mapa
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="card">
        <div className="card-body p-0">
          {viewMode === 'list' ? (
            <ScheduleListView
              schedules={schedules}
              points={points}
              getPointForSchedule={getPointForSchedule}
              onConfirm={handleConfirm}
              onStart={handleStart}
              onComplete={handleComplete}
              onCancel={handleCancel}
              isConfirming={confirmMutation.isPending}
              isStarting={startMutation.isPending}
              isCancelling={cancelMutation.isPending}
            />
          ) : (
            <MapView
              points={points}
              schedules={schedules}
              selectedPoint={selectedPoint}
              onSelectPoint={setSelectedPoint}
              mapCenter={mapCenter}
              onMapCenterChange={setMapCenter}
            />
          )}
        </div>
      </div>

      {/* Point Detail Modal */}
      {showPointDetail && selectedPoint && (
        <PointDetailModal
          point={selectedPoint}
          schedules={schedules.filter(s => s.collectionPointId === selectedPoint.id)}
          onClose={() => {
            setShowPointDetail(false);
            setSelectedPoint(null);
          }}
        />
      )}
    </div>
  );
}

function ScheduleListView({
  schedules,
  points,
  getPointForSchedule,
  onConfirm,
  onStart,
  onComplete,
  onCancel,
  isConfirming,
  isStarting,
  isCancelling,
}: {
  schedules: CollectionSchedule[];
  points: CollectionPoint[];
  getPointForSchedule: (schedule: CollectionSchedule) => CollectionPoint | undefined;
  onConfirm: (id: string) => void;
  onStart: (id: string) => void;
  onComplete: (id: string) => void;
  onCancel: (id: string) => void;
  isConfirming: boolean;
  isStarting: boolean;
  isCancelling: boolean;
}) {
  if (schedules.length === 0) {
    return (
      <div className="text-center py-12">
        <Calendar className="w-12 h-12 mx-auto mb-4 text-gray-300" />
        <p className="text-lg text-gray-500">Nenhum agendamento para hoje</p>
        <p className="text-sm text-gray-400">Aproveite o dia livre!</p>
      </div>
    );
  }

  return (
    <div className="table-container">
      <table className="table">
        <thead>
          <tr>
            <th>Ponto de Coleta</th>
            <th>Endereço</th>
            <th>Horário</th>
            <th>Status</th>
            <th>Volume Est.</th>
            <th className="text-right">Ações</th>
          </tr>
        </thead>
        <tbody>
          {schedules.map((schedule) => {
            const point = getPointForSchedule(schedule);
            const isActionLoading =
              (schedule.status === 'PENDING' && isConfirming) ||
              (schedule.status === 'CONFIRMED' && isStarting) ||
              (['PENDING', 'CONFIRMED', 'IN_PROGRESS'].includes(schedule.status) && isCancelling);

            return (
              <tr key={schedule.id} className={schedule.status === 'IN_PROGRESS' ? 'bg-yellow-50' : schedule.status === 'COMPLETED' ? 'bg-green-50' : ''}>
                <td>
                  <div>
                    <p className="font-medium text-gray-900">{schedule.collectionPointName}</p>
                    {point && (
                      <p className="text-xs text-gray-500">{point.address}, {point.neighborhood}, {point.city}/{point.state}</p>
                    )}
                  </div>
                </td>
                <td className="text-sm text-gray-600 hidden sm:table-cell">
                  {point?.address}
                </td>
                <td>
                  <div className="flex items-center space-x-1">
                    <Clock className="w-4 h-4 text-gray-400" />
                    <span className="text-sm font-medium text-gray-900">
                      {schedule.startTime}{schedule.endTime && ` - ${schedule.endTime}`}
                    </span>
                  </div>
                </td>
                <td>
                  <span className={`badge ${SCHEDULE_STATUS_COLORS[schedule.status as ScheduleStatus]}`}>
                    {SCHEDULE_STATUS_LABELS[schedule.status as ScheduleStatus]}
                  </span>
                </td>
                <td className="text-sm text-gray-600">
                  {schedule.estimatedVolumeLiters ? `${schedule.estimatedVolumeLiters.toLocaleString('pt-BR')} L` : '-'}
                </td>
                <td className="text-right">
                  <div className="flex items-center justify-end space-x-2">
                    {schedule.status === 'PENDING' && (
                      <button
                        onClick={() => onConfirm(schedule.id)}
                        disabled={isActionLoading}
                        className="btn-primary text-sm px-3 py-1.5"
                      >
                        <CheckCircle className="w-3.5 h-3.5 mr-1" />
                        Confirmar
                      </button>
                    )}
                    {schedule.status === 'CONFIRMED' && (
                      <button
                        onClick={() => onStart(schedule.id)}
                        disabled={isActionLoading}
                        className="btn-warning text-sm px-3 py-1.5"
                      >
                        <Truck className="w-3.5 h-3.5 mr-1" />
                        Iniciar
                      </button>
                    )}
                    {(schedule.status === 'CONFIRMED' || schedule.status === 'IN_PROGRESS') && (
                      <button
                        onClick={() => onComplete(schedule.id)}
                        disabled={isActionLoading}
                        className="btn-success text-sm px-3 py-1.5"
                      >
                        <CheckCircle className="w-3.5 h-3.5 mr-1" />
                        Concluir
                      </button>
                    )}
                    {['PENDING', 'CONFIRMED', 'IN_PROGRESS'].includes(schedule.status) && (
                      <button
                        onClick={() => onCancel(schedule.id)}
                        disabled={isActionLoading}
                        className="btn-danger text-sm px-3 py-1.5"
                      >
                        <XCircle className="w-3.5 h-3.5 mr-1" />
                        Cancelar
                      </button>
                    )}
                    <button
                      onClick={() => {
                        const point = getPointForSchedule(schedule);
                        if (point) {
                          window.open(`https://www.google.com/maps/dir/?api=1&destination=${point.latitude},${point.longitude}`, '_blank');
                        }
                      }}
                      className="btn-outline text-sm px-3 py-1.5"
                    >
                      <Navigation className="w-3.5 h-3.5 mr-1" />
                      Rota
                    </button>
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

function MapView({
  points,
  schedules,
  selectedPoint,
  onSelectPoint,
  mapCenter,
  onMapCenterChange,
}: {
  points: CollectionPoint[];
  schedules: CollectionSchedule[];
  selectedPoint: CollectionPoint | null;
  onSelectPoint: (point: CollectionPoint | null) => void;
  mapCenter: { lat: number; lng: number } | null;
  onMapCenterChange: (center: { lat: number; lng: number } | null) => void;
}) {
  // Calculate center from points with schedules today
  const pointsWithSchedules = points.filter(p =>
    schedules.some(s => s.collectionPointId === p.id)
  );

  const defaultCenter = pointsWithSchedules.length > 0
    ? { lat: pointsWithSchedules[0].latitude!, lng: pointsWithSchedules[0].longitude! }
    : { lat: -23.5505, lng: -46.6333 }; // São Paulo default

  const center = mapCenter || defaultCenter;

  return (
    <div className="relative h-96">
      <div className="absolute inset-0 bg-gray-100 flex items-center justify-center">
        <div className="text-center text-gray-500">
          <MapPin className="w-12 h-12 mx-auto mb-4 text-gray-300" />
          <p className="text-lg">Visualização do Mapa</p>
          <p className="text-sm">Integração com Google Maps/Leaflet a ser implementada</p>
          <div className="mt-4 grid grid-cols-2 gap-2 max-w-md mx-auto">
            {pointsWithSchedules.slice(0, 6).map((point) => (
              <button
                key={point.id}
                onClick={() => onSelectPoint(point)}
                className={`p-3 rounded-lg text-left border-2 transition-colors ${
                  selectedPoint?.id === point.id
                    ? 'border-primary-500 bg-primary-50'
                    : 'border-gray-200 hover:border-primary-300'
                }`}
              >
                <p className="font-medium text-sm text-gray-900">{point.name}</p>
                <p className="text-xs text-gray-500">{point.city}/{point.state}</p>
                <p className="text-xs text-gray-400 mt-1">
                  {schedules.filter(s => s.collectionPointId === point.id).length} agendamento(s)
                </p>
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

function PointDetailModal({
  point,
  schedules,
  onClose,
}: {
  point: CollectionPoint;
  schedules: CollectionSchedule[];
  onClose: () => void;
}) {
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content max-w-lg" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3 className="text-lg font-semibold text-gray-900">{point.name}</h3>
          <button onClick={onClose} className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100">
            <XCircle className="w-5 h-5" />
          </button>
        </div>
        <div className="modal-body p-6">
          <div className="space-y-3">
            <div className="flex items-center space-x-2 text-sm text-gray-600">
              <MapPinIcon className="w-4 h-4 text-gray-400" />
              <span>{point.address}, {point.neighborhood}, {point.city}/{point.state}</span>
            </div>
            {point.contactName && (
              <div className="flex items-center space-x-2 text-sm text-gray-600">
                <User className="w-4 h-4 text-gray-400" />
                <span>{point.contactName}</span>
              </div>
            )}
            {point.contactPhone && (
              <div className="flex items-center space-x-2 text-sm text-gray-600">
                <Phone className="w-4 h-4 text-gray-400" />
                <span>{point.contactPhone}</span>
              </div>
            )}
            <div className="pt-4 border-t border-gray-200">
              <p className="font-medium text-gray-900 mb-2">Agendamentos Hoje</p>
              {schedules.length === 0 ? (
                <p className="text-sm text-gray-500">Nenhum agendamento</p>
              ) : (
                <div className="space-y-2">
                  {schedules.map((schedule) => (
                    <div key={schedule.id} className="p-3 bg-gray-50 rounded-lg">
                      <div className="flex items-center justify-between">
                        <span className="font-medium text-sm">{schedule.startTime} - {schedule.endTime || 'Fim do dia'}</span>
                        <span className={`badge ${SCHEDULE_STATUS_COLORS[schedule.status as ScheduleStatus]}`}>
                          {SCHEDULE_STATUS_LABELS[schedule.status as ScheduleStatus]}
                        </span>
                      </div>
                      {schedule.notes && (
                        <p className="text-xs text-gray-500 mt-1">{schedule.notes}</p>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
        <div className="modal-footer">
          <button onClick={onClose} className="btn-primary">
            Fechar
          </button>
        </div>
      </div>
    </div>
  );
}

function StatCard({ title, value, icon, color, bgColor }: {
  title: string;
  value: number;
  icon: React.ReactNode;
  color: string;
  bgColor: string;
}) {
  return (
    <div className="card">
      <div className="card-body">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-xs font-medium text-gray-500">{title}</p>
            <p className="text-2xl font-bold text-gray-900 mt-1">{value}</p>
          </div>
          <div className={`p-2.5 rounded-xl ${bgColor}`}>
            <div className={`${color}`}>{icon}</div>
          </div>
        </div>
      </div>
    </div>
  );
}