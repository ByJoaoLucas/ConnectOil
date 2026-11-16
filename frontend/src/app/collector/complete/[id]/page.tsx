'use client';

import { useState, useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { schedulesApi, recordsApi, collectionPointsApi, usersApi } from '@/lib/api';
import { CollectionSchedule, CollectionRecord, CollectionPoint, User, SCHEDULE_STATUS_LABELS, SCHEDULE_STATUS_COLORS } from '@/types';
import { useAuth } from '@/contexts/AuthContext';
import toast from 'react-hot-toast';
import {
  Package,
  Calendar,
  Clock,
  Truck,
  MapPin,
  AlertTriangle,
  CheckCircle,
  XCircle,
  ArrowLeft,
  AlertCircle,
  Save,
  ChevronDown,
  ChevronUp,
  Droplets,
  Weight,
  Shield,
  Navigation,
  Phone,
} from 'lucide-react';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

interface CompletionFormData {
  volumeCollectedLiters: number;
  oilType: string;
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
  confirmationCode?: string;
  establishmentConfirmed: boolean;
}

const completionSchema = z.object({
  volumeCollectedLiters: z.number().min(0.1, 'Volume deve ser maior que 0'),
  oilType: z.string().min(1, 'Tipo de óleo é obrigatório'),
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
  confirmationCode: z.string().optional(),
  establishmentConfirmed: z.boolean().default(false),
});

const OIL_TYPES = [
  { value: 'vegetal', label: 'Vegetal' },
  { value: 'animal', label: 'Animal' },
  { value: 'industrial', label: 'Industrial' },
  { value: 'misto', label: 'Misto' },
];

export default function CollectionCompletePage() {
  const params = useParams();
  const router = useRouter();
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const id = params?.id as string;

  if (!id) {
    return null;
  }
  const [showDiscrepancyWarning, setShowDiscrepancyWarning] = useState(false);
  const [discrepancyDetails, setDiscrepancyDetails] = useState('');

  const { data: scheduleData, isLoading: scheduleLoading, error: scheduleError } = useQuery({
    queryKey: ['schedule', id],
    queryFn: () => schedulesApi.getById(id),
    enabled: !!id,
  });

  const { data: pointData } = useQuery({
    queryKey: ['collectionPoint', scheduleData?.data?.collectionPointId],
    queryFn: () => collectionPointsApi.getById(scheduleData?.data?.collectionPointId || ''),
    enabled: !!scheduleData?.data?.collectionPointId,
  });

  const schedule = scheduleData?.data;
  const point = pointData?.data;

  // Generate confirmation code
  const generateConfirmationCode = () => {
    const timestamp = Date.now().toString(36).toUpperCase();
    const random = Math.random().toString(36).substring(2, 6).toUpperCase();
    return `CC-${timestamp}-${random}`;
  };

  const createMutation = useMutation({
    mutationFn: (data: CompletionFormData) => recordsApi.create(data),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ['records'] });
      queryClient.invalidateQueries({ queryKey: ['collectorSchedules'] });
      queryClient.invalidateQueries({ queryKey: ['schedule', id] });
      queryClient.invalidateQueries({ queryKey: ['collectionPoints'] });
      toast.success('Coleta registrada com sucesso!');
      router.push(`/schedules/${id}`);
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao registrar coleta');
    },
  });

  // Check for discrepancy
  const checkDiscrepancy = (actualVolume: number) => {
    if (!schedule || !schedule.estimatedVolumeLiters) return;
    const estimated = schedule.estimatedVolumeLiters;
    const diffPercent = Math.abs((actualVolume - estimated) / estimated) * 100;
    if (diffPercent > 20) {
      setShowDiscrepancyWarning(true);
      setDiscrepancyDetails(
        `Atenção: O volume coletado (${actualVolume.toLocaleString('pt-BR')} L) difere em ${diffPercent.toFixed(1)}% ` +
        `do volume estimado (${estimated.toLocaleString('pt-BR')} L). ` +
        `Isso pode indicar um problema na medição ou no registro.`
      );
    }
  };

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<CompletionFormData>({
    resolver: zodResolver(completionSchema),
    defaultValues: {
      volumeCollectedLiters: 0,
      oilType: 'vegetal',
      containerCount: undefined,
      containerType: '',
      qualityRating: undefined,
      temperatureCelsius: undefined,
      phLevel: undefined,
      contaminationPercentage: undefined,
      photos: '',
      notes: '',
      gpsLatitude: point?.latitude,
      gpsLongitude: point?.longitude,
      confirmationCode: generateConfirmationCode(),
      establishmentConfirmed: false,
    },
  });

  const handleVolumeChange = (value: number) => {
    setValue('volumeCollectedLiters', value, { shouldValidate: true });
    checkDiscrepancy(value);
  }

  const volume = watch('volumeCollectedLiters');

  // Auto-check discrepancy when volume changes
  useEffect(() => {
    if (volume > 0) {
      checkDiscrepancy(volume);
    }
  }, [volume]);

  const onSubmit = async (data: CompletionFormData) => {
    if (!schedule) return;

    const recordData = {
      ...data,
      collectionPointId: schedule.collectionPointId,
      collectorId: user?.id || '',
      scheduleId: schedule.id,
      collectionDate: new Date().toISOString(),
      volumeCollectedLiters: data.volumeCollectedLiters,
      oilType: data.oilType,
      containerCount: data.containerCount,
      containerType: data.containerType,
      qualityRating: data.qualityRating,
      temperatureCelsius: data.temperatureCelsius,
      phLevel: data.phLevel,
      contaminationPercentage: data.contaminationPercentage,
      photos: data.photos,
      notes: data.notes,
      gpsLatitude: data.gpsLatitude,
      gpsLongitude: data.gpsLongitude,
      // US05, US14 - Confirmation and certificate fields
      confirmationCode: data.confirmationCode,
      establishmentConfirmed: data.establishmentConfirmed,
      establishmentConfirmedAt: data.establishmentConfirmed ? new Date().toISOString() : undefined,
      establishmentConfirmedById: data.establishmentConfirmed ? user?.id : undefined,
      discrepancyFlag: showDiscrepancyWarning,
      discrepancyDetails: showDiscrepancyWarning ? discrepancyDetails : undefined,
    };

    createMutation.mutate(recordData);
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
          <button onClick={() => router.push('/collector')} className="btn-primary mt-4 inline-flex">
            <ArrowLeft className="w-4 h-4 mr-2" />
            Voltar para o painel
          </button>
        </div>
      </div>
    );
  }

  if (schedule.status !== 'IN_PROGRESS' && schedule.status !== 'CONFIRMED') {
    return (
      <div className="card">
        <div className="card-body text-center py-12">
          <AlertCircle className="w-12 h-12 mx-auto mb-4 text-yellow-500" />
          <h3 className="text-lg font-semibold text-gray-900">Agendamento não pode ser finalizado</h3>
          <p className="text-gray-500 mt-2">
            Este agendamento está com status <strong>{SCHEDULE_STATUS_LABELS[schedule.status as keyof typeof SCHEDULE_STATUS_LABELS]}</strong>.
            Apenas agendamentos "Confirmado" ou "Em Andamento" podem ser finalizados.
          </p>
          <button onClick={() => router.push(`/schedules/${id}`)} className="btn-primary mt-4 inline-flex">
            <ArrowLeft className="w-4 h-4 mr-2" />
            Voltar para detalhes
          </button>
        </div>
      </div>
    );
  }

  const discrepancyPercent = schedule.estimatedVolumeLiters && volume > 0
    ? Math.abs((volume - schedule.estimatedVolumeLiters) / schedule.estimatedVolumeLiters) * 100
    : 0;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <button onClick={() => router.back()} className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100">
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="page-title">Finalizar Coleta</h1>
            <p className="page-subtitle">{schedule.collectionPointName} - {format(new Date(), 'dd/MM/yyyy HH:mm', { locale: ptBR })}</p>
          </div>
        </div>
        <span className={`badge ${SCHEDULE_STATUS_COLORS[schedule.status as keyof typeof SCHEDULE_STATUS_COLORS]}`}>
          {SCHEDULE_STATUS_LABELS[schedule.status as keyof typeof SCHEDULE_STATUS_LABELS]}
        </span>
      </div>

      {/* Discrepancy Warning Modal */}
      {showDiscrepancyWarning && (
        <div className="modal-overlay" onClick={() => setShowDiscrepancyWarning(false)}>
          <div className="modal-content max-w-md" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header border-red-200">
              <h3 className="text-lg font-semibold text-gray-900 flex items-center space-x-2">
                <AlertTriangle className="w-5 h-5 text-yellow-600" />
                <span>Discrepância Detectada</span>
              </h3>
            </div>
            <div className="modal-body p-6">
              <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-4">
                <p className="text-sm text-yellow-800">{discrepancyDetails}</p>
              </div>
              <div className="space-y-2">
                <label className="label">Justificativa da discrepância *</label>
                <textarea
                  value={discrepancyDetails}
                  onChange={(e) => setDiscrepancyDetails(e.target.value)}
                  className="input"
                  rows={3}
                  placeholder="Explique o motivo da diferença (ex: medição imprecisa, óleo já parcialmente coletado, etc.)"
                />
              </div>
              <div className="modal-footer mt-4">
                <button
                  onClick={() => setShowDiscrepancyWarning(false)}
                  className="btn-primary"
                >
                  Confirmar e Continuar
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Form */}
        <div className="lg:col-span-2 space-y-6">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            {/* Volume & Basic Info */}
            <div className="card">
              <div className="card-header">
                <h3 className="text-lg font-semibold text-gray-900 flex items-center space-x-2">
                  <Package className="w-5 h-5 text-primary-600" />
                  <span>Informações da Coleta</span>
                </h3>
              </div>
              <div className="card-body space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="label">Volume Coletado (L) *</label>
                    <div className="relative">
                      <input
                        type="number"
                        step="0.1"
                        min="0.1"
                        {...register('volumeCollectedLiters', { valueAsNumber: true })}
                        className={`input ${errors.volumeCollectedLiters ? 'input-error' : ''} text-2xl font-bold text-center`}
                        placeholder="0.0"
                      />
                      <span className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 text-lg">L</span>
                    </div>
                    {errors.volumeCollectedLiters && <p className="error-message">{errors.volumeCollectedLiters.message}</p>}

                    {schedule.estimatedVolumeLiters && (
                      <div className="mt-3 p-3 bg-gray-50 rounded-lg">
                        <p className="text-xs text-gray-500">Volume Estimado: {schedule.estimatedVolumeLiters.toLocaleString('pt-BR')} L</p>
                        <div className="w-full h-2 bg-gray-200 rounded-full overflow-hidden mt-2">
                          <div
                            className={`h-full rounded-full transition-all ${
                              discrepancyPercent > 20 ? 'bg-red-500' : discrepancyPercent > 10 ? 'bg-yellow-500' : 'bg-green-500'
                            }`}
                            style={{ width: `${Math.min((volume / schedule.estimatedVolumeLiters) * 100, 100)}%` }}
                          />
                        </div>
                        <p className="text-xs text-gray-500 mt-1 text-right">
                          {((volume / schedule.estimatedVolumeLiters) * 100).toFixed(1)}% do estimado
                          {discrepancyPercent > 20 && <span className="text-red-600 ml-2">{'⚠ Discrepância > 20%'}</span>}
                        </p>
                      </div>
                    )}
                  </div>

                  <div>
                    <label className="label">Tipo de Óleo *</label>
                    <select {...register('oilType')} className={`input ${errors.oilType ? 'input-error' : ''}`}>
                      {OIL_TYPES.map((type) => (
                        <option key={type.value} value={type.value}>{type.label}</option>
                      ))}
                    </select>
                    {errors.oilType && <p className="error-message">{errors.oilType.message}</p>}
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
                    <input
                      {...register('containerType')}
                      className="input"
                      placeholder="Ex: Tambor 200L, IBC 1000L"
                    />
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
                </div>
              </div>
            </div>

            {/* Quality Indicators */}
            <div className="card">
              <div className="card-header">
                <h3 className="text-lg font-semibold text-gray-900 flex items-center space-x-2">
                  <Shield className="w-5 h-5 text-primary-600" />
                  <span>Indicadores de Qualidade</span>
                </h3>
              </div>
              <div className="card-body">
                <QualityIndicator
                  label="Qualidade"
                  value={watch('qualityRating') ? `${watch('qualityRating')}/5` : 'Não avaliado'}
                  icon={<Shield className="w-5 h-5" />}
                  color="text-yellow-600"
                  threshold={3}
                  current={watch('qualityRating') || 0}
                />
                <QualityIndicator
                  label="Temperatura"
                  value={watch('temperatureCelsius') !== undefined ? `${watch('temperatureCelsius')}°C` : 'Não informado'}
                  icon={<Droplets className="w-5 h-5" />}
                  color="text-orange-600"
                  threshold={30}
                  current={watch('temperatureCelsius') || 0}
                />
                <QualityIndicator
                  label="pH"
                  value={watch('phLevel') != null ? watch('phLevel')!.toFixed(1) : 'Não informado'}
                  icon={<Weight className="w-5 h-5" />}
                  color="text-blue-600"
                  threshold={7}
                  current={watch('phLevel') || 0}
                  isPh={true}
                />
                <QualityIndicator
                  label="Contaminação"
                  value={watch('contaminationPercentage') !== undefined ? `${watch('contaminationPercentage')}%` : 'Não informado'}
                  icon={<AlertTriangle className="w-5 h-5" />}
                  color="text-red-600"
                  threshold={10}
                  current={watch('contaminationPercentage') || 0}
                  inverse={true}
                />
              </div>
            </div>

            {/* Confirmation Code & Establishment Signature */}
            <div className="card border-blue-200">
              <div className="card-header bg-blue-50 border-blue-200">
                <h3 className="text-lg font-semibold text-gray-900 flex items-center space-x-2">
                  <CheckCircle className="w-5 h-5 text-blue-600" />
                  <span>Confirmação e Certificado</span>
                </h3>
              </div>
              <div className="card-body space-y-4">
                <div>
                  <label className="label">Código de Confirmação</label>
                  <div className="flex items-center space-x-2">
                    <input
                      {...register('confirmationCode')}
                      className="input font-mono text-lg tracking-wider bg-gray-50"
                      readOnly
                    />
                    <button
                      type="button"
                      onClick={() => setValue('confirmationCode', generateConfirmationCode())}
                      className="btn-outline text-sm"
                    >
                      Gerar Novo
                    </button>
                  </div>
                  <p className="text-xs text-gray-500 mt-1">
                    Este código será usado para gerar o certificado de coleta e para o estabelecimento confirmar a entrega.
                  </p>
                </div>

                <div className="flex items-center space-x-3 p-4 bg-gray-50 rounded-lg">
                  <input
                    type="checkbox"
                    {...register('establishmentConfirmed')}
                    id="establishmentConfirmed"
                    className="w-5 h-5 text-primary-600 border-gray-300 rounded focus:ring-primary-500"
                  />
                  <label htmlFor="establishmentConfirmed" className="text-sm font-medium text-gray-900 cursor-pointer">
                    Estabelecimento confirmou a entrega do óleo
                  </label>
                </div>

                {watch('establishmentConfirmed') && (
                  <div className="p-3 bg-green-50 border border-green-200 rounded-lg">
                    <p className="text-sm text-green-800 flex items-center space-x-2">
                      <CheckCircle className="w-4 h-4" />
                      <span>Confirmação registrada. O certificado será gerado com o código: <strong>{watch('confirmationCode')}</strong></span>
                    </p>
                  </div>
                )}
              </div>
            </div>

            {/* Additional Info */}
            <div className="card">
              <div className="card-header">
                <h3 className="text-lg font-semibold text-gray-900">Informações Adicionais</h3>
              </div>
              <div className="card-body space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="label">Fotos (URLs separadas por vírgula)</label>
                    <input {...register('photos')} className="input" placeholder="https://exemplo.com/foto1.jpg, https://exemplo.com/foto2.jpg" />
                  </div>
                  <div>
                    <label className="label">Observações</label>
                    <textarea {...register('notes')} className="input" rows={3} placeholder="Observações adicionais sobre a coleta..." />
                  </div>
                </div>

                <div>
                  <label className="label">Coordenadas GPS</label>
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
                  <p className="text-xs text-gray-500 mt-1">Preenchido automaticamente com a localização do ponto de coleta</p>
                </div>
              </div>
            </div>

            {/* Submit */}
            <div className="flex justify-end space-x-3">
              <button
                type="button"
                onClick={() => router.push(`/schedules/${id}`)}
                className="btn-outline"
              >
                <XCircle className="w-4 h-4 mr-2" />
                Cancelar
              </button>
              <button
                type="submit"
                disabled={createMutation.isPending}
                className="btn-success"
              >
                <Save className="w-4 h-4 mr-2" />
                {createMutation.isPending ? 'Registrando...' : 'Registrar Coleta e Finalizar'}
              </button>
            </div>
          </form>
        </div>

        {/* Sidebar - Schedule Info */}
        <div className="space-y-6">
          <div className="card">
            <div className="card-header">
              <h3 className="text-lg font-semibold text-gray-900">Detalhes do Agendamento</h3>
            </div>
            <div className="card-body space-y-4">
              <InfoRow label="Ponto de Coleta" value={schedule.collectionPointName} icon={<MapPin className="w-5 h-5" />} color="text-blue-600" />
              <InfoRow label="Data" value={format(new Date(schedule.scheduledDate), 'dd/MM/yyyy', { locale: ptBR })} icon={<Calendar className="w-5 h-5" />} color="text-blue-600" />
              <InfoRow label="Horário" value={`${schedule.startTime}${schedule.endTime ? ` - ${schedule.endTime}` : ''}`} icon={<Clock className="w-5 h-5" />} color="text-blue-600" />
              <InfoRow label="Volume Estimado" value={schedule.estimatedVolumeLiters ? `${schedule.estimatedVolumeLiters.toLocaleString('pt-BR')} L` : 'Não informado'} icon={<Package className="w-5 h-5" />} color="text-blue-600" />
              <InfoRow label="Status" value={SCHEDULE_STATUS_LABELS[schedule.status as keyof typeof SCHEDULE_STATUS_LABELS]} icon={<Truck className="w-5 h-5" />} color="text-blue-600" badgeClass={SCHEDULE_STATUS_COLORS[schedule.status as keyof typeof SCHEDULE_STATUS_COLORS]} />

              {point && (
                <div className="pt-4 border-t border-gray-200">
                  <p className="font-medium text-gray-900 mb-2">Endereço do Ponto</p>
                  <p className="text-sm text-gray-600">{point.address}</p>
                  <p className="text-sm text-gray-600">{point.neighborhood}, {point.city}/{point.state}</p>
                  {point.contactName && (
                    <p className="text-sm text-gray-600 mt-1">Contato: {point.contactName} - {point.contactPhone || 'N/A'}</p>
                  )}
                </div>
              )}
            </div>
          </div>

          {/* Quick Actions */}
          <div className="card">
            <div className="card-header">
              <h3 className="text-lg font-semibold text-gray-900">Ações Rápidas</h3>
            </div>
            <div className="card-body space-y-2">
              <button
                onClick={() => {
                  if (point?.latitude && point?.longitude) {
                    window.open(`https://www.google.com/maps/dir/?api=1&destination=${point.latitude},${point.longitude}`, '_blank');
                  }
                }}
                className="btn-outline w-full justify-start"
              >
                <Navigation className="w-4 h-4 mr-2" />
                Abrir Rota no Maps
              </button>
              <button
                onClick={() => {
                  if (point?.contactPhone) {
                    window.open(`tel:${point.contactPhone.replace(/\D/g, '')}`, '_self');
                  }
                }}
                disabled={!point?.contactPhone}
                className="btn-outline w-full justify-start"
              >
                <Phone className="w-4 h-4 mr-2" />
                Ligar para Contato
              </button>
              <button
                onClick={() => router.push(`/incidents/new?pointId=${schedule.collectionPointId}&scheduleId=${id}`)}
                className="btn-outline w-full justify-start text-yellow-600 border-yellow-300 hover:bg-yellow-50"
              >
                <AlertTriangle className="w-4 h-4 mr-2" />
                Reportar Incidente
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function InfoRow({ label, value, icon, color, badgeClass }: {
  label: string;
  value: string;
  icon: React.ReactNode;
  color: string;
  badgeClass?: string;
}) {
  return (
    <div className="flex items-center space-x-3">
      <div className={`p-2 rounded-lg bg-white ${color}`}>{icon}</div>
      <div className="flex-1 min-w-0">
        <p className="text-xs font-medium text-gray-500 truncate">{label}</p>
        {badgeClass ? (
          <span className={`badge ${badgeClass}`}>{value}</span>
        ) : (
          <p className="text-sm font-semibold text-gray-900 truncate">{value}</p>
        )}
      </div>
    </div>
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