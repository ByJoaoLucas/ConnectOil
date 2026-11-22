'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import Link from 'next/link';
import { schedulesApi, collectionPointsApi, usersApi } from '@/lib/api';
import { CollectionSchedule, CollectionPoint, User, SaleType } from '@/types';
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
  CheckCircle2,
  XCircle,
  Edit,
  Trash2,
  MoreVertical,
  ChevronDown,
  ChevronUp,
  Tag,
  Timer,
  AlertTriangle,
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
import { useAuth } from '@/contexts/AuthContext';

interface ScheduleFormData {
  collectionPointId: string;
  collectorId?: string;
  scheduledDate: string;
  startTime: string;
  endTime?: string;
  notes?: string;
  estimatedVolumeLiters?: number;
  saleType: SaleType;
  timeWindow?: string;
}

const TIME_WINDOWS = [
  { value: 'MORNING', label: 'Manhã (08:00-12:00)' },
  { value: 'AFTERNOON', label: 'Tarde (13:00-17:00)' },
  { value: 'TWO_HOUR_WINDOW', label: 'Janela de 2h (horário a combinar)' },
  { value: 'FREE_TIME', label: 'Horário livre' },
];

// Helper para obter data local no formato YYYY-MM-DD (evita bug de fuso UTC)
function getLocalDateString(date: Date = new Date()): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

