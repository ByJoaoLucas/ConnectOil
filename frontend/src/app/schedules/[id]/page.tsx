'use client';

import { useParams } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { schedulesApi, recordsApi, collectionPointsApi, usersApi } from '@/lib/api';
import { CollectionSchedule, CollectionRecord, CollectionPoint, User as UserType } from '@/types';
import toast from 'react-hot-toast';
import {
  Calendar,
  Clock,
  Truck,
  MapPin,
  Package,
  User,
  AlertCircle,
  CheckCircle,
  XCircle,
  Edit,
  Trash2,
  ArrowLeft,
  MoreVertical,
  ChevronDown,
  ChevronUp,
  RefreshCw,
} from 'lucide-react';
import { format, parse } from 'date-fns';
import { ptBR } from 'date-fns/locale';

// Helper para parsear data YYYY-MM-DD como data local (evita bug de fuso UTC)
function parseLocalDate(dateString: string): Date {
  return parse(dateString, 'yyyy-MM-dd', new Date());
}
import { SCHEDULE_STATUS_LABELS, SCHEDULE_STATUS_COLORS, ScheduleStatus } from '@/types';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

interface ScheduleFormData {
  collectionPointId: string;
  collectorId: string;
  scheduledDate: string;
  startTime: string;
  endTime: string;
  notes?: string;
  estimatedVolumeLiters?: number;
}

const scheduleSchema = z.object({
  collectionPointId: z.string().uuid('Selecione um ponto de coleta válido'),
  collectorId: z.string().uuid('Selecione um coletor válido'),
  scheduledDate: z.string().min(1, 'Data é obrigatória'),
  startTime: z.string().regex(/^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$/, 'Horário inválido (HH:MM)'),
  endTime: z.string().regex(/^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$/, 'Horário inválido (HH:MM)').optional(),
  notes: z.string().max(500).optional(),
  estimatedVolumeLiters: z.number().min(1).optional(),
});

