'use client';

import { useState } from 'react';
import NextImage from 'next/image';
import { useParams } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { recordsApi, collectionPointsApi, usersApi } from '@/lib/api';
import { CollectionRecord, CollectionPoint, User as UserType } from '@/types';
import toast from 'react-hot-toast';
import {
  Package,
  Calendar,
  Clock,
  Truck,
  MapPin,
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
  Star,
  Thermometer,
  Droplet,
  Image as ImageIcon,
  MapPin as MapPinIcon,
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

export default function RecordDetailPage() {
  const params = useParams();
  const router = useRouter();
  const queryClient = useQueryClient();
  const id = params?.id as string;
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);

  const { data: recordData, isLoading: recordLoading, error: recordError } = useQuery({
    queryKey: ['record', id],
    queryFn: () => recordsApi.getById(id),
    enabled: !!id,
  });

  const { data: pointsData } = useQuery({
    queryKey: ['collectionPoints'],
    queryFn: () => collectionPointsApi.getAllList(),
  });

  const { data: collectorsData } = useQuery({
    queryKey: ['collectors'],
    queryFn: () => usersApi.getCollectors(),
  });

  const record = recordData?.data;
  const points = pointsData?.data || [];
  const collectors = collectorsData?.data || [];

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: RecordFormData }) =>
      recordsApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['record', id] });
      queryClient.invalidateQueries({ queryKey: ['records'] });
      queryClient.invalidateQueries({ queryKey: ['collectionPoints'] });
      toast.success('Registro atualizado com sucesso!');
      setIsEditModalOpen(false);
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao atualizar registro');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => recordsApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['records'] });
      queryClient.invalidateQueries({ queryKey: ['collectionPoints'] });
      toast.success('Registro excluído com sucesso!');
      router.push('/records');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao excluir registro');
    },
  });

  const verifyMutation = useMutation({
    mutationFn: ({ id, managerId }: { id: string; managerId: string }) =>
      recordsApi.verify(id, managerId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['record', id] });
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
    updateMutation.mutate({ id, data: cleanData });
  };

  const handleDelete = () => {
    if (confirm('Tem certeza que deseja excluir este registro?')) {
      deleteMutation.mutate(id);
    }
  };

  const handleVerify = () => {
    const { user } = JSON.parse(localStorage.getItem('user') || '{}');
    if (user?.id) {
      verifyMutation.mutate({ id, managerId: user.id });
    }
  };

  if (recordLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="spinner w-8 h-8"></div>
      </div>
    );
  }

  if (recordError || !record) {
    return (
      <div className="card">
        <div className="card-body text-center py-12">
          <AlertCircle className="w-12 h-12 mx-auto mb-4 text-red-500" />
          <h3 className="text-lg font-semibold text-gray-900">Registro não encontrado</h3>
          <p className="text-gray-500 mt-2">O registro solicitado não existe ou foi removido.</p>
          <button onClick={() => router.push('/records')} className="btn-primary mt-4 inline-flex">
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
            <h1 className="page-title">Registro de Coleta</h1>
            <p className="page-subtitle">{record.collectionPointName} - {format(new Date(record.collectionDate), 'dd/MM/yyyy HH:mm', { locale: ptBR })}</p>
          </div>
        </div>
        <div className="flex items-center space-x-2">
          <span className={`badge ${
            record.status === 'VERIFIED' ? 'badge-success' :
            record.status === 'REJECTED' ? 'badge-danger' : 'badge-warning'
          }`}>
            {record.status}
          </span>
          {record.verifiedByManager && (
            <span className="badge badge-success">Verificado</span>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <div className="card">
            <div className="card-header flex items-center justify-between">
              <h3 className="text-lg font-semibold text-gray-900">Informações da Coleta</h3>
              <button
                onClick={() => setIsEditModalOpen(true)}
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
                    <span className="font-medium text-gray-900">{record.collectionPointName}</span>
                  </div>
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Coletor</label>
                  <div className="mt-1 flex items-center space-x-2">
                    <User className="w-5 h-5 text-gray-400" />
                    <span className="font-medium text-gray-900">{record.collectorName}</span>
                  </div>
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Data/Hora</label>
                  <div className="mt-1 flex items-center space-x-2">
                    <Calendar className="w-5 h-5 text-gray-400" />
                    <span className="font-medium text-gray-900">
                      {format(new Date(record.collectionDate), 'EEEE, dd/MM/yyyy HH:mm', { locale: ptBR })}
                    </span>
                  </div>
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Volume Coletado</label>
                  <div className="mt-1 flex items-center space-x-2">
                    <Package className="w-5 h-5 text-gray-400" />
                    <span className="font-medium text-gray-900 text-lg">
                      {record.volumeCollectedLiters.toLocaleString('pt-BR')} L
                    </span>
                  </div>
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Tipo de Óleo</label>
                  <div className="mt-1 flex items-center space-x-2">
                    <Droplet className="w-5 h-5 text-gray-400" />
                    <span className="font-medium text-gray-900 capitalize">{record.oilType || 'Não informado'}</span>
                  </div>
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Qualidade</label>
                  <div className="mt-1 flex items-center space-x-2">
                    <Star className="w-5 h-5 text-gray-400" />
                    {record.qualityRating ? (
                      <div className="flex items-center space-x-1">
                        {[1, 2, 3, 4, 5].map((star) => (
                          <span
                            key={star}
                            className={`text-xl ${
                              star <= (record.qualityRating || 0) ? 'text-yellow-400' : 'text-gray-300'
                            }`}
                          >
                            ★
                          </span>
                        ))}
                        <span className="text-sm text-gray-500 ml-1">({record.qualityRating}/5)</span>
                      </div>
                    ) : (
                      <span className="text-gray-500">Não avaliado</span>
                    )}
                  </div>
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Temperatura</label>
                  <div className="mt-1 flex items-center space-x-2">
                    <Thermometer className="w-5 h-5 text-gray-400" />
                    <span className="font-medium text-gray-900">
                      {record.temperatureCelsius !== undefined ? `${record.temperatureCelsius}°C` : 'Não informado'}
                    </span>
                  </div>
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">pH</label>
                  <div className="mt-1 flex items-center space-x-2">
                    <Droplet className="w-5 h-5 text-gray-400" />
                    <span className="font-medium text-gray-900">
                      {record.phLevel !== undefined ? record.phLevel.toFixed(1) : 'Não informado'}
                    </span>
                  </div>
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Contaminação</label>
                  <div className="mt-1 flex items-center space-x-2">
                    <AlertCircle className="w-5 h-5 text-gray-400" />
                    <span className="font-medium text-gray-900">
                      {record.contaminationPercentage !== undefined ? `${record.contaminationPercentage}%` : 'Não informado'}
                    </span>
                  </div>
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Recipientes</label>
                  <div className="mt-1 flex items-center space-x-2">
                    <Package className="w-5 h-5 text-gray-400" />
                    <span className="font-medium text-gray-900">
                      {record.containerCount ? `${record.containerCount} x ${record.containerType || 'unidade'}` : 'Não informado'}
                    </span>
                  </div>
                </div>
                <div className="md:col-span-2">
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Coordenadas GPS</label>
                  <div className="mt-1 flex items-center space-x-4">
                    {record.gpsLatitude !== undefined && record.gpsLongitude !== undefined ? (
                      <>
                        <div className="flex items-center space-x-2">
                          <MapPinIcon className="w-5 h-5 text-gray-400" />
                          <span className="font-mono text-sm text-gray-900">
                            {record.gpsLatitude.toFixed(6)}, {record.gpsLongitude.toFixed(6)}
                          </span>
                        </div>
                        <a
                          href={`https://www.google.com/maps/search/?api=1&query=${record.gpsLatitude},${record.gpsLongitude}`}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-sm text-primary-600 hover:underline flex items-center space-x-1"
                        >
                          <MapPinIcon className="w-4 h-4" />
                          <span>Ver no mapa</span>
                        </a>
                      </>
                    ) : (
                      <span className="text-gray-500">Não informado</span>
                    )}
                  </div>
                </div>
              </div>

              {record.photos && (
                <div className="mt-6 pt-6 border-t border-gray-200">
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Fotos</label>
                  <div className="mt-3 flex flex-wrap gap-3">
                    {record.photos.split(',').map((photo: string, index: number) => (
                      <a
                        key={index}
                        href={photo.trim()}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="relative block w-24 h-24 rounded-lg overflow-hidden border border-gray-200"
                      >
                        <NextImage
                          src={photo.trim()}
                          alt={`Foto ${index + 1}`}
                          fill
                          unoptimized
                          className="object-cover"
                        />
                        <div className="absolute inset-0 bg-black/50 opacity-0 hover:opacity-100 transition-opacity flex items-center justify-center">
                          <ImageIcon className="w-6 h-6 text-white" />
                        </div>
                      </a>
                    ))}
                  </div>
                </div>
              )}

              {record.notes && (
                <div className="mt-6 pt-6 border-t border-gray-200">
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Observações</label>
                  <p className="mt-1 text-sm text-gray-900">{record.notes}</p>
                </div>
              )}

              <div className="mt-6 pt-6 border-t border-gray-200">
                <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Datas Importantes</label>
                <div className="mt-2 grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <p className="text-gray-500">Criado em</p>
                    <p className="font-medium text-gray-900">{record.createdAt ? format(new Date(record.createdAt), 'dd/MM/yyyy HH:mm', { locale: ptBR }) : '-'}</p>
                  </div>
                  <div>
                    <p className="text-gray-500">Atualizado em</p>
                    <p className="font-medium text-gray-900">{record.updatedAt ? format(new Date(record.updatedAt), 'dd/MM/yyyy HH:mm', { locale: ptBR }) : '-'}</p>
                  </div>
                  {record.verifiedAt && (
                    <div>
                      <p className="text-gray-500">Verificado em</p>
                      <p className="font-medium text-gray-900">{format(new Date(record.verifiedAt), 'dd/MM/yyyy HH:mm', { locale: ptBR })}</p>
                    </div>
                  )}
                  {record.verifiedByName && (
                    <div>
                      <p className="text-gray-500">Verificado por</p>
                      <p className="font-medium text-gray-900">{record.verifiedByName}</p>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="space-y-6">
          <div className="card">
            <div className="card-header">
              <h3 className="text-lg font-semibold text-gray-900">Ações</h3>
            </div>
            <div className="card-body space-y-3">
              {!record.verifiedByManager && (
                <button
                  onClick={handleVerify}
                  disabled={verifyMutation.isPending}
                  className="btn-success w-full justify-center"
                >
                  <CheckCircle className="w-4 h-4 mr-2" />
                  {verifyMutation.isPending ? 'Verificando...' : 'Verificar Registro'}
                </button>
              )}
              {record.verifiedByManager && (
                <div className="p-3 bg-green-50 border border-green-200 rounded-lg text-center">
                  <CheckCircle className="w-5 h-5 text-green-600 mx-auto mb-1" />
                  <p className="text-sm text-green-800">Registro verificado por gerente</p>
                </div>
              )}
              <button
                onClick={handleDelete}
                disabled={deleteMutation.isPending}
                className="btn-outline w-full justify-center text-red-600 border-red-300 hover:bg-red-50"
              >
                <Trash2 className="w-4 h-4 mr-2" />
                {deleteMutation.isPending ? 'Excluindo...' : 'Excluir Registro'}
              </button>
            </div>
          </div>

          <div className="card">
            <div className="card-header">
              <h3 className="text-lg font-semibold text-gray-900">Indicadores de Qualidade</h3>
            </div>
            <div className="card-body space-y-4">
              <QualityIndicator
                label="Qualidade"
                value={record.qualityRating ? `${record.qualityRating}/5` : 'N/A'}
                icon={<Star className="w-5 h-5" />}
                color="text-yellow-600"
                threshold={3}
                current={record.qualityRating || 0}
              />
              <QualityIndicator
                label="Temperatura"
                value={record.temperatureCelsius !== undefined ? `${record.temperatureCelsius}°C` : 'N/A'}
                icon={<Thermometer className="w-5 h-5" />}
                color="text-orange-600"
                threshold={30}
                current={record.temperatureCelsius || 0}
              />
              <QualityIndicator
                label="pH"
                value={record.phLevel !== undefined ? record.phLevel.toFixed(1) : 'N/A'}
                icon={<Droplet className="w-5 h-5" />}
                color="text-blue-600"
                threshold={7}
                current={record.phLevel || 0}
                isPh={true}
              />
              <QualityIndicator
                label="Contaminação"
                value={record.contaminationPercentage !== undefined ? `${record.contaminationPercentage}%` : 'N/A'}
                icon={<AlertCircle className="w-5 h-5" />}
                color="text-red-600"
                threshold={10}
                current={record.contaminationPercentage || 0}
                inverse={true}
              />
            </div>
          </div>
        </div>
      </div>

      {isEditModalOpen && (
        <div className="modal-overlay" onClick={() => setIsEditModalOpen(false)}>
          <div className="modal-content max-w-2xl" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-lg font-semibold text-gray-900">Editar Registro de Coleta</h3>
              <button
                onClick={() => setIsEditModalOpen(false)}
                className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100"
              >
                <XCircle className="w-5 h-5" />
              </button>
            </div>
            <RecordEditForm
              record={record}
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

function RecordEditForm({
  record,
  points,
  collectors,
  onSubmit,
  onClose,
  isLoading,
}: {
  record: CollectionRecord;
  points: CollectionPoint[];
  collectors: UserType[];
  onSubmit: (data: RecordFormData) => void;
  onClose: () => void;
  isLoading: boolean;
}) {
  const { register, handleSubmit, setValue, formState: { errors } } = useForm<RecordFormData>({
    resolver: zodResolver(recordSchema),
    defaultValues: {
      collectionPointId: record.collectionPointId,
      collectorId: record.collectorId,
      scheduleId: record.scheduleId || '',
      collectionDate: record.collectionDate.slice(0, 16),
      volumeCollectedLiters: record.volumeCollectedLiters,
      oilType: record.oilType || '',
      containerCount: record.containerCount,
      containerType: record.containerType || '',
      qualityRating: record.qualityRating,
      temperatureCelsius: record.temperatureCelsius,
      phLevel: record.phLevel,
      contaminationPercentage: record.contaminationPercentage,
      photos: record.photos || '',
      notes: record.notes || '',
      gpsLatitude: record.gpsLatitude,
      gpsLongitude: record.gpsLongitude,
    },
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="modal-body space-y-4 p-6 max-h-[80vh] overflow-y-auto">
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
            'Atualizar'
          )}
        </button>
      </div>
    </form>
  );
}

function QualityIndicator({
  label,
  value,
  icon,
  color,
  threshold,
  current,
  isPh = false,
  inverse = false,
}: {
  label: string;
  value: string;
  icon: React.ReactNode;
  color: string;
  threshold: number;
  current: number;
  isPh?: boolean;
  inverse?: boolean;
}) {
  let isGood = false;
  if (isPh) {
    isGood = current >= 6.5 && current <= 7.5;
  } else if (inverse) {
    isGood = current <= threshold;
  } else {
    isGood = current >= threshold;
  }

  return (
    <div className="flex items-center space-x-3 p-3 bg-gray-50 rounded-lg">
      <div className={`p-2 rounded-lg bg-white ${color}`}>{icon}</div>
      <div className="flex-1 min-w-0">
        <p className="text-xs font-medium text-gray-500 truncate">{label}</p>
        <p className="text-sm font-semibold text-gray-900 truncate">{value}</p>
      </div>
      <span className={`badge ${isGood ? 'badge-success' : 'badge-danger'}`}>
        {isGood ? 'OK' : 'Atenção'}
      </span>
    </div>
  );
}