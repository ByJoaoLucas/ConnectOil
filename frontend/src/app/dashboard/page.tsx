'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import { dashboardApi, schedulesApi, recordsApi } from '@/lib/api';
import { DashboardStatsDTO, CollectionSchedule, CollectionRecord, SCHEDULE_STATUS_LABELS, SCHEDULE_STATUS_COLORS, ScheduleStatus } from '@/types';
import {
  MapPin,
  Calendar,
  Package,
  TrendingUp,
  AlertCircle,
  Clock,
  CheckCircle,
  XCircle,
  Users,
  Building2,
  Truck,
  Star,
  Target,
  Play,
  CheckCheck,
  RefreshCw,
  X,
} from 'lucide-react';
import { format, parse } from 'date-fns';
import { ptBR } from 'date-fns/locale';

// Helper para parsear data YYYY-MM-DD como data local (evita bug de fuso UTC)
function parseLocalDate(dateString: string): Date {
  return parse(dateString, 'yyyy-MM-dd', new Date());
}

interface StatsCardProps {
  title: string;
  value: string | number;
  icon: React.ReactNode;
  color: string;
  bgColor: string;
  trend?: string;
}

function StatsCard({ title, value, icon, color, bgColor, trend }: StatsCardProps) {
  return (
    <div className="card">
      <div className="card-body">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-sm font-medium text-gray-500">{title}</p>
            <p className="text-3xl font-bold text-gray-900 mt-1">{value}</p>
            {trend && <p className="text-sm text-green-600 mt-1">{trend}</p>}
          </div>
          <div className={`p-3 rounded-xl ${bgColor}`}>
            <div className={`${color}`}>{icon}</div>
          </div>
        </div>
      </div>
    </div>
  );
}

