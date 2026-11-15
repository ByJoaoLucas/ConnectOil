'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import Link from 'next/link';
import { recordsApi, collectionPointsApi, usersApi } from '@/lib/api';
import { CollectionRecord, CollectionPoint, User } from '@/types';
import toast from 'react-hot-toast';
import {
  Plus,
  Search,
  Filter,
  Package,
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
  Eye,
} from 'lucide-react';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

interface RecordFormData {
  collectionPointId: string;
  collectorId: string;
  scheduleId?: string;
  collectionDate: string;
  volumeCollectedLiters: number;
  oilType?: string;
  containerCount?: number;
  containerType?: string;
  qualityRating?: number;
  temperatureCelsius?: number;
  phLevel?: number;
  contaminationPercentage?: number;
  photos?: string;
  notes?: string;
  gpsLatitude?: number;
  gpsLongitude?: number;
}

const recordSchema = z.object({
  collectionPointId: z.string().uuid('Selecione um ponto de coleta válido'),
  collectorId: z.string().uuid('Selecione um coletor válido'),
  scheduleId: z.string().uuid().optional().or(z.literal('')),
  collectionDate: z.string().min(1, 'Data da coleta é obrigatória'),
  volumeCollectedLiters: z.number().min(0.1, 'Volume deve ser maior que 0'),
  oilType: z.string().optional(),
  containerCount: z.number().min(1).optional(),
  containerType: z.string().optional(),
  qualityRating: z.number().min(1).max(5).optional(),
  temperatureCelsius: z.number().optional(),
  phLevel: z.number().min(0).max(14).optional(),
  contaminationPercentage: z.number().min(0).max(100).optional(),
  photos: z.string().optional(),
  notes: z.string().max(1000).optional(),
  gpsLatitude: z.number().min(-90).max(90).optional(),
  gpsLongitude: z.number().min(-180).max(180).optional(),
});

