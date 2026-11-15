'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import Link from 'next/link';
import { schedulesApi, collectionPointsApi, usersApi } from '@/lib/api';
import { CollectionSchedule, CollectionPoint, User } from '@/types';
import toast from 'react-hot-toast';
import {
  Plus,
  Search,
  Filter,
  Calendar,
  Clock,
  Truck,
  MapPin,
  AlertCircle,
  CheckCircle,
  XCircle,
  Edit,
  Trash2,
  MoreVertical,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
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

export default function SchedulesPage() {
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<ScheduleStatus | 'ALL'>('ALL');
  const [dateFilter, setDateFilter] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingSchedule, setEditingSchedule] = useState<CollectionSchedule | null>(null);
  const queryClient = useQueryClient();

  const { data: schedulesData, isLoading: schedulesLoading } = useQuery({
    queryKey: ['schedules'],
    queryFn: () => schedulesApi.getAllList(),
  });

  const { data: pointsData } = useQuery({
    queryKey: ['collectionPoints'],
    queryFn: () => collectionPointsApi.getAllList(),
  });

  const { data: collectorsData } = useQuery({
    queryKey: ['collectors'],
    queryFn: () => usersApi.getCollectors(),
  });

  const schedules = schedulesData?.data || [];
  const points = pointsData?.data || [];
  const collectors = collectorsData?.data || [];

  const filteredSchedules = schedules.filter((schedule) => {
    const matchesSearch =
      schedule.collectionPointName?.toLowerCase().includes(search.toLowerCase()) ||
      schedule.collectorName?.toLowerCase().includes(search.toLowerCase()) ||
      schedule.notes?.toLowerCase().includes(search.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || schedule.status === statusFilter;
    const matchesDate = !dateFilter || schedule.scheduledDate === dateFilter;
    return matchesSearch && matchesStatus && matchesDate;
  });

  const createMutation = useMutation({
    mutationFn: (data: ScheduleFormData) => schedulesApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schedules'] });
      toast.success('Agendamento criado com sucesso!');
      setIsModalOpen(false);
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao criar agendamento');
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: ScheduleFormData }) =>
      schedulesApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schedules'] });
      toast.success('Agendamento atualizado com sucesso!');
      setIsModalOpen(false);
      setEditingSchedule(null);
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
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao excluir agendamento');
    },
  });

  const confirmMutation = useMutation({
    mutationFn: (id: string) => schedulesApi.confirm(id),
    onSuccess: () => {
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
      queryClient.invalidateQueries({ queryKey: ['schedules'] });
      toast.success('Agendamento cancelado!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao cancelar agendamento');
    },
  });

  const handleSubmit = (data: ScheduleFormData) => {
    if (editingSchedule) {
      updateMutation.mutate({ id: editingSchedule.id, data });
    } else {
      createMutation.mutate(data);
    }
  };

  const handleEdit = (schedule: CollectionSchedule) => {
    setEditingSchedule(schedule);
    setIsModalOpen(true);
  };

  const handleDelete = (id: string) => {
    if (confirm('Tem certeza que deseja excluir este agendamento?')) {
      deleteMutation.mutate(id);
    }
  };

  const handleConfirm = (id: string) => {
    if (confirm('Confirmar este agendamento?')) {
      confirmMutation.mutate(id);
    }
  };

  const handleStart = (id: string) => {
    if (confirm('Iniciar este agendamento?')) {
      startMutation.mutate(id);
    }
  };

  const handleComplete = (id: string) => {
    if (confirm('Marcar como concluído?')) {
      completeMutation.mutate(id);
    }
  };

  const handleCancel = (id: string) => {
    const reason = prompt('Motivo do cancelamento:');
    if (reason) {
      cancelMutation.mutate({ id, reason });
    }
  };

  const openCreateModal = () => {
    setEditingSchedule(null);
    setIsModalOpen(true);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="page-title">Agendamentos</h1>
          <p className="page-subtitle">Gerencie os agendamentos de coleta</p>
        </div>
        <button onClick={openCreateModal} className="btn-primary">
          <Plus className="w-4 h-4 mr-2" />
          Novo Agendamento
        </button>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="flex flex-col sm:flex-row gap-4">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type="text"
                placeholder="Buscar por ponto, coletor, observações..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="input pl-10"
              />
            </div>
            <div className="relative">
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
                <option value="RESCHEDULED">Reagendado</option>
              </select>
            </div>
            <div className="relative">
              <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type="date"
                value={dateFilter}
                onChange={(e) => setDateFilter(e.target.value)}
                className="input pl-10"
              />
            </div>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="card-body p-0">
          {schedulesLoading ? (
            <div className="flex items-center justify-center h-64">
              <div className="spinner w-8 h-8"></div>
            </div>
          ) : (
            <div className="table-container">
              <table className="table">
                <thead>
                  <tr>
                    <th>Ponto de Coleta</th>
                    <th>Coletor</th>
                    <th>Data</th>
                    <th>Horário</th>
                    <th>Status</th>
                    <th>Volume Estimado</th>
                    <th>Observações</th>
                    <th className="text-right">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredSchedules.length === 0 ? (
                    <tr>
                      <td colSpan={8} className="text-center py-12 text-gray-500">
                        <Calendar className="w-12 h-12 mx-auto mb-4 text-gray-300" />
                        <p className="text-lg">Nenhum agendamento encontrado</p>
                        <p className="text-sm">Tente ajustar os filtros ou cadastre um novo agendamento</p>
                      </td>
                    </tr>
                  ) : (
                    filteredSchedules.map((schedule) => (
                      <tr key={schedule.id}>
                        <td>
                          <div>
                            <p className="font-medium text-gray-900">{schedule.collectionPointName}</p>
                          </div>
                        </td>
                        <td className="text-sm text-gray-600">{schedule.collectorName}</td>
                        <td>
                          {format(new Date(schedule.scheduledDate), 'dd/MM/yyyy', { locale: ptBR })}
                          {schedule.isToday && (
                            <span className="ml-2 badge-info">Hoje</span>
                          )}
                          {schedule.isOverdue && schedule.status !== 'COMPLETED' && schedule.status !== 'CANCELLED' && (
                            <span className="ml-2 badge-danger">Atrasado</span>
                          )}
                        </td>
                        <td>{schedule.startTime}{schedule.endTime && ` - ${schedule.endTime}`}</td>
                        <td>
                          <span className={`badge ${SCHEDULE_STATUS_COLORS[schedule.status as ScheduleStatus]}`}>
                            {SCHEDULE_STATUS_LABELS[schedule.status as ScheduleStatus]}
                          </span>
                        </td>
                        <td className="text-sm text-gray-600">
                          {schedule.estimatedVolumeLiters ? `${schedule.estimatedVolumeLiters.toLocaleString('pt-BR')} L` : '-'}
                        </td>
                        <td className="text-sm text-gray-500 max-w-xs truncate">{schedule.notes || '-'}</td>
                        <td className="text-right">
                          <div className="flex items-center justify-end space-x-2">
                            <Link
                              href={`/schedules/${schedule.id}`}
                              className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100"
                              title="Ver detalhes"
                            >
                              <Calendar className="w-5 h-5" />
                            </Link>
                            <button
                              onClick={() => handleEdit(schedule)}
                              className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100"
                              title="Editar"
                              disabled={['COMPLETED', 'CANCELLED'].includes(schedule.status)}
                            >
                              <Edit className="w-5 h-5" />
                            </button>
                            {schedule.status === 'PENDING' && (
                              <button
                                onClick={() => handleConfirm(schedule.id)}
                                className="p-2 rounded-lg text-blue-500 hover:text-blue-700 hover:bg-blue-50"
                                title="Confirmar"
                              >
                                <CheckCircle className="w-5 h-5" />
                              </button>
                            )}
                            {schedule.status === 'CONFIRMED' && (
                              <button
                                onClick={() => handleStart(schedule.id)}
                                className="p-2 rounded-lg text-yellow-500 hover:text-yellow-700 hover:bg-yellow-50"
                                title="Iniciar"
                              >
                                <Truck className="w-5 h-5" />
                              </button>
                            )}
                            {(schedule.status === 'CONFIRMED' || schedule.status === 'IN_PROGRESS') && (
                              <button
                                onClick={() => handleComplete(schedule.id)}
                                className="p-2 rounded-lg text-green-500 hover:text-green-700 hover:bg-green-50"
                                title="Concluir"
                              >
                                <CheckCircle className="w-5 h-5" />
                              </button>
                            )}
                            {['PENDING', 'CONFIRMED', 'IN_PROGRESS'].includes(schedule.status) && (
                              <button
                                onClick={() => handleCancel(schedule.id)}
                                className="p-2 rounded-lg text-red-500 hover:text-red-700 hover:bg-red-50"
                                title="Cancelar"
                              >
                                <XCircle className="w-5 h-5" />
                              </button>
                            )}
                            <button
                              onClick={() => handleDelete(schedule.id)}
                              className="p-2 rounded-lg text-gray-500 hover:text-red-600 hover:bg-red-50"
                              title="Excluir"
                              disabled={schedule.status === 'COMPLETED'}
                            >
                              <Trash2 className="w-5 h-5" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {isModalOpen && (
        <div className="modal-overlay" onClick={() => setIsModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-lg font-semibold text-gray-900">
                {editingSchedule ? 'Editar Agendamento' : 'Novo Agendamento'}
              </h3>
              <button
                onClick={() => setIsModalOpen(false)}
                className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100"
              >
                <XCircle className="w-5 h-5" />
              </button>
            </div>
            <ScheduleForm
              initialData={editingSchedule}
              points={points}
              collectors={collectors}
              onSubmit={handleSubmit}
              onClose={() => {
                setIsModalOpen(false);
                setEditingSchedule(null);
              }}
              isLoading={createMutation.isPending || updateMutation.isPending}
            />
          </div>
        </div>
      )}
    </div>
  );
}

function ScheduleForm({
  initialData,
  points,
  collectors,
  onSubmit,
  onClose,
  isLoading,
}: {
  initialData: CollectionSchedule | null;
  points: CollectionPoint[];
  collectors: User[];
  onSubmit: (data: ScheduleFormData) => void;
  onClose: () => void;
  isLoading: boolean;
}) {
  const { register, handleSubmit, setValue, watch, formState: { errors } } = useForm<ScheduleFormData>({
    resolver: zodResolver(scheduleSchema),
    defaultValues: {
      collectionPointId: '',
      collectorId: '',
      scheduledDate: new Date().toISOString().split('T')[0],
      startTime: '08:00',
      endTime: '12:00',
      notes: '',
      estimatedVolumeLiters: undefined,
    },
  });

  if (initialData) {
    setValue('collectionPointId', initialData.collectionPointId);
    setValue('collectorId', initialData.collectorId);
    setValue('scheduledDate', initialData.scheduledDate);
    setValue('startTime', initialData.startTime);
    setValue('endTime', initialData.endTime || '');
    setValue('notes', initialData.notes || '');
    setValue('estimatedVolumeLiters', initialData.estimatedVolumeLiters);
  }

  const activePoints = points.filter(p => p.status === 'ACTIVE');
  const activeCollectors = collectors.filter(c => c.isActive);

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="modal-body space-y-4 p-6 max-h-[70vh] overflow-y-auto">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="label">Ponto de Coleta *</label>
          <select {...register('collectionPointId')} className={`input ${errors.collectionPointId ? 'input-error' : ''}`}>
            <option value="">Selecione um ponto</option>
            {activePoints.map((point) => (
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
            <option value="">Selecione um coletor</option>
            {activeCollectors.map((collector) => (
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
            initialData ? 'Atualizar' : 'Criar'
          )}
        </button>
      </div>
    </form>
  );
}