function RecentSchedules({ schedules, userRole }: { schedules: CollectionSchedule[]; userRole: string }) {
  const queryClient = useQueryClient();

  const getStatusConfig = (status: ScheduleStatus) => {
    // Use centralized labels from types
    const label = SCHEDULE_STATUS_LABELS[status];
    const colorClass = SCHEDULE_STATUS_COLORS[status].replace('bg-', 'badge-').replace(' text-', '-');

    // Map status to action for collector
    const actionMap: Record<ScheduleStatus, string | null> = {
      PENDING: 'Aceitar',
      CONFIRMED: 'Iniciar',
      ASSIGNED: 'Iniciar',
      EN_ROUTE: 'Chegar',
      IN_PROGRESS: 'Concluir',
      COMPLETED: null,
      CANCELLED: null,
      RESCHEDULED: null,
    };

    return { label, className: `badge ${colorClass}`, action: actionMap[status] };
  };

  const getActionIcon = (action: string | null) => {
    switch (action) {
      case 'Aceitar': return <CheckCheck className="w-4 h-4" />;
      case 'Iniciar': return <Play className="w-4 h-4" />;
      case 'Chegar': return <Truck className="w-4 h-4" />;
      case 'Concluir': return <CheckCircle className="w-4 h-4" />;
      default: return null;
    }
  };

  const confirmMutation = useMutation({
    mutationFn: (id: string) => schedulesApi.confirm(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dashboardStats'] });
      queryClient.invalidateQueries({ queryKey: ['schedules'] });
    },
  });

  const startMutation = useMutation({
    mutationFn: (id: string) => schedulesApi.start(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dashboardStats'] });
      queryClient.invalidateQueries({ queryKey: ['schedules'] });
    },
  });

  const arriveMutation = useMutation({
    mutationFn: (id: string) => schedulesApi.arrive(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dashboardStats'] });
      queryClient.invalidateQueries({ queryKey: ['schedules'] });
    },
  });

  const completeMutation = useMutation({
    mutationFn: (id: string) => schedulesApi.complete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dashboardStats'] });
      queryClient.invalidateQueries({ queryKey: ['schedules'] });
    },
  });

  const handleScheduleAction = async (scheduleId: string, action: string) => {
    try {
      switch (action) {
        case 'Aceitar': await confirmMutation.mutateAsync(scheduleId); break;
        case 'Iniciar': await startMutation.mutateAsync(scheduleId); break;
        case 'Chegar': await arriveMutation.mutateAsync(scheduleId); break;
        case 'Concluir': await completeMutation.mutateAsync(scheduleId); break;
      }
    } catch (error) {
      console.error('Erro ao executar ação:', error);
    }
  };

  return (
    <div className="card">
      <div className="card-header">
        <h3 className="text-lg font-semibold text-gray-900">
          {userRole === 'COLLECTOR' ? 'Meus Agendamentos Atribuídos' : 'Próximos Agendamentos'}
        </h3>
      </div>
      <div className="card-body p-0">
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Ponto de Coleta</th>
                <th>Data</th>
                <th>Horário</th>
                <th>Status</th>
                {userRole === 'COLLECTOR' && <th className="text-right">Ações</th>}
              </tr>
            </thead>
            <tbody>
              {schedules.length === 0 ? (
                <tr>
                  <td colSpan={userRole === 'COLLECTOR' ? 5 : 4} className="text-center py-8 text-gray-500">
                    Nenhum agendamento encontrado
                  </td>
                </tr>
              ) : (
                schedules.slice(0, 5).map((schedule) => {
                  const statusConfig = getStatusConfig(schedule.status);
                  const showAction = userRole === 'COLLECTOR' && statusConfig.action &&
                    ['PENDING', 'ASSIGNED', 'EN_ROUTE', 'IN_PROGRESS'].includes(schedule.status);
                  return (
                    <tr key={schedule.id}>
                      <td className="font-medium text-gray-900">{schedule.collectionPointName}</td>
                      <td>
                        {format(parseLocalDate(schedule.scheduledDate), 'dd/MM/yyyy', { locale: ptBR })}
                        {schedule.isToday && (
                          <span className="ml-2 badge-info">Hoje</span>
                        )}
                      </td>
                      <td>{schedule.startTime}{schedule.endTime && ` - ${schedule.endTime}`}</td>
                      <td>
                        <span className={`badge ${statusConfig.className}`}>
                          {statusConfig.label}
                        </span>
                      </td>
                      {userRole === 'COLLECTOR' && (
                        <td className="text-right">
                          {showAction && (
                            <button
                              onClick={() => handleScheduleAction(schedule.id, statusConfig.action!)}
                              className="btn btn-primary btn-sm"
                              disabled={false}
                            >
                              {getActionIcon(statusConfig.action)} {statusConfig.action}
                            </button>
                          )}
                          {!showAction && statusConfig.action && (
                            <span className="text-gray-400 text-sm">—</span>
                          )}
                        </td>
                      )}
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function RecentRecords({ records, userRole }: { records: CollectionRecord[]; userRole: string }) {
  return (
    <div className="card">
      <div className="card-header">
        <h3 className="text-lg font-semibold text-gray-900">
          {userRole === 'COLLECTOR' ? 'Minhas Últimas Coletas' : 'Últimas Coletas'}
        </h3>
      </div>
      <div className="card-body p-0">
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Ponto de Coleta</th>
                {userRole !== 'ESTABLISHMENT' && <th>Coletor</th>}
                <th>Volume (L)</th>
                <th>Data</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {records.length === 0 ? (
                <tr>
                  <td colSpan={userRole === 'ESTABLISHMENT' ? 4 : 5} className="text-center py-8 text-gray-500">
                    Nenhum registro encontrado
                  </td>
                </tr>
              ) : (
                records.slice(0, 5).map((record) => (
                  <tr key={record.id}>
                    <td className="font-medium text-gray-900">{record.collectionPointName}</td>
                    {userRole !== 'ESTABLISHMENT' && <td>{record.collectorName}</td>}
                    <td>{record.volumeCollectedLiters.toLocaleString('pt-BR')}</td>
                    <td>{format(new Date(record.collectionDate), 'dd/MM/yyyy HH:mm', { locale: ptBR })}</td>
                    <td>
                      <span className={record.verifiedByManager ? 'badge-success' : 'badge-warning'}>
                        {record.verifiedByManager ? 'Verificado' : 'Pendente'}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function AdminStatsGrid({ stats }: { stats: DashboardStatsDTO }) {
  return (
    <div className="space-y-4">
      <h2 className="text-lg font-semibold text-gray-900">Visão Geral do Sistema</h2>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatsCard
          title="Total de Pontos"
          value={stats.totalPoints}
          icon={<MapPin className="w-6 h-6" />}
          color="text-primary-600"
          bgColor="bg-primary-100"
        />
        <StatsCard
          title="Pontos Ativos"
          value={stats.activePoints}
          icon={<CheckCircle className="w-6 h-6" />}
          color="text-green-600"
          bgColor="bg-green-100"
        />
        <StatsCard
          title="Pontos Cheios"
          value={stats.fullPoints}
          icon={<AlertCircle className="w-6 h-6" />}
          color="text-red-600"
          bgColor="bg-red-100"
        />
        <StatsCard
          title="Pendentes Aprovação"
          value={stats.pendingApprovalPoints}
          icon={<Clock className="w-6 h-6" />}
          color="text-blue-600"
          bgColor="bg-blue-100"
        />
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatsCard
          title="Total Coletores"
          value={stats.totalCollectors}
          icon={<Users className="w-6 h-6" />}
          color="text-indigo-600"
          bgColor="bg-indigo-100"
        />
        <StatsCard
          title="Coletores Ativos"
          value={stats.activeCollectors}
          icon={<Truck className="w-6 h-6" />}
          color="text-teal-600"
          bgColor="bg-teal-100"
        />
        <StatsCard
          title="Total Estabelecimentos"
          value={stats.totalEstablishments}
          icon={<Building2 className="w-6 h-6" />}
          color="text-purple-600"
          bgColor="bg-purple-100"
        />
        <StatsCard
          title="Estabelecimentos Ativos"
          value={stats.activeEstablishments}
          icon={<Target className="w-6 h-6" />}
          color="text-pink-600"
          bgColor="bg-pink-100"
        />
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatsCard
          title="Agendamentos Hoje"
          value={stats.todaySchedules}
          icon={<Calendar className="w-6 h-6" />}
          color="text-blue-600"
          bgColor="bg-blue-100"
        />
        <StatsCard
          title="Agendamentos Pendentes"
          value={stats.pendingSchedules}
          icon={<Clock className="w-6 h-6" />}
          color="text-yellow-600"
          bgColor="bg-yellow-100"
        />
        <StatsCard
          title="Volume Total Coletado"
          value={`${(stats.totalVolume || 0).toLocaleString('pt-BR')} L`}
          icon={<Package className="w-6 h-6" />}
          color="text-purple-600"
          bgColor="bg-purple-100"
        />
        <StatsCard
          title="Volume Este Mês"
          value={`${(stats.monthVolume || 0).toLocaleString('pt-BR')} L`}
          icon={<TrendingUp className="w-6 h-6" />}
          color="text-green-600"
          bgColor="bg-green-100"
        />
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatsCard
          title="Total de Coletas"
          value={stats.totalRecords}
          icon={<Package className="w-6 h-6" />}
          color="text-indigo-600"
          bgColor="bg-indigo-100"
        />
        <StatsCard
          title="Coletas Este Mês"
          value={stats.monthRecords}
          icon={<Calendar className="w-6 h-6" />}
          color="text-blue-600"
          bgColor="bg-blue-100"
        />
        <StatsCard
          title="Pendentes Verificação"
          value={stats.pendingVerification}
          icon={<XCircle className="w-6 h-6" />}
          color="text-orange-600"
          bgColor="bg-orange-100"
        />
        <StatsCard
          title="Atrasados"
          value={stats.overdueSchedules}
          icon={<AlertCircle className="w-6 h-6" />}
          color="text-red-600"
          bgColor="bg-red-100"
        />
      </div>
    </div>
  );
}

function EstablishmentStatsGrid({ stats }: { stats: DashboardStatsDTO }) {
  return (
    <div className="space-y-4">
      <h2 className="text-lg font-semibold text-gray-900">Meu Estabelecimento</h2>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatsCard
          title="Meus Pontos"
          value={stats.totalPoints}
          icon={<MapPin className="w-6 h-6" />}
          color="text-primary-600"
          bgColor="bg-primary-100"
        />
        <StatsCard
          title="Pontos Ativos"
          value={stats.activePoints}
          icon={<CheckCircle className="w-6 h-6" />}
          color="text-green-600"
          bgColor="bg-green-100"
        />
        <StatsCard
          title="Pontos Cheios"
          value={stats.fullPoints}
          icon={<AlertCircle className="w-6 h-6" />}
          color="text-red-600"
          bgColor="bg-red-100"
        />
        <StatsCard
          title="Agendamentos Hoje"
          value={stats.todaySchedules}
          icon={<Calendar className="w-6 h-6" />}
          color="text-blue-600"
          bgColor="bg-blue-100"
        />
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatsCard
          title="Agendamentos Pendentes"
          value={stats.pendingSchedules}
          icon={<Clock className="w-6 h-6" />}
          color="text-yellow-600"
          bgColor="bg-yellow-100"
        />
        <StatsCard
          title="Total Coletas"
          value={stats.totalRecords}
          icon={<Package className="w-6 h-6" />}
          color="text-indigo-600"
          bgColor="bg-indigo-100"
        />
        <StatsCard
          title="Coletas Este Mês"
          value={stats.monthRecords}
          icon={<Calendar className="w-6 h-6" />}
          color="text-blue-600"
          bgColor="bg-blue-100"
        />
        <StatsCard
          title="Volume Total"
          value={`${(stats.totalVolume || 0).toLocaleString('pt-BR')} L`}
          icon={<TrendingUp className="w-6 h-6" />}
          color="text-purple-600"
          bgColor="bg-purple-100"
        />
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatsCard
          title="Volume Este Mês"
          value={`${(stats.monthVolume || 0).toLocaleString('pt-BR')} L`}
          icon={<TrendingUp className="w-6 h-6" />}
          color="text-green-600"
          bgColor="bg-green-100"
        />
        <StatsCard
          title="Pendentes Verificação"
          value={stats.pendingVerification}
          icon={<XCircle className="w-6 h-6" />}
          color="text-orange-600"
          bgColor="bg-orange-100"
        />
        <StatsCard
          title="Total Agendamentos"
          value={stats.totalSchedules}
          icon={<Calendar className="w-6 h-6" />}
          color="text-teal-600"
          bgColor="bg-teal-100"
        />
        <StatsCard
          title="Status"
          value="Ativo"
          icon={<CheckCircle className="w-6 h-6" />}
          color="text-green-600"
          bgColor="bg-green-100"
        />
      </div>
    </div>
  );
}

function CollectorStatsGrid({ stats }: { stats: DashboardStatsDTO }) {
  return (
    <div className="space-y-4">
      <h2 className="text-lg font-semibold text-gray-900">Meu Painel de Coletor</h2>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatsCard
          title="Pontos Atribuídos"
          value={stats.totalPoints}
          icon={<MapPin className="w-6 h-6" />}
          color="text-primary-600"
          bgColor="bg-primary-100"
        />
        <StatsCard
          title="Pontos Ativos"
          value={stats.activePoints}
          icon={<CheckCircle className="w-6 h-6" />}
          color="text-green-600"
          bgColor="bg-green-100"
        />
        <StatsCard
          title="Pontos Cheios"
          value={stats.fullPoints}
          icon={<AlertCircle className="w-6 h-6" />}
          color="text-red-600"
          bgColor="bg-red-100"
        />
        <StatsCard
          title="Agendamentos Hoje"
          value={stats.todaySchedules}
          icon={<Calendar className="w-6 h-6" />}
          color="text-blue-600"
          bgColor="bg-blue-100"
        />
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatsCard
          title="Em Andamento"
          value={stats.inProgressSchedules}
          icon={<Truck className="w-6 h-6" />}
          color="text-orange-600"
          bgColor="bg-orange-100"
        />
        <StatsCard
          title="Pendentes"
          value={stats.pendingSchedules}
          icon={<Clock className="w-6 h-6" />}
          color="text-yellow-600"
          bgColor="bg-yellow-100"
        />
        <StatsCard
          title="Concluídos"
          value={stats.completedSchedules}
          icon={<CheckCircle className="w-6 h-6" />}
          color="text-green-600"
          bgColor="bg-green-100"
        />
        <StatsCard
          title="Total Agendamentos"
          value={stats.totalSchedules}
          icon={<Calendar className="w-6 h-6" />}
          color="text-indigo-600"
          bgColor="bg-indigo-100"
        />
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatsCard
          title="Total Coletas"
          value={stats.totalRecords}
          icon={<Package className="w-6 h-6" />}
          color="text-purple-600"
          bgColor="bg-purple-100"
        />
        <StatsCard
          title="Coletas Este Mês"
          value={stats.monthRecords}
          icon={<Calendar className="w-6 h-6" />}
          color="text-blue-600"
          bgColor="bg-blue-100"
        />
        <StatsCard
          title="Volume Total"
          value={`${(stats.totalVolume || 0).toLocaleString('pt-BR')} L`}
          icon={<TrendingUp className="w-6 h-6" />}
          color="text-green-600"
          bgColor="bg-green-100"
        />
        <StatsCard
          title="Volume Este Mês"
          value={`${(stats.monthVolume || 0).toLocaleString('pt-BR')} L`}
          icon={<Package className="w-6 h-6" />}
          color="text-teal-600"
          bgColor="bg-teal-100"
        />
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatsCard
          title="Qualidade Média"
          value={(stats.avgQuality || 0).toFixed(1)}
          icon={<Star className="w-6 h-6" />}
          color="text-yellow-600"
          bgColor="bg-yellow-100"
        />
        <StatsCard
          title="Pendentes Verificação"
          value={stats.pendingVerification}
          icon={<XCircle className="w-6 h-6" />}
          color="text-orange-600"
          bgColor="bg-orange-100"
        />
        <StatsCard
          title="Performance"
          value={`${stats.avgQuality >= 4 ? 'Excelente' : stats.avgQuality >= 3 ? 'Boa' : 'Regular'}`}
          icon={<Target className="w-6 h-6" />}
          color="text-blue-600"
          bgColor="bg-blue-100"
        />
        <StatsCard
          title="Status"
          value="Ativo"
          icon={<CheckCircle className="w-6 h-6" />}
          color="text-green-600"
          bgColor="bg-green-100"
        />
      </div>
    </div>
  );
}

export default function DashboardPage() {
  const { user } = useAuth();

  const { data: statsData, isLoading: statsLoading } = useQuery({
    queryKey: ['dashboardStats'],
    queryFn: () => dashboardApi.getStats(),
  });

  const { data: schedulesData } = useQuery({
    queryKey: ['schedules'],
    queryFn: () => {
      // Use role-appropriate endpoint
      return schedulesApi.getAllList();
    },
  });

  const { data: recordsData } = useQuery({
    queryKey: ['records'],
    queryFn: () => {
      return recordsApi.getAllList();
    },
  });

  const stats = statsData?.data as DashboardStatsDTO | undefined;
  const allSchedules = schedulesData?.data || [];
  const allRecords = recordsData?.data || [];

  // Filter schedules and records based on user role
  let userSchedules = allSchedules;
  let userRecords = allRecords;

  if (user?.role === 'COLLECTOR') {
    userSchedules = allSchedules.filter((s: CollectionSchedule) => s.collectorId === user.id);
    userRecords = allRecords.filter((r: CollectionRecord) => r.collectorId === user.id);
  } else if (user?.role === 'ESTABLISHMENT') {
    // For establishment, we'd need to filter by their points
    // For now, show all - the backend stats already filter
    // TODO: Filter by establishment points
  }

  if (statsLoading) {
    return (
      <div className="space-y-6">
        <div className="page-header">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="page-title">Dashboard</h1>
              <p className="page-subtitle">Carregando estatísticas...</p>
            </div>
          </div>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="card animate-pulse">
              <div className="card-body">
                <div className="h-4 bg-gray-200 rounded w-3/4 mb-2"></div>
                <div className="h-8 bg-gray-200 rounded w-1/2"></div>
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="page-header">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="page-title">Dashboard</h1>
            <p className="page-subtitle">
              {user?.role === 'ADMIN' ? 'Visão geral do sistema de coleta de óleo' :
               user?.role === 'ESTABLISHMENT' ? 'Estatísticas do seu estabelecimento' :
               'Seu painel de coletas e agendamentos'}
            </p>
          </div>
          <div className="hidden sm:block text-sm text-gray-500">
            {format(new Date(), "EEEE, d 'de' MMMM 'de' yyyy", { locale: ptBR })}
          </div>
        </div>
      </div>

      {stats && (
        <>
          {user?.role === 'ADMIN' && <AdminStatsGrid stats={stats} />}
          {user?.role === 'ESTABLISHMENT' && <EstablishmentStatsGrid stats={stats} />}
          {user?.role === 'COLLECTOR' && <CollectorStatsGrid stats={stats} />}
        </>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <RecentSchedules schedules={userSchedules} userRole={user?.role || ''} />
        <RecentRecords records={userRecords} userRole={user?.role || ''} />
      </div>
    </div>
  );
}