'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { incidentsApi, collectionPointsApi, usersApi } from '@/lib/api';
import { Incident, CollectionPoint, User as UserType } from '@/types';
import toast from 'react-hot-toast';
import {
  Plus,
  Search,
  Filter,
  AlertTriangle,
  CheckCircle,
  XCircle,
  Edit,
  Trash2,
  MapPin,
  User,
  Calendar,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

interface IncidentFormData {
  collectionPointId: string;
  scheduleId?: string;
  incidentType: string;
  description?: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  photos?: string;
  gpsLatitude?: number;
  gpsLongitude?: number;
}

const incidentSchema = z.object({
  collectionPointId: z.string().uuid('Selecione um ponto de coleta válido'),
  scheduleId: z.string().uuid().optional().or(z.literal('')),
  incidentType: z.string().min(2, 'Tipo de incidente é obrigatório'),
  description: z.string().max(1000).optional(),
  severity: z.enum(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']),
  photos: z.string().optional(),
  gpsLatitude: z.number().min(-90).max(90).optional(),
  gpsLongitude: z.number().min(-180).max(180).optional(),
});

const INCIDENT_TYPES = ['Vazamento', 'Contaminação', 'Equipamento Quebrado', 'Acesso Bloqueado', 'Coleta não realizada', 'Outro'];

export default function IncidentsPage() {
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED'>('ALL');
  const [severityFilter, setSeverityFilter] = useState<'ALL' | 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'>('ALL');
  const [pointFilter, setPointFilter] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingIncident, setEditingIncident] = useState<Incident | null>(null);
  const queryClient = useQueryClient();

  const { data: incidentsData, isLoading: incidentsLoading } = useQuery({
    queryKey: ['incidents'],
    queryFn: () => incidentsApi.getAllList(),
  });

  const { data: pointsData } = useQuery({
    queryKey: ['collectionPoints'],
    queryFn: () => collectionPointsApi.getAllList(),
  });

  const incidents = incidentsData?.data || [];
  const points = pointsData?.data || [];

  const filteredIncidents = incidents.filter((incident: Incident) => {
    const matchesSearch =
      incident.incidentType.toLowerCase().includes(search.toLowerCase()) ||
      incident.description?.toLowerCase().includes(search.toLowerCase()) ||
      incident.collectionPointName?.toLowerCase().includes(search.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || incident.status === statusFilter;
    const matchesSeverity = severityFilter === 'ALL' || incident.severity === severityFilter;
    const matchesPoint = !pointFilter || incident.collectionPointId === pointFilter;
    return matchesSearch && matchesStatus && matchesSeverity && matchesPoint;
  });

  const createMutation = useMutation({
    mutationFn: (data: IncidentFormData) => incidentsApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['incidents'] });
      toast.success('Incidente reportado com sucesso!');
      setIsModalOpen(false);
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao reportar incidente');
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: IncidentFormData }) =>
      incidentsApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['incidents'] });
      toast.success('Incidente atualizado com sucesso!');
      setIsModalOpen(false);
      setEditingIncident(null);
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao atualizar incidente');
    },
  });

  const resolveMutation = useMutation({
    mutationFn: ({ id, resolutionNotes }: { id: string; resolutionNotes: string }) =>
      incidentsApi.resolve(id, resolutionNotes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['incidents'] });
      toast.success('Incidente resolvido com sucesso!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao resolver incidente');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => incidentsApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['incidents'] });
      toast.success('Incidente excluído com sucesso!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao excluir incidente');
    },
  });

  const handleSubmit = (data: IncidentFormData) => {
    const cleanData = { ...data };
    if (!cleanData.scheduleId) delete cleanData.scheduleId;
    if (editingIncident) {
      updateMutation.mutate({ id: editingIncident.id, data: cleanData });
    } else {
      createMutation.mutate(cleanData);
    }
  };

  const handleEdit = (incident: Incident) => {
    setEditingIncident(incident);
    setIsModalOpen(true);
  };

  const handleDelete = (id: string) => {
    if (confirm('Tem certeza que deseja excluir este incidente?')) {
      deleteMutation.mutate(id);
    }
  };

  const handleResolve = (incident: Incident) => {
    const notes = prompt('Observações da resolução:');
    if (notes !== null) {
      resolveMutation.mutate({ id: incident.id, resolutionNotes: notes || 'Resolvido sem observações' });
    }
  };

  const openCreateModal = () => {
    setEditingIncident(null);
    setIsModalOpen(true);
  };

  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case 'CRITICAL': return 'bg-red-100 text-red-800';
      case 'HIGH': return 'bg-orange-100 text-orange-800';
      case 'MEDIUM': return 'bg-yellow-100 text-yellow-800';
      case 'LOW': return 'bg-green-100 text-green-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getSeverityLabel = (severity: string) => {
    switch (severity) {
      case 'CRITICAL': return 'Crítico';
      case 'HIGH': return 'Alto';
      case 'MEDIUM': return 'Médio';
      case 'LOW': return 'Baixo';
      default: return severity;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'OPEN': return 'bg-red-100 text-red-800';
      case 'IN_PROGRESS': return 'bg-yellow-100 text-yellow-800';
      case 'RESOLVED': return 'bg-green-100 text-green-800';
      case 'CLOSED': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'OPEN': return 'Aberto';
      case 'IN_PROGRESS': return 'Em Andamento';
      case 'RESOLVED': return 'Resolvido';
      case 'CLOSED': return 'Fechado';
      default: return status;
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="page-title">Incidentes</h1>
          <p className="page-subtitle">Gerencie incidentes e problemas nos pontos de coleta</p>
        </div>
        <button onClick={openCreateModal} className="btn-primary">
          <Plus className="w-4 h-4 mr-2" />
          Reportar Incidente
        </button>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="flex flex-col sm:flex-row gap-4">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type="text"
                placeholder="Buscar por tipo, descrição, ponto..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="input pl-10"
              />
            </div>
            <div className="relative">
              <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value as typeof statusFilter)}
                className="input pl-10 pr-10 appearance-none"
              >
                <option value="ALL">Todos os status</option>
                <option value="OPEN">Aberto</option>
                <option value="IN_PROGRESS">Em Andamento</option>
                <option value="RESOLVED">Resolvido</option>
                <option value="CLOSED">Fechado</option>
              </select>
            </div>
            <div className="relative">
              <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <select
                value={severityFilter}
                onChange={(e) => setSeverityFilter(e.target.value as typeof severityFilter)}
                className="input pl-10 pr-10 appearance-none"
              >
                <option value="ALL">Todas as severidades</option>
                <option value="CRITICAL">Crítico</option>
                <option value="HIGH">Alto</option>
                <option value="MEDIUM">Médio</option>
                <option value="LOW">Baixo</option>
              </select>
            </div>
            <div className="relative">
              <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <select
                value={pointFilter}
                onChange={(e) => setPointFilter(e.target.value)}
                className="input pl-10 pr-10 appearance-none"
              >
                <option value="">Todos os pontos</option>
                {points.map((point: CollectionPoint) => (
                  <option key={point.id} value={point.id}>{point.name}</option>
                ))}
              </select>
            </div>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="card-body p-0">
          {incidentsLoading ? (
            <div className="flex items-center justify-center h-64">
              <div className="spinner w-8 h-8"></div>
            </div>
          ) : (
            <div className="table-container">
              <table className="table">
                <thead>
                  <tr>
                    <th>Ponto de Coleta</th>
                    <th>Tipo</th>
                    <th>Severidade</th>
                    <th>Status</th>
                    <th>Reportado por</th>
                    <th>Data</th>
                    <th className="text-right">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredIncidents.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="text-center py-12 text-gray-500">
                        <AlertTriangle className="w-12 h-12 mx-auto mb-4 text-gray-300" />
                        <p className="text-lg">Nenhum incidente encontrado</p>
                        <p className="text-sm">Tente ajustar os filtros ou reporte um novo incidente</p>
                      </td>
                    </tr>
                  ) : (
                    filteredIncidents.map((incident: Incident) => (
                      <tr key={incident.id} className={incident.severity === 'CRITICAL' ? 'bg-red-50' : ''}>
                        <td>
                          <div>
                            <p className="font-medium text-gray-900">{incident.collectionPointName}</p>
                          </div>
                        </td>
                        <td className="text-sm text-gray-600">{incident.incidentType}</td>
                        <td>
                          <span className={`badge ${getSeverityColor(incident.severity)}`}>
                            {getSeverityLabel(incident.severity)}
                          </span>
                        </td>
                        <td>
                          <span className={`badge ${getStatusColor(incident.status)}`}>
                            {getStatusLabel(incident.status)}
                          </span>
                        </td>
                        <td className="text-sm text-gray-600">{incident.reporterName || '-'}</td>
                        <td className="text-sm text-gray-600">
                          {incident.createdAt ? format(new Date(incident.createdAt), 'dd/MM/yyyy HH:mm', { locale: ptBR }) : '-'}
                        </td>
                        <td className="text-right">
                          <div className="flex items-center justify-end space-x-2">
                            {incident.status === 'OPEN' && (
                              <button
                                onClick={() => handleResolve(incident)}
                                className="p-2 rounded-lg text-green-500 hover:text-green-700 hover:bg-green-50"
                                title="Resolver"
                              >
                                <CheckCircle className="w-5 h-5" />
                              </button>
                            )}
                            <button
                              onClick={() => handleEdit(incident)}
                              className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100"
                              title="Editar"
                            >
                              <Edit className="w-5 h-5" />
                            </button>
                            <button
                              onClick={() => handleDelete(incident.id)}
                              className="p-2 rounded-lg text-gray-500 hover:text-red-600 hover:bg-red-50"
                              title="Excluir"
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
                {editingIncident ? 'Editar Incidente' : 'Reportar Novo Incidente'}
              </h3>
              <button
                onClick={() => setIsModalOpen(false)}
                className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100"
              >
                <XCircle className="w-5 h-5" />
              </button>
            </div>
            <IncidentForm
              initialData={editingIncident}
              points={points}
              onSubmit={handleSubmit}
              onClose={() => {
                setIsModalOpen(false);
                setEditingIncident(null);
              }}
              isLoading={createMutation.isPending || updateMutation.isPending}
            />
          </div>
        </div>
      )}
    </div>
  );
}