// Helper para obter hora local no formato HH:MM
function getLocalTimeString(date: Date = new Date()): string {
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${hours}:${minutes}`;
}

// Validação: data/hora não podem ser no passado
function isDateTimeInPast(scheduledDate: string, startTime: string): boolean {
  const now = new Date();
  const [year, month, day] = scheduledDate.split('-').map(Number);
  const [hours, minutes] = startTime.split(':').map(Number);
  const scheduleDateTime = new Date(year, month - 1, day, hours, minutes);
  return scheduleDateTime < now;
}

const scheduleSchema = z.object({
  collectionPointId: z.string().uuid('Selecione um ponto de coleta válido'),
  collectorId: z.string().uuid('Selecione um coletor válido').optional().or(z.literal('')),
  scheduledDate: z.string().min(1, 'Data é obrigatória'),
  startTime: z.string().regex(/^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$/, 'Horário inválido (HH:MM)'),
  endTime: z.string().regex(/^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$/, 'Horário inválido (HH:MM)').optional(),
  notes: z.string().max(500).optional(),
  estimatedVolumeLiters: z.number().min(1).max(9999999).optional(),
  saleType: z.enum(['SALE', 'EXCHANGE']),
  timeWindow: z.string().optional(),
}).refine((data) => !isDateTimeInPast(data.scheduledDate, data.startTime), {
  message: 'Não é possível criar um agendamento para uma data ou horário no passado.',
  path: ['scheduledDate'],
});

export default function SchedulesPage() {
  const { user } = useAuth();
  const isEstablishment = user?.role === 'ESTABLISHMENT';
  const isCollector = user?.role === 'COLLECTOR';
  const isAdmin = user?.role === 'ADMIN';
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<ScheduleStatus | 'ALL'>('ALL');
  const [dateFilter, setDateFilter] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingSchedule, setEditingSchedule] = useState<CollectionSchedule | null>(null);
  const [cancelScheduleId, setCancelScheduleId] = useState<string | null>(null);
  const [cancelReason, setCancelReason] = useState('');
  const queryClient = useQueryClient();

  // Query keys based on role
  const schedulesQueryKey = isEstablishment
    ? ['schedules', 'my']
    : isCollector
    ? ['schedules', 'collector']
    : ['schedules'];
  const pointsQueryKey = isEstablishment ? ['collectionPoints', 'my'] : ['collectionPoints'];

  const { data: schedulesData, isLoading: schedulesLoading } = useQuery({
    queryKey: schedulesQueryKey,
    queryFn: () => {
      if (isEstablishment) return schedulesApi.getEstablishmentSchedulesList();
      if (isCollector) return schedulesApi.getMySchedules();
      return schedulesApi.getAllList();
    },
  });

  const { data: pointsData } = useQuery({
    queryKey: pointsQueryKey,
    queryFn: () => isEstablishment ? collectionPointsApi.getMyPointsList() : collectionPointsApi.getAllList(),
  });

  const { data: collectorsData } = useQuery({
    queryKey: ['collectors'],
    queryFn: () => usersApi.getCollectors(),
    enabled: !isEstablishment,
  });

  const schedules = schedulesData?.data || [];
  const points = pointsData?.data || [];
  const collectors = collectorsData?.data || [];

  const filteredSchedules = schedules.filter((schedule: CollectionSchedule) => {
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
      queryClient.invalidateQueries({ queryKey: schedulesQueryKey });
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
      queryClient.invalidateQueries({ queryKey: schedulesQueryKey });
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
      queryClient.invalidateQueries({ queryKey: schedulesQueryKey });
      toast.success('Agendamento excluído com sucesso!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao excluir agendamento');
    },
  });

  const confirmMutation = useMutation({
    mutationFn: (id: string) => schedulesApi.confirm(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: schedulesQueryKey });
      toast.success('Agendamento confirmado!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao confirmar agendamento');
    },
  });

  const acceptMutation = useMutation({
    mutationFn: (id: string) => schedulesApi.accept(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: schedulesQueryKey });
      toast.success('Coleta aceita com sucesso!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao aceitar coleta');
    },
  });

  const startMutation = useMutation({
    mutationFn: (id: string) => schedulesApi.start(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: schedulesQueryKey });
      toast.success('Agendamento iniciado!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao iniciar agendamento');
    },
  });

  const completeMutation = useMutation({
    mutationFn: (id: string) => schedulesApi.complete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: schedulesQueryKey });
      toast.success('Agendamento concluído!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao concluir agendamento');
    },
  });

  const cancelMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => schedulesApi.cancel(id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: schedulesQueryKey });
      toast.success('Agendamento cancelado!');
      setCancelScheduleId(null);
      setCancelReason('');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao cancelar agendamento');
    },
  });

  const handleSubmit = (data: ScheduleFormData) => {
    // For ESTABLISHMENT, ensure collectorId is not sent
    const payload = isEstablishment ? { ...data, collectorId: undefined } : data;
    if (editingSchedule) {
      updateMutation.mutate({ id: editingSchedule.id, data: payload });
    } else {
      createMutation.mutate(payload);
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

  const handleAccept = (id: string) => {
    if (confirm('Aceitar esta coleta? Você será atribuído como coletor responsável.')) {
      acceptMutation.mutate(id);
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
    setCancelScheduleId(id);
    setCancelReason('');
  };

  const handleCancelConfirm = () => {
    if (cancelScheduleId && cancelReason.trim()) {
      cancelMutation.mutate({ id: cancelScheduleId, reason: cancelReason });
    }
  };

  const openCreateModal = () => {
    setEditingSchedule(null);
    setIsModalOpen(true);
  };

  const getTimeWindowLabel = (value: string | undefined) => {
    if (!value) return '-';
    const tw = TIME_WINDOWS.find(t => t.value === value);
    return tw ? tw.label : value;
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="page-title">Agendamentos</h1>
          <p className="page-subtitle">
            {isEstablishment ? 'Gerencie seus agendamentos de coleta' : 'Gerencie os agendamentos de coleta'}
          </p>
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
                {Object.entries(SCHEDULE_STATUS_LABELS).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
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
                    {!isEstablishment && <th>Coletor</th>}
                    <th>Data</th>
                    <th>Horário</th>
                    <th>Status</th>
                    {isEstablishment && <th>Tipo</th>}
                    {isEstablishment && <th>Janela</th>}
                    <th>Volume Estimado</th>
                    <th>Observações</th>
                    <th className="text-right">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredSchedules.length === 0 ? (
                    <tr>
                      <td colSpan={isEstablishment ? 9 : 8} className="text-center py-12 text-gray-500">
                        <Calendar className="w-12 h-12 mx-auto mb-4 text-gray-300" />
                        <p className="text-lg">Nenhum agendamento encontrado</p>
                        <p className="text-sm">
                          {isEstablishment
                            ? 'Você ainda não possui agendamentos. Cadastre um novo agendamento para seus pontos.'
                            : 'Tente ajustar os filtros ou cadastre um novo agendamento'}
                        </p>
                      </td>
                    </tr>
                  ) : (
                    filteredSchedules.map((schedule: CollectionSchedule) => (
                      <tr key={schedule.id}>
                        <td>
                          <div>
                            <p className="font-medium text-gray-900">{schedule.collectionPointName}</p>
                          </div>
                        </td>
                        {!isEstablishment && (
                          <td className="text-sm text-gray-600">{schedule.collectorName || 'Não atribuído'}</td>
                        )}
                        <td>
                          {format(parseLocalDate(schedule.scheduledDate), 'dd/MM/yyyy', { locale: ptBR })}
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
                        {isEstablishment && (
                          <td>
                            {schedule.saleType && (
                              <span className={`badge ${schedule.saleType === 'SALE' ? 'bg-green-100 text-green-800' : 'bg-purple-100 text-purple-800'}`}>
                                {schedule.saleType === 'SALE' ? 'Venda' : 'Troca'}
                              </span>
                            )}
                          </td>
                        )}
                        {isEstablishment && (
                          <td className="text-sm text-gray-600">{getTimeWindowLabel(schedule.timeWindow)}</td>
                        )}
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
                            {/* Botão "Aceitar Coleta" para COLLECTOR em agendamentos PENDING sem coletor */}
                            {isCollector &&
                              schedule.status === 'PENDING' &&
                              !schedule.collectorId &&
                              (
                                <button
                                  onClick={() => handleAccept(schedule.id)}
                                  className="p-2 rounded-lg text-green-600 hover:text-green-800 hover:bg-green-50"
                                  title="Aceitar Coleta"
                                >
                                  <CheckCircle2 className="w-5 h-5" />
                                </button>
                              )}
                            {/* Botão Confirmar: apenas ADMIN ou COLLECTOR dono do agendamento */}
                            {schedule.status === 'PENDING' &&
                              schedule.collectorId &&
                              (isAdmin || (isCollector && schedule.collectorId === user?.id)) && (
                                <button
                                  onClick={() => handleConfirm(schedule.id)}
                                  className="p-2 rounded-lg text-blue-500 hover:text-blue-700 hover:bg-blue-50"
                                  title="Confirmar"
                                >
                                  <CheckCircle className="w-5 h-5" />
                                </button>
                              )}
                            {/* Botão Iniciar Deslocamento: ADMIN ou COLLECTOR dono - para status CONFIRMED ou ASSIGNED (Atribuída) */}
                            {(schedule.status === 'CONFIRMED' || schedule.status === 'ASSIGNED') &&
                              (isAdmin || (isCollector && schedule.collectorId === user?.id)) && (
                                <button
                                  onClick={() => handleStart(schedule.id)}
                                  className="p-2 rounded-lg text-yellow-500 hover:text-yellow-700 hover:bg-yellow-50"
                                  title="Iniciar Deslocamento (A caminho)"
                                >
                                  <Truck className="w-5 h-5" />
                                </button>
                              )}
                            {/* Botão Concluir Coleta: ADMIN ou COLLECTOR dono - para status IN_PROGRESS (A caminho) */}
                            {schedule.status === 'IN_PROGRESS' &&
                              (isAdmin || (isCollector && schedule.collectorId === user?.id)) && (
                                <button
                                  onClick={() => handleComplete(schedule.id)}
                                  className="p-2 rounded-lg text-green-500 hover:text-green-700 hover:bg-green-50"
                                  title="Concluir Coleta"
                                >
                                  <CheckCircle className="w-5 h-5" />
                                </button>
                              )}
                            {/* Botão Cancelar: todos podem cancelar conforme regras de negócio */}
                            {['PENDING', 'CONFIRMED', 'IN_PROGRESS'].includes(schedule.status) && (
                              <button
                                onClick={() => handleCancel(schedule.id)}
                                className="p-2 rounded-lg text-red-500 hover:text-red-700 hover:bg-red-50"
                                title="Cancelar"
                              >
                                <XCircle className="w-5 h-5" />
                              </button>
                            )}
                            {/* Botão Editar: apenas ADMIN */}
                            {isAdmin && (
                              <button
                                onClick={() => handleEdit(schedule)}
                                className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100"
                                title="Editar"
                                disabled={['COMPLETED', 'CANCELLED'].includes(schedule.status)}
                              >
                                <Edit className="w-5 h-5" />
                              </button>
                            )}
                            {/* Botão Excluir: apenas ADMIN */}
                            {isAdmin && (
                              <button
                                onClick={() => handleDelete(schedule.id)}
                                className="p-2 rounded-lg text-gray-500 hover:text-red-600 hover:bg-red-50"
                                title="Excluir"
                                disabled={schedule.status === 'COMPLETED'}
                              >
                                <Trash2 className="w-5 h-5" />
                              </button>
                            )}
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
              isEstablishment={isEstablishment}
              isAdmin={isAdmin}
              isEditing={!!editingSchedule}
            />
          </div>
        </div>
      )}

      {cancelScheduleId && (
        <div className="modal-overlay" onClick={() => { setCancelScheduleId(null); setCancelReason(''); }}>
          <div className="modal-content modal-sm" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-lg font-semibold text-gray-900">Cancelar Agendamento</h3>
              <button
                onClick={() => { setCancelScheduleId(null); setCancelReason(''); }}
                className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100"
              >
                <XCircle className="w-5 h-5" />
              </button>
            </div>
            <div className="modal-body p-6 space-y-4">
              <div className="flex items-start gap-3 p-3 bg-amber-50 border border-amber-200 rounded-lg">
                <AlertTriangle className="w-5 h-5 text-amber-600 mt-0.5 flex-shrink-0" />
                <div>
                  <p className="font-medium text-amber-800">
                    {isEstablishment
                      ? 'Atenção: Como estabelecimento, você só pode cancelar agendamentos com pelo menos 2 horas de antecedência.'
                      : 'Como coletor, você pode cancelar a qualquer momento, mas deve informar o motivo.'}
                  </p>
                  <p className="text-sm text-amber-700 mt-1">
                    {isEstablishment
                      ? 'Verifique o horário do agendamento antes de confirmar o cancelamento.'
                      : 'O motivo é obrigatório para o cancelamento pelo coletor.'}
                  </p>
                </div>
              </div>
              <div>
                <label className="label">Motivo do cancelamento *</label>
                <textarea
                  value={cancelReason}
                  onChange={(e) => setCancelReason(e.target.value)}
                  className="input"
                  rows={3}
                  placeholder={isEstablishment ? 'Ex: Emergência no estabelecimento, mudança de planos...' : 'Ex: Imprevisto, veículo quebrado, doença...'}
                  required
                />
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  onClick={() => { setCancelScheduleId(null); setCancelReason(''); }}
                  className="btn-outline"
                >
                  Voltar
                </button>
                <button
                  type="button"
                  onClick={handleCancelConfirm}
                  disabled={!cancelReason.trim() || cancelMutation.isPending}
                  className="btn-danger"
                >
                  {cancelMutation.isPending ? (
                    <span className="flex items-center space-x-2">
                      <div className="spinner w-4 h-4"></div>
                      <span>Cancelando...</span>
                    </span>
                  ) : (
                    'Confirmar Cancelamento'
                  )}
                </button>
              </div>
            </div>
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
  isEstablishment,
  isAdmin,
  isEditing,
}: {
  initialData: CollectionSchedule | null;
  points: CollectionPoint[];
  collectors: User[];
  onSubmit: (data: ScheduleFormData) => void;
  onClose: () => void;
  isLoading: boolean;
  isEstablishment: boolean;
  isAdmin: boolean;
  isEditing: boolean;
}) {
  const { register, handleSubmit, setValue, watch, formState: { errors } } = useForm<ScheduleFormData>({
    resolver: zodResolver(scheduleSchema),
    defaultValues: {
      collectionPointId: '',
      collectorId: '',
      scheduledDate: getLocalDateString(),
      startTime: getLocalTimeString(new Date(Date.now() + 60 * 60 * 1000)), // Próxima hora cheia
      endTime: getLocalTimeString(new Date(Date.now() + 2 * 60 * 60 * 1000)),
      notes: '',
      estimatedVolumeLiters: undefined,
      saleType: 'SALE',
      timeWindow: '',
    },
  });

  if (initialData) {
    setValue('collectionPointId', initialData.collectionPointId);
    setValue('collectorId', initialData.collectorId || '');
    setValue('scheduledDate', String(initialData.scheduledDate).slice(0, 10));
    // Backend returns HH:mm:ss, slice to HH:mm for time input
    setValue('startTime', initialData.startTime ? initialData.startTime.slice(0, 5) : '');
    setValue('endTime', initialData.endTime ? initialData.endTime.slice(0, 5) : '');
    setValue('notes', initialData.notes || '');
    setValue('estimatedVolumeLiters', initialData.estimatedVolumeLiters);
    setValue('saleType', initialData.saleType || 'SALE');
    setValue('timeWindow', initialData.timeWindow || '');
  }

  const activePoints = points.filter(p => p.status === 'ACTIVE');
  const activeCollectors = collectors.filter(c => c.isActive);

  const watchedSaleType = watch('saleType');
  const watchedTimeWindow = watch('timeWindow');

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="modal-body space-y-4 p-6 max-h-[70vh] overflow-y-auto">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label htmlFor="collectionPointId" className="label">Ponto de Coleta *</label>
          <select id="collectionPointId" {...register('collectionPointId')} className={`input ${errors.collectionPointId ? 'input-error' : ''}`}>
            <option value="">Selecione um ponto</option>
            {activePoints.map((point) => (
              <option key={point.id} value={point.id}>
                {point.name} - {point.city}/{point.state}
              </option>
            ))}
          </select>
          {errors.collectionPointId && <p className="error-message">{errors.collectionPointId.message}</p>}
        </div>

        {isAdmin && (
          <div>
            <label htmlFor="collectorId" className="label">Coletor *</label>
            <select id="collectorId" {...register('collectorId')} className={`input ${errors.collectorId ? 'input-error' : ''}`}>
              <option value="">Selecione um coletor</option>
              {activeCollectors.map((collector) => (
                <option key={collector.id} value={collector.id}>
                  {collector.name} ({collector.email})
                </option>
              ))}
            </select>
            {errors.collectorId && <p className="error-message">{errors.collectorId.message}</p>}
          </div>
        )}

        <div>
          <label htmlFor="scheduledDate" className="label">Data *</label>
          <input
            id="scheduledDate"
            type="date"
            {...register('scheduledDate')}
            className={`input ${errors.scheduledDate ? 'input-error' : ''}`}
            min={isEditing || isAdmin ? undefined : getLocalDateString()}
          />
          {errors.scheduledDate && <p className="error-message">{errors.scheduledDate.message}</p>}
        </div>

        <div>
          <label htmlFor="startTime" className="label">Horário Início *</label>
          <input
            id="startTime"
            type="time"
            {...register('startTime')}
            className={`input ${errors.startTime ? 'input-error' : ''}`}
          />
          {errors.startTime && <p className="error-message">{errors.startTime.message}</p>}
        </div>

        <div>
          <label htmlFor="endTime" className="label">Horário Fim</label>
          <input id="endTime" type="time" {...register('endTime')} className="input" />
        </div>

        {isEstablishment && (
          <div>
            <label htmlFor="saleType" className="label">Tipo de Operação *</label>
            <select id="saleType" {...register('saleType')} className={`input ${errors.saleType ? 'input-error' : ''}`}>
              <option value="SALE">Venda</option>
              <option value="EXCHANGE">Troca</option>
            </select>
            {errors.saleType && <p className="error-message">{errors.saleType.message}</p>}
          </div>
        )}

        {isEstablishment && (
          <div>
            <label htmlFor="timeWindow" className="label">Janela de Horário</label>
            <select id="timeWindow" {...register('timeWindow')} className="input">
              <option value="">Selecione (opcional)</option>
              {TIME_WINDOWS.map((tw) => (
                <option key={tw.value} value={tw.value}>{tw.label}</option>
              ))}
            </select>
          </div>
        )}

        <div>
          <label htmlFor="estimatedVolumeLiters" className="label">Volume Estimado (L)</label>
          <input
            id="estimatedVolumeLiters"
            type="number"
            min="1"
            max="9999999"
            {...register('estimatedVolumeLiters', { valueAsNumber: true })}
            className="input"
            placeholder="Ex: 500"
            maxLength={7}
          />
        </div>

        <div className="md:col-span-2">
          <label htmlFor="notes" className="label">Observações</label>
          <textarea id="notes" {...register('notes')} className="input" rows={3} placeholder="Observações adicionais..." />
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