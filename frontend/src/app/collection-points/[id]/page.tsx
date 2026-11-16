'use client';

import { useParams } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { collectionPointsApi, schedulesApi, recordsApi } from '@/lib/api';
import { CollectionPoint, CollectionSchedule, CollectionRecord } from '@/types';
import { useRouter } from 'next/navigation';
import toast from 'react-hot-toast';
import {
  MapPin,
  Truck,
  Package,
  Calendar,
  Clock,
  AlertCircle,
  CheckCircle,
  XCircle,
  Edit,
  Trash2,
  ArrowLeft,
  MoreVertical,
  User,
  Phone,
  Mail,
  Map,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { STATUS_LABELS, STATUS_COLORS, PointStatus, ScheduleStatus } from '@/types';

interface ScheduleFormData {
  collectionPointId: string;
  collectorId: string;
  scheduledDate: string;
  startTime: string;
  endTime: string;
  notes?: string;
}

export default function CollectionPointDetailPage() {
  const params = useParams();
  const router = useRouter();
  const queryClient = useQueryClient();
  const id = params?.id as string;

  if (!id) {
    return null; // or redirect
  }

  const { data: pointData, isLoading: pointLoading, error: pointError } = useQuery({
    queryKey: ['collectionPoint', id],
    queryFn: () => collectionPointsApi.getById(id),
    enabled: !!id,
  });

  const { data: schedulesData, isLoading: schedulesLoading } = useQuery({
    queryKey: ['collectionPointSchedules', id],
    queryFn: () => schedulesApi.getByPoint(id),
    enabled: !!id,
  });

  const { data: recordsData, isLoading: recordsLoading } = useQuery({
    queryKey: ['collectionPointRecords', id],
    queryFn: () => recordsApi.getByPoint(id),
    enabled: !!id,
  });

  const point = pointData?.data;
  const schedules: CollectionSchedule[] = schedulesData?.data || [];
  const records: CollectionRecord[] = recordsData?.data || [];

  const deleteMutation = useMutation({
    mutationFn: (id: string) => collectionPointsApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['collectionPoints'] });
      toast.success('Ponto de coleta excluído com sucesso!');
      router.push('/collection-points');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao excluir ponto de coleta');
    },
  });

  const handleDelete = () => {
    if (confirm('Tem certeza que deseja excluir este ponto de coleta? Esta ação não pode ser desfeita.')) {
      deleteMutation.mutate(id);
    }
  };

  if (pointLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="spinner w-8 h-8"></div>
      </div>
    );
  }

  if (pointError || !point) {
    return (
      <div className="card">
        <div className="card-body text-center py-12">
          <AlertCircle className="w-12 h-12 mx-auto mb-4 text-red-500" />
          <h3 className="text-lg font-semibold text-gray-900">Ponto de coleta não encontrado</h3>
          <p className="text-gray-500 mt-2">O ponto de coleta solicitado não existe ou foi removido.</p>
          <button onClick={() => router.push('/collection-points')} className="btn-primary mt-4 inline-flex">
            <ArrowLeft className="w-4 h-4 mr-2" />
            Voltar para lista
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <button onClick={() => router.back()} className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100">
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="page-title">{point.name}</h1>
            <p className="page-subtitle">{point.address}, {point.city}/{point.state}</p>
          </div>
        </div>
        <div className="flex items-center space-x-2">
          <span className={`badge ${STATUS_COLORS[point.status as PointStatus]}`}>
            {STATUS_LABELS[point.status as PointStatus]}
          </span>
          <button onClick={handleDelete} className="btn-danger">
            <Trash2 className="w-4 h-4 mr-2" />
            Excluir
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <div className="card">
            <div className="card-header">
              <h3 className="text-lg font-semibold text-gray-900">Informações do Ponto</h3>
            </div>
            <div className="card-body">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Endereço Completo</label>
                  <p className="mt-1 text-sm text-gray-900">{point.address}</p>
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Bairro</label>
                  <p className="mt-1 text-sm text-gray-900">{point.neighborhood || '-'}</p>
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Cidade/Estado</label>
                  <p className="mt-1 text-sm text-gray-900">{point.city}/{point.state}</p>
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">CEP</label>
                  <p className="mt-1 text-sm text-gray-900">{point.zipCode || '-'}</p>
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Coordenadas</label>
                  <p className="mt-1 text-sm text-gray-900">
                    {point.latitude && point.longitude
                      ? `${point.latitude.toFixed(6)}, ${point.longitude.toFixed(6)}`
                      : 'Não informado'}
                  </p>
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Capacidade</label>
                  <p className="mt-1 text-sm text-gray-900">
                    {point.capacityLiters ? `${point.capacityLiters.toLocaleString('pt-BR')} L` : 'Não informado'}
                  </p>
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Volume Atual</label>
                  <div className="mt-1 flex items-center space-x-2">
                    <span className="text-sm font-medium text-gray-900">
                      {point.currentVolumeLiters.toLocaleString('pt-BR')} L
                    </span>
                    <div className="w-32 h-2 bg-gray-200 rounded-full overflow-hidden">
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
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Coletor Responsável</label>
                  <p className="mt-1 text-sm text-gray-900">{point.collectorName || 'Não atribuído'}</p>
                </div>
              </div>

              {point.description && (
                <div className="mt-6 pt-6 border-t border-gray-200">
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Descrição</label>
                  <p className="mt-1 text-sm text-gray-900">{point.description}</p>
                </div>
              )}

              {(point.contactName || point.contactPhone || point.contactEmail) && (
                <div className="mt-6 pt-6 border-t border-gray-200">
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Contato</label>
                  <div className="mt-2 space-y-2">
                    {point.contactName && (
                      <div className="flex items-center space-x-2 text-sm text-gray-900">
                        <User className="w-4 h-4 text-gray-400" />
                        <span>{point.contactName}</span>
                      </div>
                    )}
                    {point.contactPhone && (
                      <div className="flex items-center space-x-2 text-sm text-gray-900">
                        <Phone className="w-4 h-4 text-gray-400" />
                        <span>{point.contactPhone}</span>
                      </div>
                    )}
                    {point.contactEmail && (
                      <div className="flex items-center space-x-2 text-sm text-gray-900">
                        <Mail className="w-4 h-4 text-gray-400" />
                        <span>{point.contactEmail}</span>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {(point.operatingHours || point.acceptedOilTypes) && (
                <div className="mt-6 pt-6 border-t border-gray-200">
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wider">Detalhes Adicionais</label>
                  <div className="mt-2 space-y-2">
                    {point.operatingHours && (
                      <div>
                        <p className="text-xs font-medium text-gray-500">Horário de Funcionamento</p>
                        <pre className="mt-1 text-xs text-gray-900 bg-gray-50 p-3 rounded overflow-x-auto">{point.operatingHours}</pre>
                      </div>
                    )}
                    {point.acceptedOilTypes && (
                      <div>
                        <p className="text-xs font-medium text-gray-500">Tipos de Óleo Aceitos</p>
                        <pre className="mt-1 text-xs text-gray-900 bg-gray-50 p-3 rounded overflow-x-auto">{point.acceptedOilTypes}</pre>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          </div>

          <div className="card">
            <div className="card-header flex items-center justify-between">
              <h3 className="text-lg font-semibold text-gray-900">Agendamentos</h3>
            </div>
            <div className="card-body p-0">
              {schedulesLoading ? (
                <div className="flex items-center justify-center h-48">
                  <div className="spinner w-6 h-6"></div>
                </div>
              ) : (
                <div className="table-container">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Data</th>
                        <th>Horário</th>
                        <th>Coletor</th>
                        <th>Status</th>
                        <th>Observações</th>
                      </tr>
                    </thead>
                    <tbody>
                      {schedules.length === 0 ? (
                        <tr>
                          <td colSpan={5} className="text-center py-8 text-gray-500">
                            Nenhum agendamento encontrado
                          </td>
                        </tr>
                      ) : (
                        schedules.map((schedule) => (
                          <tr key={schedule.id}>
                            <td>
                              {format(new Date(schedule.scheduledDate), 'dd/MM/yyyy', { locale: ptBR })}
                              {schedule.isToday && <span className="ml-2 badge-info">Hoje</span>}
                            </td>
                            <td>{schedule.startTime}{schedule.endTime && ` - ${schedule.endTime}`}</td>
                            <td>{schedule.collectorName}</td>
                            <td>
                              <span className={`badge ${
                                schedule.status === 'COMPLETED' ? 'badge-success' :
                                schedule.status === 'CANCELLED' ? 'badge-danger' :
                                schedule.status === 'IN_PROGRESS' ? 'badge-warning' : 'badge-info'
                              }`}>
                                {schedule.status}
                              </span>
                            </td>
                            <td className="text-sm text-gray-500 max-w-xs truncate">{schedule.notes || '-'}</td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>

          <div className="card">
            <div className="card-header flex items-center justify-between">
              <h3 className="text-lg font-semibold text-gray-900">Histórico de Coletas</h3>
            </div>
            <div className="card-body p-0">
              {recordsLoading ? (
                <div className="flex items-center justify-center h-48">
                  <div className="spinner w-6 h-6"></div>
                </div>
              ) : (
                <div className="table-container">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Data</th>
                        <th>Coletor</th>
                        <th>Volume (L)</th>
                        <th>Tipo de Óleo</th>
                        <th>Status</th>
                        <th>Verificado</th>
                      </tr>
                    </thead>
                    <tbody>
                      {records.length === 0 ? (
                        <tr>
                          <td colSpan={6} className="text-center py-8 text-gray-500">
                            Nenhum registro de coleta encontrado
                          </td>
                        </tr>
                      ) : (
                        records.map((record) => (
                          <tr key={record.id}>
                            <td>{format(new Date(record.collectionDate), 'dd/MM/yyyy HH:mm', { locale: ptBR })}</td>
                            <td>{record.collectorName}</td>
                            <td className="font-medium">{record.volumeCollectedLiters.toLocaleString('pt-BR')}</td>
                            <td className="capitalize">{record.oilType}</td>
                            <td>
                              <span className={record.verifiedByManager ? 'badge-success' : 'badge-warning'}>
                                {record.verifiedByManager ? 'Verificado' : 'Pendente'}
                              </span>
                            </td>
                            <td>
                              <span className={record.verifiedByManager ? 'badge-success' : 'badge-warning'}>
                                {record.verifiedByManager ? 'Sim' : 'Não'}
                              </span>
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
        </div>

        <div className="space-y-6">
          <div className="card">
            <div className="card-header">
              <h3 className="text-lg font-semibold text-gray-900">Localização no Mapa</h3>
            </div>
            <div className="card-body p-0">
              <div className="aspect-video bg-gray-100 relative overflow-hidden">
                {point.latitude && point.longitude ? (
                  <iframe
                    width="100%"
                    height="100%"
                    frameBorder="0"
                    style={{ border: 0 }}
                    src={`https://www.google.com/maps/embed/v1/place?key=${process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY || 'YOUR_API_KEY'}&q=${point.latitude},${point.longitude}&zoom=16`}
                    allowFullScreen
                    title={`Localização de ${point.name}`}
                  />
                ) : (
                  <div className="flex flex-col items-center justify-center h-full text-gray-500">
                    <MapPin className="w-12 h-12 mb-4 text-gray-300" />
                    <p className="text-lg">Coordenadas não informadas</p>
                    <p className="text-sm">Adicione latitude e longitude para ver o mapa</p>
                  </div>
                )}
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card-header">
              <h3 className="text-lg font-semibold text-gray-900">Estatísticas Rápidas</h3>
            </div>
            <div className="card-body space-y-4">
              <StatItem label="Total de Agendamentos" value={schedules.length} icon={<Calendar className="w-5 h-5" />} color="text-blue-600" />
              <StatItem label="Agendamentos Concluídos" value={schedules.filter(s => s.status === 'COMPLETED').length} icon={<CheckCircle className="w-5 h-5" />} color="text-green-600" />
              <StatItem label="Agendamentos Pendentes" value={schedules.filter(s => s.status === 'PENDING' || s.status === 'CONFIRMED').length} icon={<Clock className="w-5 h-5" />} color="text-yellow-600" />
              <StatItem label="Total de Coletas" value={records.length} icon={<Package className="w-5 h-5" />} color="text-purple-600" />
              <StatItem label="Volume Total Coletado" value={`${records.reduce((sum, r) => sum + r.volumeCollectedLiters, 0).toLocaleString('pt-BR')} L`} icon={<Truck className="w-5 h-5" />} color="text-indigo-600" />
              <StatItem label="Pendentes Verificação" value={records.filter(r => !r.verifiedByManager).length} icon={<AlertCircle className="w-5 h-5" />} color="text-orange-600" />
            </div>
          </div>

          <div className="card">
            <div className="card-header">
              <h3 className="text-lg font-semibold text-gray-900">Datas Importantes</h3>
            </div>
            <div className="card-body space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-500">Criado em</span>
                <span className="text-sm font-medium text-gray-900">{format(new Date(point.createdAt), 'dd/MM/yyyy HH:mm', { locale: ptBR })}</span>
              </div>
              {point.updatedAt && point.updatedAt !== point.createdAt && (
                <div className="flex items-center justify-between">
                  <span className="text-sm text-gray-500">Atualizado em</span>
                  <span className="text-sm font-medium text-gray-900">{format(new Date(point.updatedAt), 'dd/MM/yyyy HH:mm', { locale: ptBR })}</span>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function StatItem({ label, value, icon, color }: { label: string; value: string | number; icon: React.ReactNode; color: string }) {
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