export default function RecordsPage() {
  const [search, setSearch] = useState('');
  const [pointFilter, setPointFilter] = useState('');
  const [collectorFilter, setCollectorFilter] = useState('');
  const [dateFromFilter, setDateFromFilter] = useState('');
  const [dateToFilter, setDateToFilter] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<CollectionRecord | null>(null);
  const queryClient = useQueryClient();

  const { data: recordsData, isLoading: recordsLoading } = useQuery({
    queryKey: ['records'],
    queryFn: () => recordsApi.getAllList(),
  });

  const { data: pointsData } = useQuery({
    queryKey: ['collectionPoints'],
    queryFn: () => collectionPointsApi.getAllList(),
  });

  const { data: collectorsData } = useQuery({
    queryKey: ['collectors'],
    queryFn: () => usersApi.getCollectors(),
  });

  const records = recordsData?.data || [];
  const points = pointsData?.data || [];
  const collectors = collectorsData?.data || [];

  const filteredRecords = records.filter((record) => {
    const matchesSearch =
      record.collectionPointName?.toLowerCase().includes(search.toLowerCase()) ||
      record.collectorName?.toLowerCase().includes(search.toLowerCase()) ||
      record.notes?.toLowerCase().includes(search.toLowerCase());
    const matchesPoint = !pointFilter || record.collectionPointId === pointFilter;
    const matchesCollector = !collectorFilter || record.collectorId === collectorFilter;
    const matchesDateFrom = !dateFromFilter || record.collectionDate >= dateFromFilter;
    const matchesDateTo = !dateToFilter || record.collectionDate <= dateToFilter + 'T23:59:59';
    return matchesSearch && matchesPoint && matchesCollector && matchesDateFrom && matchesDateTo;
  });

  const createMutation = useMutation({
    mutationFn: (data: RecordFormData) => recordsApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['records'] });
      queryClient.invalidateQueries({ queryKey: ['collectionPoints'] });
      toast.success('Registro de coleta criado com sucesso!');
      setIsModalOpen(false);
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao criar registro');
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: RecordFormData }) =>
      recordsApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['records'] });
      toast.success('Registro atualizado com sucesso!');
      setIsModalOpen(false);
      setEditingRecord(null);
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao atualizar registro');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => recordsApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['records'] });
      toast.success('Registro excluído com sucesso!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao excluir registro');
    },
  });

  const verifyMutation = useMutation({
    mutationFn: ({ id, managerId }: { id: string; managerId: string }) =>
      recordsApi.verify(id, managerId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['records'] });
      toast.success('Registro verificado com sucesso!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao verificar registro');
    },
  });

  const handleSubmit = (data: RecordFormData) => {
    const cleanData = { ...data };
    if (!cleanData.scheduleId) delete cleanData.scheduleId;
    if (editingRecord) {
      updateMutation.mutate({ id: editingRecord.id, data: cleanData });
    } else {
      createMutation.mutate(cleanData);
    }
  };

  const handleEdit = (record: CollectionRecord) => {
    setEditingRecord(record);
    setIsModalOpen(true);
  };

  const handleDelete = (id: string) => {
    if (confirm('Tem certeza que deseja excluir este registro?')) {
      deleteMutation.mutate(id);
    }
  };

  const handleVerify = (record: CollectionRecord) => {
    const { user } = JSON.parse(localStorage.getItem('user') || '{}');
    if (user?.id) {
      verifyMutation.mutate({ id: record.id, managerId: user.id });
    }
  };

  const openCreateModal = () => {
    setEditingRecord(null);
    setIsModalOpen(true);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="page-title">Registros de Coleta</h1>
          <p className="page-subtitle">Gerencie os registros de coleta de óleo</p>
        </div>
        <button onClick={openCreateModal} className="btn-primary">
          <Plus className="w-4 h-4 mr-2" />
          Novo Registro
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
                value={pointFilter}
                onChange={(e) => setPointFilter(e.target.value)}
                className="input pl-10 pr-10 appearance-none"
              >
                <option value="">Todos os pontos</option>
                {points.map((point) => (
                  <option key={point.id} value={point.id}>
                    {point.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="relative">
              <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <select
                value={collectorFilter}
                onChange={(e) => setCollectorFilter(e.target.value)}
                className="input pl-10 pr-10 appearance-none"
              >
                <option value="">Todos os coletores</option>
                {collectors.map((collector) => (
                  <option key={collector.id} value={collector.id}>
                    {collector.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="relative">
              <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type="date"
                value={dateFromFilter}
                onChange={(e) => setDateFromFilter(e.target.value)}
                className="input pl-10"
                placeholder="Data inicial"
              />
            </div>
            <div className="relative">
              <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type="date"
                value={dateToFilter}
                onChange={(e) => setDateToFilter(e.target.value)}
                className="input pl-10"
                placeholder="Data final"
              />
            </div>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="card-body p-0">
          {recordsLoading ? (
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
                    <th>Data/Hora</th>
                    <th>Volume (L)</th>
                    <th>Tipo de Óleo</th>
                    <th>Qualidade</th>
                    <th>Status</th>
                    <th>Verificado</th>
                    <th className="text-right">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredRecords.length === 0 ? (
                    <tr>
                      <td colSpan={9} className="text-center py-12 text-gray-500">
                        <Package className="w-12 h-12 mx-auto mb-4 text-gray-300" />
                        <p className="text-lg">Nenhum registro encontrado</p>
                        <p className="text-sm">Tente ajustar os filtros ou cadastre um novo registro</p>
                      </td>
                    </tr>
                  ) : (
                    filteredRecords.map((record) => (
                      <tr key={record.id}>
                        <td>
                          <div>
                            <p className="font-medium text-gray-900">{record.collectionPointName}</p>
                          </div>
                        </td>
                        <td className="text-sm text-gray-600">{record.collectorName}</td>
                        <td>{format(new Date(record.collectionDate), 'dd/MM/yyyy HH:mm', { locale: ptBR })}</td>
                        <td className="font-medium">{record.volumeCollectedLiters.toLocaleString('pt-BR')}</td>
                        <td className="capitalize text-sm text-gray-600">{record.oilType || '-'}</td>
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
                          <span className={`badge ${
                            record.status === 'VERIFIED' ? 'badge-success' :
                            record.status === 'REJECTED' ? 'badge-danger' : 'badge-warning'
                          }`}>
                            {record.status}
                          </span>
                        </td>
                        <td>
                          <span className={record.verifiedByManager ? 'badge-success' : 'badge-warning'}>
                            {record.verifiedByManager ? 'Sim' : 'Não'}
                          </span>
                        </td>
                        <td className="text-right">
                          <div className="flex items-center justify-end space-x-2">
                            <Link
                              href={`/records/${record.id}`}
                              className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100"
                              title="Ver detalhes"
                            >
                              <Eye className="w-5 h-5" />
                            </Link>
                            <button
                              onClick={() => handleEdit(record)}
                              className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100"
                              title="Editar"
                            >
                              <Edit className="w-5 h-5" />
                            </button>
                            {!record.verifiedByManager && (
                              <button
                                onClick={() => handleVerify(record)}
                                className="p-2 rounded-lg text-green-500 hover:text-green-700 hover:bg-green-50"
                                title="Verificar"
                              >
                                <CheckCircle className="w-5 h-5" />
                              </button>
                            )}
                            <button
                              onClick={() => handleDelete(record.id)}
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
          <div className="modal-content max-w-2xl" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-lg font-semibold text-gray-900">
                {editingRecord ? 'Editar Registro de Coleta' : 'Novo Registro de Coleta'}
              </h3>
              <button
                onClick={() => setIsModalOpen(false)}
                className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100"
              >
                <XCircle className="w-5 h-5" />
              </button>
            </div>
            <RecordForm
              initialData={editingRecord}
              points={points}
              collectors={collectors}
              onSubmit={handleSubmit}
              onClose={() => {
                setIsModalOpen(false);
                setEditingRecord(null);
              }}
              isLoading={createMutation.isPending || updateMutation.isPending}
            />
          </div>
        </div>
      )}
    </div>
  );
}

function RecordForm({
  initialData,
  points,
  collectors,
  onSubmit,
  onClose,
  isLoading,
}: {
  initialData: CollectionRecord | null;
  points: CollectionPoint[];
  collectors: User[];
  onSubmit: (data: RecordFormData) => void;
  onClose: () => void;
  isLoading: boolean;
}) {
  const { register, handleSubmit, setValue, formState: { errors } } = useForm<RecordFormData>({
    resolver: zodResolver(recordSchema),
    defaultValues: {
      collectionPointId: '',
      collectorId: '',
      scheduleId: '',
      collectionDate: new Date().toISOString().slice(0, 16),
      volumeCollectedLiters: 0,
      oilType: '',
      containerCount: undefined,
      containerType: '',
      qualityRating: undefined,
      temperatureCelsius: undefined,
      phLevel: undefined,
      contaminationPercentage: undefined,
      photos: '',
      notes: '',
      gpsLatitude: undefined,
      gpsLongitude: undefined,
    },
  });

  if (initialData) {
    setValue('collectionPointId', initialData.collectionPointId);
    setValue('collectorId', initialData.collectorId);
    setValue('scheduleId', initialData.scheduleId || '');
    setValue('collectionDate', initialData.collectionDate.slice(0, 16));
    setValue('volumeCollectedLiters', initialData.volumeCollectedLiters);
    setValue('oilType', initialData.oilType || '');
    setValue('containerCount', initialData.containerCount);
    setValue('containerType', initialData.containerType || '');
    setValue('qualityRating', initialData.qualityRating);
    setValue('temperatureCelsius', initialData.temperatureCelsius);
    setValue('phLevel', initialData.phLevel);
    setValue('contaminationPercentage', initialData.contaminationPercentage);
    setValue('photos', initialData.photos || '');
    setValue('notes', initialData.notes || '');
    setValue('gpsLatitude', initialData.gpsLatitude);
    setValue('gpsLongitude', initialData.gpsLongitude);
  }

  const activePoints = points.filter(p => p.status === 'ACTIVE');
  const activeCollectors = collectors.filter(c => c.isActive);

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="modal-body space-y-4 p-6 max-h-[80vh] overflow-y-auto">
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
          <label className="label">Agendamento (opcional)</label>
          <select {...register('scheduleId')} className="input">
            <option value="">Nenhum</option>
          </select>
        </div>

        <div>
          <label className="label">Data/Hora da Coleta *</label>
          <input
            type="datetime-local"
            {...register('collectionDate')}
            className={`input ${errors.collectionDate ? 'input-error' : ''}`}
          />
          {errors.collectionDate && <p className="error-message">{errors.collectionDate.message}</p>}
        </div>

        <div>
          <label className="label">Volume Coletado (L) *</label>
          <input
            type="number"
            step="0.1"
            min="0.1"
            {...register('volumeCollectedLiters', { valueAsNumber: true })}
            className={`input ${errors.volumeCollectedLiters ? 'input-error' : ''}`}
            placeholder="Ex: 500"
          />
          {errors.volumeCollectedLiters && <p className="error-message">{errors.volumeCollectedLiters.message}</p>}
        </div>

        <div>
          <label className="label">Tipo de Óleo</label>
          <select {...register('oilType')} className="input">
            <option value="">Selecione</option>
            <option value="vegetal">Vegetal</option>
            <option value="animal">Animal</option>
            <option value="industrial">Industrial</option>
            <option value="misto">Misto</option>
          </select>
        </div>

        <div>
          <label className="label">Quantidade de Recipientes</label>
          <input
            type="number"
            min="1"
            {...register('containerCount', { valueAsNumber: true })}
            className="input"
            placeholder="Ex: 5"
          />
        </div>

        <div>
          <label className="label">Tipo de Recipiente</label>
          <input {...register('containerType')} className="input" placeholder="Ex: Tambor 200L" />
        </div>

        <div>
          <label className="label">Avaliação de Qualidade (1-5)</label>
          <select {...register('qualityRating', { valueAsNumber: true })} className="input">
            <option value="">Selecione</option>
            <option value={1}>1 - Ruim</option>
            <option value={2}>2 - Regular</option>
            <option value={3}>3 - Bom</option>
            <option value={4}>4 - Muito Bom</option>
            <option value={5}>5 - Excelente</option>
          </select>
        </div>

        <div>
          <label className="label">Temperatura (°C)</label>
          <input
            type="number"
            step="0.1"
            {...register('temperatureCelsius', { valueAsNumber: true })}
            className="input"
            placeholder="Ex: 25.5"
          />
        </div>

        <div>
          <label className="label">pH</label>
          <input
            type="number"
            step="0.1"
            min="0"
            max="14"
            {...register('phLevel', { valueAsNumber: true })}
            className="input"
            placeholder="Ex: 7.0"
          />
        </div>

        <div>
          <label className="label">Contaminação (%)</label>
          <input
            type="number"
            min="0"
            max="100"
            {...register('contaminationPercentage', { valueAsNumber: true })}
            className="input"
            placeholder="Ex: 5"
          />
        </div>

        <div className="md:col-span-2">
          <label className="label">Fotos (URLs separadas por vírgula)</label>
          <input {...register('photos')} className="input" placeholder="https://exemplo.com/foto1.jpg, https://exemplo.com/foto2.jpg" />
        </div>

        <div className="md:col-span-2">
          <label className="label">Observações</label>
          <textarea {...register('notes')} className="input" rows={3} placeholder="Observações adicionais..." />
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
            initialData ? 'Atualizar' : 'Criar'
          )}
        </button>
      </div>
    </form>
  );
}