export default function ScheduleDetailPage() {
  const params = useParams();
  const router = useRouter();
  const queryClient = useQueryClient();
  const id = params?.id as string;
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);

  const { data: scheduleData, isLoading: scheduleLoading, error: scheduleError } = useQuery({
    queryKey: ['schedule', id],
    queryFn: () => schedulesApi.getById(id),
    enabled: !!id,
  });

  const { data: recordsData, isLoading: recordsLoading } = useQuery({
    queryKey: ['scheduleRecords', id],
    queryFn: () => recordsApi.getByCollectorAndDateRange(scheduleData?.data?.collectorId || '', scheduleData?.data?.scheduledDate || '', scheduleData?.data?.scheduledDate || ''),
    enabled: !!scheduleData?.data,
  });

  const { data: pointsData } = useQuery({
    queryKey: ['collectionPoints'],
    queryFn: () => collectionPointsApi.getAllList(),
  });

  const { data: collectorsData } = useQuery({
    queryKey: ['collectors'],
    queryFn: () => usersApi.getCollectors(),
  });

  const schedule = scheduleData?.data;
  const records = recordsData?.data || [];
  const points = pointsData?.data || [];
  const collectors = collectorsData?.data || [];

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: ScheduleFormData }) =>
      schedulesApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schedule', id] });
      queryClient.invalidateQueries({ queryKey: ['schedules'] });
      toast.success('Agendamento atualizado com sucesso!');
      setIsEditModalOpen(false);
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao atualizar agendamento');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => schedulesApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schedules'] });
      toast.success('Agendamento excluído com sucesso!');
      router.push('/schedules');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao excluir agendamento');
    },
  });

  const confirmMutation = useMutation({
    mutationFn: (id: string) => schedulesApi.confirm(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schedule', id] });
      queryClient.invalidateQueries({ queryKey: ['schedules'] });
      toast.success('Agendamento confirmado!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao confirmar agendamento');
    },
  });

  const startMutation = useMutation({
    mutationFn: (id: string) => schedulesApi.start(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schedule', id] });
      queryClient.invalidateQueries({ queryKey: ['schedules'] });
      toast.success('Agendamento iniciado!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao iniciar agendamento');
    },
  });

  const completeMutation = useMutation({
    mutationFn: (id: string) => schedulesApi.complete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schedule', id] });
      queryClient.invalidateQueries({ queryKey: ['schedules'] });
      toast.success('Agendamento concluído!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao concluir agendamento');
    },
  });

  const cancelMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => schedulesApi.cancel(id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schedule', id] });
      queryClient.invalidateQueries({ queryKey: ['schedules'] });
      toast.success('Agendamento cancelado!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao cancelar agendamento');
    },
  });

  const handleSubmit = (data: ScheduleFormData) => {
    updateMutation.mutate({ id, data });
  };

  const handleDelete = () => {
    if (confirm('Tem certeza que deseja excluir este agendamento?')) {
      deleteMutation.mutate(id);
    }
  };

  const handleConfirm = () => {
    if (confirm('Confirmar este agendamento?')) {
      confirmMutation.mutate(id);
    }
  };

  const handleStart = () => {
    if (confirm('Iniciar este agendamento?')) {
      startMutation.mutate(id);
    }
  };

  const handleComplete = () => {
    if (confirm('Marcar como concluído?')) {
      completeMutation.mutate(id);
    }
  };

  const handleCancel = () => {
    const reason = prompt('Motivo do cancelamento:');
    if (reason) {
      cancelMutation.mutate({ id, reason });
    }
  };

  if (scheduleLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="spinner w-8 h-8"></div>
      </div>
    );
  }

  if (scheduleError || !schedule) {
    return (
      <div className="card">
        <div className="card-body text-center py-12">
          <AlertCircle className="w-12 h-12 mx-auto mb-4 text-red-500" />
          <h3 className="text-lg font-semibold text-gray-900">Agendamento não encontrado</h3>
          <p className="text-gray-500 mt-2">O agendamento solicitado não existe ou foi removido.</p>
          <button onClick={() => router.push('/schedules')} className="btn-primary mt-4 inline-flex">
            <ArrowLeft className="w-4 h-4 mr-2" />
            Voltar para lista
          </button>
        </div>
      </div>
    );
  }

  const activePoints = points.filter((p: CollectionPoint) => p.status === 'ACTIVE');
  const activeCollectors = collectors.filter((c: UserType) => c.isActive);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <button onClick={() => router.back()} className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100">
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="page-title">Agendamento #{schedule.id.slice(0, 8)}</h1>
            <p className="page-subtitle">{schedule.collectionPointName}</p>
          </div>
        </div>
        <div className="flex items-center space-x-2">
          <span className={`badge ${SCHEDULE_STATUS_COLORS[schedule.status as ScheduleStatus]}`}>
            {SCHEDULE_STATUS_LABELS[schedule.status as ScheduleStatus]}
          </span>
          {schedule.isOverdue && schedule.status !== 'COMPLETED' && schedule.status !== 'CANCELLED' && (
            <span className="badge badge-danger">Atrasado</span>
          )}
          {schedule.isToday && (
            <span className="badge badge-info">Hoje</span>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <div className="card">
            <div className="card-header flex items-center justify-between">
              <h3 className="text-lg font-semibold text-gray-900">Informações do Agendamento</h3>
              <button
                onClick={() => setIsEditModalOpen(true)}
                disabled={['COMPLETED', 'CANCELLED'].includes(schedule.status)}
                className="btn-outline text-sm"
              >
                <Edit className="w-4 h-4 mr-2" />
                Editar
              </button>
            </div>
            <div className="card-body">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Ponto de Coleta</label>
                  <div className="mt-1 flex items-center space-x-2">
                    <MapPin className="w-5 h-5 text-gray-400" />
                    <span className="font-medium text-gray-900">{schedule.collectionPointName}</span>
                  </div>
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Coletor</label>
                  <div className="mt-1 flex items-center space-x-2">
                    <User className="w-5 h-5 text-gray-400" />
                    <span className="font-medium text-gray-900">{schedule.collectorName}</span>
                  </div>
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Data</label>
                  <div className="mt-1 flex items-center space-x-2">
                    <Calendar className="w-5 h-5 text-gray-400" />
                    <span className="font-medium text-gray-900">
                      {format(parseLocalDate(schedule.scheduledDate), 'EEEE, dd/MM/yyyy', { locale: ptBR })}
                    </span>
                  </div>
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Horário</label>
                  <div className="mt-1 flex items-center space-x-2">
                    <Clock className="w-5 h-5 text-gray-400" />
                    <span className="font-medium text-gray-900">
                      {schedule.startTime}{schedule.endTime && ` - ${schedule.endTime}`}
                    </span>
                  </div>
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Volume Estimado</label>
                  <div className="mt-1 flex items-center space-x-2">
                    <Package className="w-5 h-5 text-gray-400" />
                    <span className="font-medium text-gray-900">
                      {schedule.estimatedVolumeLiters ? `${schedule.estimatedVolumeLiters.toLocaleString('pt-BR')} L` : 'Não informado'}
                    </span>
                  </div>
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Status</label>
                  <div className="mt-1 flex items-center space-x-2">
                    <span className={`badge ${SCHEDULE_STATUS_COLORS[schedule.status as ScheduleStatus]}`}>
                      {SCHEDULE_STATUS_LABELS[schedule.status as ScheduleStatus]}
                    </span>
                  </div>
                </div>
              </div>

              {schedule.notes && (
                <div className="mt-6 pt-6 border-t border-gray-200">
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Observações</label>
                  <p className="mt-1 text-sm text-gray-900">{schedule.notes}</p>
                </div>
              )}

              <div className="mt-6 pt-6 border-t border-gray-200">
                <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Datas Importantes</label>
                <div className="mt-2 grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <p className="text-gray-500">Criado em</p>
                    <p className="font-medium text-gray-900">{schedule.createdAt ? format(new Date(schedule.createdAt), 'dd/MM/yyyy HH:mm', { locale: ptBR }) : '-'}</p>
                  </div>
                  <div>
                    <p className="text-gray-500">Atualizado em</p>
                    <p className="font-medium text-gray-900">{schedule.updatedAt ? format(new Date(schedule.updatedAt), 'dd/MM/yyyy HH:mm', { locale: ptBR }) : '-'}</p>
                  </div>
                  {schedule.completedAt && (
                    <div>
                      <p className="text-gray-500">Concluído em</p>
                      <p className="font-medium text-gray-900">{format(new Date(schedule.completedAt), 'dd/MM/yyyy HH:mm', { locale: ptBR })}</p>
                    </div>
                  )}
                  {schedule.cancelledAt && (
                    <div>
                      <p className="text-gray-500">Cancelado em</p>
                      <p className="font-medium text-gray-900">{format(new Date(schedule.cancelledAt), 'dd/MM/yyyy HH:mm', { locale: ptBR })}</p>
                    </div>
                  )}
                  {schedule.cancellationReason && (
                    <div className="col-span-2">
                      <p className="text-gray-500">Motivo do Cancelamento</p>
                      <p className="font-medium text-gray-900">{schedule.cancellationReason}</p>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card-header flex items-center justify-between">
              <h3 className="text-lg font-semibold text-gray-900">Registros de Coleta Relacionados</h3>
            </div>
            <div className="card-body p-0">
              {recordsLoading ? (
                <div className="flex items-center justify-center h-48">
                  <div className="spinner w-6 h-6"></div>
                </div>
              ) : records.length === 0 ? (
                <div className="text-center py-8 text-gray-500">
                  <Package className="w-12 h-12 mx-auto mb-4 text-gray-300" />
                  <p>Nenhum registro de coleta vinculado a este agendamento</p>
                </div>
              ) : (
                <div className="table-container">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Data/Hora</th>
                        <th>Volume (L)</th>
                        <th>Tipo de Óleo</th>
                        <th>Qualidade</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {records.map((record: CollectionRecord) => (
                        <tr key={record.id}>
                          <td>{format(new Date(record.collectionDate), 'dd/MM/yyyy HH:mm', { locale: ptBR })}</td>
                          <td className="font-medium">{record.volumeCollectedLiters.toLocaleString('pt-BR')}</td>
                          <td className="capitalize">{record.oilType || '-'}</td>
                          <td>
                            {record.qualityRating ? (
                              <div className="flex items-center space-x-1">
                                {[1, 2, 3, 4, 5].map((star) => (
                                  <span
                                    key={star}
                                    className={`text-lg ${
                                      star <= (record.qualityRating || 0) ? 'text-yellow-400' : 'text-gray-300'
                                    }`}
                                  >
                                    ★
                                  </span>
                                ))}
                              </div>
                            ) : (
                              '-'
                            )}
                          </td>
                          <td>
                            <span className={record.verifiedByManager ? 'badge-success' : 'badge-warning'}>
                              {record.verifiedByManager ? 'Verificado' : 'Pendente'}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        </div>

        <div className="space-y-6">
          <div className="card">
            <div className="card-header">
              <h3 className="text-lg font-semibold text-gray-900">Ações</h3>
            </div>
            <div className="card-body space-y-3">
              {schedule.status === 'PENDING' && (
                <button
                  onClick={handleConfirm}
                  disabled={confirmMutation.isPending}
                  className="btn-primary w-full justify-center"
                >
                  <CheckCircle className="w-4 h-4 mr-2" />
                  {confirmMutation.isPending ? 'Confirmando...' : 'Confirmar Agendamento'}
                </button>
              )}
              {schedule.status === 'CONFIRMED' && (
                <button
                  onClick={handleStart}
                  disabled={startMutation.isPending}
                  className="btn-warning w-full justify-center"
                >
                  <Truck className="w-4 h-4 mr-2" />
                  {startMutation.isPending ? 'Iniciando...' : 'Iniciar Coleta'}
                </button>
              )}
              {(schedule.status === 'CONFIRMED' || schedule.status === 'IN_PROGRESS') && (
                <button
                  onClick={handleComplete}
                  disabled={completeMutation.isPending}
                  className="btn-success w-full justify-center"
                >
                  <CheckCircle className="w-4 h-4 mr-2" />
                  {completeMutation.isPending ? 'Concluindo...' : 'Marcar como Concluído'}
                </button>
              )}
              {['PENDING', 'CONFIRMED', 'IN_PROGRESS'].includes(schedule.status) && (
                <button
                  onClick={handleCancel}
                  disabled={cancelMutation.isPending}
                  className="btn-danger w-full justify-center"
                >
                  <XCircle className="w-4 h-4 mr-2" />
                  {cancelMutation.isPending ? 'Cancelando...' : 'Cancelar Agendamento'}
                </button>
              )}
              {schedule.status !== 'COMPLETED' && (
                <button
                  onClick={handleDelete}
                  disabled={deleteMutation.isPending}
                  className="btn-outline w-full justify-center text-red-600 border-red-300 hover:bg-red-50"
                >
                  <Trash2 className="w-4 h-4 mr-2" />
                  {deleteMutation.isPending ? 'Excluindo...' : 'Excluir Agendamento'}
                </button>
              )}
            </div>
          </div>

          <div className="card">
            <div className="card-header">
              <h3 className="text-lg font-semibold text-gray-900">Resumo</h3>
            </div>
            <div className="card-body space-y-4">
              <StatItem label="Volume Estimado" value={schedule.estimatedVolumeLiters ? `${schedule.estimatedVolumeLiters.toLocaleString('pt-BR')} L` : 'Não informado'} icon={<Package className="w-5 h-5" />} color="text-blue-600" />
              <StatItem label="Volume Coletado" value={`${records.reduce((sum: number, r: CollectionRecord) => sum + r.volumeCollectedLiters, 0).toLocaleString('pt-BR')} L`} icon={<Truck className="w-5 h-5" />} color="text-green-600" />
              <StatItem label="Total de Coletas" value={records.length} icon={<Calendar className="w-5 h-5" />} color="text-purple-600" />
              <StatItem label="Qualidade Média" value={records.length > 0 ? (records.reduce((sum: number, r: CollectionRecord) => sum + (r.qualityRating || 0), 0) / records.filter((r: CollectionRecord) => r.qualityRating).length).toFixed(1) : '-'} icon={<CheckCircle className="w-5 h-5" />} color="text-yellow-600" />
            </div>
          </div>
        </div>
      </div>

      {isEditModalOpen && (
        <div className="modal-overlay" onClick={() => setIsEditModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-lg font-semibold text-gray-900">Editar Agendamento</h3>
              <button
                onClick={() => setIsEditModalOpen(false)}
                className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100"
              >
                <XCircle className="w-5 h-5" />
              </button>
            </div>
            <ScheduleEditForm
              schedule={schedule}
              points={activePoints}
              collectors={activeCollectors}
              onSubmit={handleSubmit}
              onClose={() => setIsEditModalOpen(false)}
              isLoading={updateMutation.isPending}
            />
          </div>
        </div>
      )}
    </div>
  );
}

function ScheduleEditForm({
  schedule,
  points,
  collectors,
  onSubmit,
  onClose,
  isLoading,
}: {
  schedule: CollectionSchedule;
  points: CollectionPoint[];
  collectors: UserType[];
  onSubmit: (data: ScheduleFormData) => void;
  onClose: () => void;
  isLoading: boolean;
}) {
  const { register, handleSubmit, setValue, formState: { errors } } = useForm<ScheduleFormData>({
    resolver: zodResolver(scheduleSchema),
    defaultValues: {
      collectionPointId: schedule.collectionPointId,
      collectorId: schedule.collectorId,
      scheduledDate: schedule.scheduledDate,
      startTime: schedule.startTime,
      endTime: schedule.endTime || '',
      notes: schedule.notes || '',
      estimatedVolumeLiters: schedule.estimatedVolumeLiters,
    },
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="modal-body space-y-4 p-6 max-h-[70vh] overflow-y-auto">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="label">Ponto de Coleta *</label>
          <select {...register('collectionPointId')} className={`input ${errors.collectionPointId ? 'input-error' : ''}`}>
            {points.map((point) => (
              <option key={point.id} value={point.id}>
                {point.name} - {point.city}/{point.state}
              </option>
            ))}
          </select>
          {errors.collectionPointId && <p className="error-message">{errors.collectionPointId.message}</p>}
        </div>

        <div>
          <label className="label">Coletor *</label>
          <select {...register('collectorId')} className={`input ${errors.collectorId ? 'input-error' : ''}`}>
            {collectors.map((collector) => (
              <option key={collector.id} value={collector.id}>
                {collector.name} ({collector.email})
              </option>
            ))}
          </select>
          {errors.collectorId && <p className="error-message">{errors.collectorId.message}</p>}
        </div>

        <div>
          <label className="label">Data *</label>
          <input
            type="date"
            {...register('scheduledDate')}
            className={`input ${errors.scheduledDate ? 'input-error' : ''}`}
            min={new Date().toISOString().split('T')[0]}
          />
          {errors.scheduledDate && <p className="error-message">{errors.scheduledDate.message}</p>}
        </div>

        <div>
          <label className="label">Horário Início *</label>
          <input
            type="time"
            {...register('startTime')}
            className={`input ${errors.startTime ? 'input-error' : ''}`}
          />
          {errors.startTime && <p className="error-message">{errors.startTime.message}</p>}
        </div>

        <div>
          <label className="label">Horário Fim</label>
          <input type="time" {...register('endTime')} className="input" />
        </div>

        <div>
          <label className="label">Volume Estimado (L)</label>
          <input
            type="number"
            min="1"
            {...register('estimatedVolumeLiters', { valueAsNumber: true })}
            className="input"
            placeholder="Ex: 500"
          />
        </div>

        <div className="md:col-span-2">
          <label className="label">Observações</label>
          <textarea {...register('notes')} className="input" rows={3} placeholder="Observações adicionais..." />
        </div>
      </div>

      <div className="modal-footer">
        <button type="button" onClick={onClose} className="btn-outline">
          Cancelar
        </button>
        <button type="submit" disabled={isLoading} className="btn-primary">
          {isLoading ? (
            <span className="flex items-center space-x-2">
              <div className="spinner w-4 h-4"></div>
              <span>Salvando...</span>
            </span>
          ) : (
            'Atualizar'
          )}
        </button>
      </div>
    </form>
  );
}

function StatItem({ label, value, icon, color }: { label: string; value: string; icon: React.ReactNode; color: string }) {
  return (
    <div className="flex items-center space-x-3 p-3 bg-gray-50 rounded-lg">
      <div className={`p-2 rounded-lg bg-white ${color}`}>{icon}</div>
      <div className="flex-1 min-w-0">
        <p className="text-xs font-medium text-gray-500 truncate">{label}</p>
        <p className="text-sm font-semibold text-gray-900 truncate">{value}</p>
      </div>
    </div>
  );
}