function IncidentForm({
  initialData,
  points,
  onSubmit,
  onClose,
  isLoading,
}: {
  initialData: Incident | null;
  points: CollectionPoint[];
  onSubmit: (data: IncidentFormData) => void;
  onClose: () => void;
  isLoading: boolean;
}) {
  const { register, handleSubmit, setValue, formState: { errors } } = useForm<IncidentFormData>({
    resolver: zodResolver(incidentSchema),
    defaultValues: {
      collectionPointId: '',
      scheduleId: '',
      incidentType: '',
      description: '',
      severity: 'MEDIUM',
      photos: '',
      gpsLatitude: undefined,
      gpsLongitude: undefined,
    },
  });

  if (initialData) {
    setValue('collectionPointId', initialData.collectionPointId);
    setValue('scheduleId', initialData.scheduleId || '');
    setValue('incidentType', initialData.incidentType);
    setValue('description', initialData.description || '');
    setValue('severity', initialData.severity);
    setValue('photos', initialData.photos || '');
    setValue('gpsLatitude', initialData.gpsLatitude);
    setValue('gpsLongitude', initialData.gpsLongitude);
  }

  const activePoints = points.filter(p => p.status === 'ACTIVE');

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
          <label className="label">Agendamento (opcional)</label>
          <select {...register('scheduleId')} className="input">
            <option value="">Nenhum</option>
          </select>
        </div>

        <div>
          <label className="label">Tipo de Incidente *</label>
          <select {...register('incidentType')} className={`input ${errors.incidentType ? 'input-error' : ''}`}>
            <option value="">Selecione</option>
            {INCIDENT_TYPES.map((type) => (
              <option key={type} value={type}>{type}</option>
            ))}
          </select>
          {errors.incidentType && <p className="error-message">{errors.incidentType.message}</p>}
        </div>

        <div>
          <label className="label">Severidade *</label>
          <select {...register('severity')} className={`input ${errors.severity ? 'input-error' : ''}`}>
            <option value="LOW">Baixo</option>
            <option value="MEDIUM">Médio</option>
            <option value="HIGH">Alto</option>
            <option value="CRITICAL">Crítico</option>
          </select>
          {errors.severity && <p className="error-message">{errors.severity.message}</p>}
        </div>

        <div className="md:col-span-2">
          <label className="label">Descrição</label>
          <textarea {...register('description')} className="input" rows={3} placeholder="Descreva o incidente detalhadamente..." />
        </div>

        <div className="md:col-span-2">
          <label className="label">Fotos (URLs separadas por vírgula)</label>
          <input {...register('photos')} className="input" placeholder="https://exemplo.com/foto1.jpg, https://exemplo.com/foto2.jpg" />
        </div>

        <div className="md:col-span-2">
          <label className="label">Coordenadas GPS (opcional)</label>
          <div className="grid grid-cols-2 gap-4">
            <input
              type="number"
              step="any"
              {...register('gpsLatitude', { valueAsNumber: true })}
              className="input"
              placeholder="Latitude"
            />
            <input
              type="number"
              step="any"
              {...register('gpsLongitude', { valueAsNumber: true })}
              className="input"
              placeholder="Longitude"
            />
          </div>
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
            initialData ? 'Atualizar' : 'Reportar'
          )}
        </button>
      </div>
    </form>
  );
}