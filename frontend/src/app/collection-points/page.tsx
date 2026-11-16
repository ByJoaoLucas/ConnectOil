'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import Link from 'next/link';
import { collectionPointsApi } from '@/lib/api';
import { CollectionPoint } from '@/types';
import toast from 'react-hot-toast';
import {
  Plus,
  Search,
  Filter,
  MapPin,
  Truck,
  AlertCircle,
  CheckCircle,
  XCircle,
  Edit,
  Trash2,
  MoreVertical,
} from 'lucide-react';
import { STATUS_LABELS, STATUS_COLORS, PointStatus } from '@/types';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

interface CollectionPointFormData {
  name: string;
  description?: string;
  address: string;
  neighborhood?: string;
  city: string;
  state: string;
  zipCode?: string;
  latitude?: number;
  longitude?: number;
  capacityLiters?: number;
  contactName?: string;
  contactPhone?: string;
  contactEmail?: string;
  operatingHours?: string;
  acceptedOilTypes?: string;
}

const pointSchema = z.object({
  name: z.string().min(2, 'Nome deve ter pelo menos 2 caracteres').max(150),
  description: z.string().max(1000).optional(),
  address: z.string().min(5, 'Endereço é obrigatório').max(255),
  neighborhood: z.string().max(100).optional(),
  city: z.string().min(2, 'Cidade é obrigatória').max(100),
  state: z.string().length(2, 'Estado deve ter 2 caracteres'),
  zipCode: z.string().regex(/^\d{5}-?\d{3}$/, 'CEP inválido').optional().or(z.literal('')),
  latitude: z.number().min(-90).max(90).optional(),
  longitude: z.number().min(-180).max(180).optional(),
  capacityLiters: z.number().min(1).max(100000).optional(),
  contactName: z.string().max(100).optional(),
  contactPhone: z.string().optional(),
  contactEmail: z.string().email('E-mail inválido').optional().or(z.literal('')),
  operatingHours: z.string().optional(),
  acceptedOilTypes: z.string().optional(),
});

