'use client';

import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import { collectionPointsApi, schedulesApi, recordsApi } from '@/lib/api';
import { CollectionPoint, CollectionSchedule, CollectionRecord } from '@/types';
import {
  MapPin,
  Calendar,
  Package,
  TrendingUp,
  AlertCircle,
  Clock,
  CheckCircle,
  XCircle,
} from 'lucide-react';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';

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

function RecentSchedules({ schedules }: { schedules: CollectionSchedule[] }) {
  return (
    <div className="card">
      <div className="card-header">
        <h3 className="text-lg font-semibold text-gray-900">Próximos Agendamentos</h3>
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
              </tr>
            </thead>
            <tbody>
              {schedules.length === 0 ? (
                <tr>
                  <td colSpan={4} className="text-center py-8 text-gray-500">
                    Nenhum agendamento encontrado
                  </td>
                </tr>
              ) : (
                schedules.slice(0, 5).map((schedule) => (
                  <tr key={schedule.id}>
                    <td className="font-medium text-gray-900">{schedule.collectionPointName}</td>
                    <td>
                      {format(new Date(schedule.scheduledDate), 'dd/MM/yyyy', { locale: ptBR })}
                      {schedule.isToday && (
                        <span className="ml-2 badge-info">Hoje</span>
                      )}
                    </td>
                    <td>{schedule.startTime}{schedule.endTime && ` - ${schedule.endTime}`}</td>
                    <td>
                      <span className={`badge ${
                        schedule.status === 'COMPLETED' ? 'badge-success' :
                        schedule.status === 'CANCELLED' ? 'badge-danger' :
                        schedule.status === 'IN_PROGRESS' ? 'badge-warning' : 'badge-info'
                      }`}>
                        {schedule.status}
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

function RecentRecords({ records }: { records: CollectionRecord[] }) {
  return (
    <div className="card">
      <div className="card-header">
        <h3 className="text-lg font-semibold text-gray-900">Últimas Coletas</h3>
      </div>
      <div className="card-body p-0">
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Ponto de Coleta</th>
                <th>Coletor</th>
                <th>Volume (L)</th>
                <th>Data</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {records.length === 0 ? (
                <tr>
                  <td colSpan={5} className="text-center py-8 text-gray-500">
                    Nenhum registro encontrado
                  </td>
                </tr>
              ) : (
                records.slice(0, 5).map((record) => (
                  <tr key={record.id}>
                    <td className="font-medium text-gray-900">{record.collectionPointName}</td>
                    <td>{record.collectorName}</td>
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

export default function DashboardPage() {
  const { user } = useAuth();

  const { data: pointsData } = useQuery({
    queryKey: ['collectionPoints'],
    queryFn: () => collectionPointsApi.getAllList(),
  });

  const { data: schedulesData } = useQuery({
    queryKey: ['schedules'],
    queryFn: () => schedulesApi.getAllList(),
  });

  const { data: recordsData } = useQuery({
    queryKey: ['records'],
    queryFn: () => recordsApi.getAllList(),
  });

  const points = pointsData?.data || [];
  const schedules = schedulesData?.data || [];
  const records = recordsData?.data || [];

  const totalPoints = points.length;
  const activePoints = points.filter((p: CollectionPoint) => p.status === 'ACTIVE').length;
  const fullPoints = points.filter((p: CollectionPoint) => p.status === 'FULL').length;
  const pendingSchedules = schedules.filter((s: CollectionSchedule) => s.status === 'PENDING' || s.status === 'CONFIRMED').length;
  const todaySchedules = schedules.filter((s: CollectionSchedule) => s.isToday).length;
  const totalVolume = records.reduce((sum: number, r: CollectionRecord) => sum + r.volumeCollectedLiters, 0);
  const pendingVerification = records.filter((r: CollectionRecord) => !r.verifiedByManager).length;

  const userSchedules = user?.role === 'COLLECTOR'
    ? schedules.filter((s: CollectionSchedule) => s.collectorId === user.id)
    : schedules;

  const userRecords = user?.role === 'COLLECTOR'
    ? records.filter((r: CollectionRecord) => r.collectorId === user.id)
    : records;

  return (
    <div className="space-y-6">
      <div className="page-header">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="page-title">Dashboard</h1>
            <p className="page-subtitle">Visão geral do sistema de coleta de óleo</p>
          </div>
          <div className="hidden sm:block text-sm text-gray-500">
            {format(new Date(), "EEEE, d 'de' MMMM 'de' yyyy", { locale: ptBR })}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatsCard
          title="Total de Pontos"
          value={totalPoints}
          icon={<MapPin className="w-6 h-6" />}
          color="text-primary-600"
          bgColor="bg-primary-100"
        />
        <StatsCard
          title="Pontos Ativos"
          value={activePoints}
          icon={<CheckCircle className="w-6 h-6" />}
          color="text-green-600"
          bgColor="bg-green-100"
        />
        <StatsCard
          title="Pontos Cheios"
          value={fullPoints}
          icon={<AlertCircle className="w-6 h-6" />}
          color="text-red-600"
          bgColor="bg-red-100"
        />
        <StatsCard
          title="Agendamentos Hoje"
          value={todaySchedules}
          icon={<Clock className="w-6 h-6" />}
          color="text-blue-600"
          bgColor="bg-blue-100"
        />
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatsCard
          title="Agendamentos Pendentes"
          value={pendingSchedules}
          icon={<Calendar className="w-6 h-6" />}
          color="text-yellow-600"
          bgColor="bg-yellow-100"
        />
        <StatsCard
          title="Volume Total Coletado"
          value={`${totalVolume.toLocaleString('pt-BR')} L`}
          icon={<Package className="w-6 h-6" />}
          color="text-purple-600"
          bgColor="bg-purple-100"
        />
        <StatsCard
          title="Pendentes Verificação"
          value={pendingVerification}
          icon={<XCircle className="w-6 h-6" />}
          color="text-orange-600"
          bgColor="bg-orange-100"
        />
        <StatsCard
          title="Total de Coletas"
          value={records.length}
          icon={<TrendingUp className="w-6 h-6" />}
          color="text-indigo-600"
          bgColor="bg-indigo-100"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <RecentSchedules schedules={userSchedules} />
        <RecentRecords records={userRecords} />
      </div>
    </div>
  );
}