export default function CollectionPointsPage() {
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<PointStatus | 'ALL'>('ALL');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingPoint, setEditingPoint] = useState<CollectionPoint | null>(null);
  const queryClient = useQueryClient();

  const { data: pointsData, isLoading } = useQuery({
    queryKey: ['collectionPoints'],
    queryFn: () => collectionPointsApi.getAllList(),
  });

  const points: CollectionPoint[] = pointsData?.data || [];

  const filteredPoints = points.filter((point) => {
    const matchesSearch =
      point.name.toLowerCase().includes(search.toLowerCase()) ||
      point.city.toLowerCase().includes(search.toLowerCase()) ||
      point.address.toLowerCase().includes(search.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || point.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const createMutation = useMutation({
    mutationFn: (data: CollectionPointFormData) => collectionPointsApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['collectionPoints'] });
      toast.success('Ponto de coleta criado com sucesso!');
      setIsModalOpen(false);
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao criar ponto de coleta');
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: CollectionPointFormData }) =>
      collectionPointsApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['collectionPoints'] });
      toast.success('Ponto de coleta atualizado com sucesso!');
      setIsModalOpen(false);
      setEditingPoint(null);
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao atualizar ponto de coleta');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => collectionPointsApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['collectionPoints'] });
      toast.success('Ponto de coleta excluído com sucesso!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao excluir ponto de coleta');
    },
  });

  const handleSubmit = (data: CollectionPointFormData) => {
    if (editingPoint) {
      updateMutation.mutate({ id: editingPoint.id, data });
    } else {
      createMutation.mutate(data);
    }
  };

  const handleEdit = (point: CollectionPoint) => {
    setEditingPoint(point);
    setIsModalOpen(true);
  };

  const handleDelete = (id: string) => {
    if (confirm('Tem certeza que deseja excluir este ponto de coleta?')) {
      deleteMutation.mutate(id);
    }
  };

  const openCreateModal = () => {
    setEditingPoint(null);
    setIsModalOpen(true);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="page-title">Pontos de Coleta</h1>
          <p className="page-subtitle">Gerencie os pontos de coleta de óleo</p>
        </div>
        <button onClick={openCreateModal} className="btn-primary">
          <Plus className="w-4 h-4 mr-2" />
          Novo Ponto
        </button>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="flex flex-col sm:flex-row gap-4">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type="text"
                placeholder="Buscar por nome, cidade, endereço..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="input pl-10"
              />
            </div>
            <div className="relative">
              <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value as PointStatus | 'ALL')}
                className="input pl-10 pr-10 appearance-none"
              >
                <option value="ALL">Todos os status</option>
                <option value="ACTIVE">Ativo</option>
                <option value="INACTIVE">Inativo</option>
                <option value="MAINTENANCE">Manutenção</option>
                <option value="FULL">Cheio</option>
                <option value="PENDING_APPROVAL">Pendente Aprovação</option>
              </select>
            </div>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="card-body p-0">
          {isLoading ? (
            <div className="flex items-center justify-center h-64">
              <div className="spinner w-8 h-8"></div>
            </div>
          ) : (
            <div className="table-container">
              <table className="table">
                <thead>
                  <tr>
                    <th>Nome</th>
                    <th>Endereço</th>
                    <th>Cidade/Estado</th>
                    <th>Status</th>
                    <th>Capacidade</th>
                    <th>Volume Atual</th>
                    <th>Coletor</th>
                    <th className="text-right">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredPoints.length === 0 ? (
                    <tr>
                      <td colSpan={8} className="text-center py-12 text-gray-500">
                        <MapPin className="w-12 h-12 mx-auto mb-4 text-gray-300" />
                        <p className="text-lg">Nenhum ponto de coleta encontrado</p>
                        <p className="text-sm">Tente ajustar os filtros ou cadastre um novo ponto</p>
                      </td>
                    </tr>
                  ) : (
                    filteredPoints.map((point) => (
                      <tr key={point.id}>
                        <td>
                          <div>
                            <p className="font-medium text-gray-900">{point.name}</p>
                            {point.description && (
                              <p className="text-sm text-gray-500 truncate max-w-xs">{point.description}</p>
                            )}
                          </div>
                        </td>
                        <td className="text-sm text-gray-600">{point.address}</td>
                        <td className="text-sm text-gray-600">{point.city}/{point.state}</td>
                        <td>
                          <span className={`badge ${STATUS_COLORS[point.status as PointStatus]}`}>
                            {STATUS_LABELS[point.status as PointStatus]}
                          </span>
                        </td>
                        <td className="text-sm text-gray-600">
                          {point.capacityLiters ? `${point.capacityLiters.toLocaleString('pt-BR')} L` : '-'}
                        </td>
                        <td>
                          <div className="flex items-center space-x-2">
                            <span className="text-sm font-medium text-gray-900">
                              {point.currentVolumeLiters.toLocaleString('pt-BR')} L
                            </span>
                            <div className="w-24 h-2 bg-gray-200 rounded-full overflow-hidden">
                              <div
                                className={`h-full rounded-full transition-all ${
                                  point.fillPercentage >= 90
                                    ? 'bg-red-500'
                                    : point.fillPercentage >= 70
                                    ? 'bg-yellow-500'
                                    : 'bg-green-500'
                                }`}
                                style={{ width: `${Math.min(point.fillPercentage, 100)}%` }}
                              />
                            </div>
                            <span className="text-xs text-gray-500">{point.fillPercentage.toFixed(0)}%</span>
                          </div>
                        </td>
                        <td className="text-sm text-gray-600">{point.collectorName || '-'}</td>
                        <td className="text-right">
                          <div className="flex items-center justify-end space-x-2">
                            <Link
                              href={`/collection-points/${point.id}`}
                              className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100"
                              title="Ver detalhes"
                            >
                              <MapPin className="w-5 h-5" />
                            </Link>
                            <button
                              onClick={() => handleEdit(point)}
                              className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100"
                              title="Editar"
                            >
                              <Edit className="w-5 h-5" />
                            </button>
                            <button
                              onClick={() => handleDelete(point.id)}
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
                {editingPoint ? 'Editar Ponto de Coleta' : 'Novo Ponto de Coleta'}
              </h3>
              <button
                onClick={() => setIsModalOpen(false)}
                className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100"
              >
                <XCircle className="w-5 h-5" />
              </button>
            </div>
            <CollectionPointForm
              initialData={editingPoint}
              onSubmit={handleSubmit}
              onClose={() => {
                setIsModalOpen(false);
                setEditingPoint(null);
              }}
              isLoading={createMutation.isPending || updateMutation.isPending}
            />
          </div>
        </div>
      )}
    </div>
  );
}

function CollectionPointForm({
  initialData,
  onSubmit,
  onClose,
  isLoading,
}: {
  initialData: CollectionPoint | null;
  onSubmit: (data: CollectionPointFormData) => void;
  onClose: () => void;
  isLoading: boolean;
}) {
  const { register, handleSubmit, setValue, formState: { errors } } = useForm<CollectionPointFormData>({
    resolver: zodResolver(pointSchema),
    defaultValues: {
      name: '',
      description: '',
      address: '',
      neighborhood: '',
      city: '',
      state: '',
      zipCode: '',
      capacityLiters: undefined,
      contactName: '',
      contactPhone: '',
      contactEmail: '',
      operatingHours: '',
      acceptedOilTypes: '',
    },
  });

  if (initialData) {
    Object.keys(initialData).forEach((key) => {
      if (key in initialData && key !== 'id' && key !== 'collectorId' && key !== 'collectorName' && key !== 'fillPercentage' && key !== 'createdAt' && key !== 'updatedAt' && key !== 'status' && key !== 'currentVolumeLiters') {
        setValue(key as keyof CollectionPointFormData, (initialData as any)[key] || '');
      }
    });
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="modal-body space-y-4 p-6 max-h-[70vh] overflow-y-auto">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="md:col-span-2">
          <label className="label">Nome *</label>
          <input {...register('name')} className={`input ${errors.name ? 'input-error' : ''}`} placeholder="Nome do ponto de coleta" />
          {errors.name && <p className="error-message">{errors.name.message}</p>}
        </div>

        <div className="md:col-span-2">
          <label className="label">Descrição</label>
          <textarea {...register('description')} className="input" rows={3} placeholder="Descrição do ponto" />
        </div>

        <div className="md:col-span-2">
          <label className="label">Endereço *</label>
          <input {...register('address')} className={`input ${errors.address ? 'input-error' : ''}`} placeholder="Rua, número, complemento" />
          {errors.address && <p className="error-message">{errors.address.message}</p>}
        </div>

        <div>
          <label className="label">Bairro</label>
          <input {...register('neighborhood')} className="input" placeholder="Bairro" />
        </div>

        <div>
          <label className="label">Cidade *</label>
          <input {...register('city')} className={`input ${errors.city ? 'input-error' : ''}`} placeholder="Cidade" />
          {errors.city && <p className="error-message">{errors.city.message}</p>}
        </div>

        <div>
          <label className="label">Estado *</label>
          <input {...register('state')} className={`input ${errors.state ? 'input-error' : ''}`} placeholder="SP" maxLength={2} />
          {errors.state && <p className="error-message">{errors.state.message}</p>}
        </div>

        <div>
          <label className="label">CEP</label>
          <input {...register('zipCode')} className="input" placeholder="00000-000" />
        </div>

        <div>
          <label className="label">Latitude</label>
          <input {...register('latitude', { valueAsNumber: true })} type="number" step="any" className="input" placeholder="-23.5505" />
        </div>

        <div>
          <label className="label">Longitude</label>
          <input {...register('longitude', { valueAsNumber: true })} type="number" step="any" className="input" placeholder="-46.6333" />
        </div>

        <div>
          <label className="label">Capacidade (Litros)</label>
          <input {...register('capacityLiters', { valueAsNumber: true })} type="number" min="1" className="input" placeholder="1000" />
        </div>

        <div>
          <label className="label">Nome do Contato</label>
          <input {...register('contactName')} className="input" placeholder="Nome do responsável" />
        </div>

        <div>
          <label className="label">Telefone do Contato</label>
          <input {...register('contactPhone')} className="input" placeholder="(11) 99999-9999" />
        </div>

        <div>
          <label className="label">E-mail do Contato</label>
          <input {...register('contactEmail')} type="email" className="input" placeholder="contato@email.com" />
        </div>

        <div className="md:col-span-2">
          <label className="label">Horário de Funcionamento (JSON)</label>
          <textarea {...register('operatingHours')} className="input font-mono text-sm" rows={3} placeholder='{"segunda": "08:00-18:00", "terca": "08:00-18:00"}' />
        </div>

        <div className="md:col-span-2">
          <label className="label">Tipos de Óleo Aceitos (JSON)</label>
          <textarea {...register('acceptedOilTypes')} className="input font-mono text-sm" rows={3} placeholder='["vegetal", "animal", "industrial"]' />
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