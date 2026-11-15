'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { recordsApi, collectionPointsApi, usersApi } from '@/lib/api';
import { DailyVolumeDTO, CollectorPerformanceDTO, OilTypeVolumeDTO } from '@/types';
import {
  BarChart,
  Bar,
  LineChart,
  Line,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { Calendar, Package, Users, TrendingUp, Download, Filter } from 'lucide-react';
import { format, subDays, startOfMonth, endOfMonth, subMonths } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import toast from 'react-hot-toast';

interface ReportFilters {
  startDate: string;
  endDate: string;
}

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16'];

export default function ReportsPage() {
  const [filters, setFilters] = useState<ReportFilters>({
    startDate: format(subDays(new Date(), 30), 'yyyy-MM-dd'),
    endDate: format(new Date(), 'yyyy-MM-dd'),
  });
  const [activeTab, setActiveTab] = useState<'volume' | 'collectors' | 'oil-types' | 'points'>('volume');

  const { data: dailyVolumeData, isLoading: volumeLoading } = useQuery({
    queryKey: ['dailyVolumeReport', filters.startDate, filters.endDate],
    queryFn: () => recordsApi.getDailyVolumeReport(filters.startDate, filters.endDate),
  });

  const { data: collectorPerfData, isLoading: collectorLoading } = useQuery({
    queryKey: ['collectorPerformanceReport', filters.startDate, filters.endDate],
    queryFn: () => recordsApi.getCollectorPerformanceReport(filters.startDate, filters.endDate),
  });

  const { data: oilTypeData, isLoading: oilTypeLoading } = useQuery({
    queryKey: ['oilTypeVolumeReport', filters.startDate, filters.endDate],
    queryFn: () => recordsApi.getOilTypeVolumeReport(filters.startDate, filters.endDate),
  });

  const { data: pointsData } = useQuery({
    queryKey: ['collectionPoints'],
    queryFn: () => collectionPointsApi.getAllList(),
  });

  const dailyVolume = dailyVolumeData?.data || [];
  const collectorPerformance = collectorPerfData?.data || [];
  const oilTypeVolumes = oilTypeData?.data || [];
  const points = pointsData?.data || [];

  const totalVolume = dailyVolume.reduce((sum, d) => sum + d.totalVolume, 0);
  const totalCollections = collectorPerformance.reduce((sum, c) => sum + c.totalCollections, 0);
  const avgQuality = collectorPerformance.length > 0
    ? collectorPerformance.reduce((sum, c) => sum + c.averageQuality, 0) / collectorPerformance.length
    : 0;
  const activePoints = points.filter(p => p.status === 'ACTIVE').length;

  const handleDateChange = (field: 'startDate' | 'endDate', value: string) => {
    setFilters(prev => ({ ...prev, [field]: value }));
  };

  const handleExport = (type: string) => {
    toast.success(`Exportando relatório de ${type}... (implementar backend)`);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="page-title">Relatórios</h1>
          <p className="page-subtitle">Análises e métricas do sistema de coleta</p>
        </div>
        <div className="flex items-center space-x-2">
          <Download className="w-5 h-5 text-gray-400" />
          <select
            value={activeTab}
            onChange={(e) => setActiveTab(e.target.value as typeof activeTab)}
            className="input w-auto"
          >
            <option value="volume">Volume Diário</option>
            <option value="collectors">Desempenho Coletores</option>
            <option value="oil-types">Tipos de Óleo</option>
            <option value="points">Pontos de Coleta</option>
          </select>
        </div>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="flex flex-col sm:flex-row gap-4 items-end">
            <div className="flex-1">
              <label className="label">Data Inicial</label>
              <input
                type="date"
                value={filters.startDate}
                onChange={(e) => handleDateChange('startDate', e.target.value)}
                className="input"
                max={filters.endDate}
              />
            </div>
            <div className="flex-1">
              <label className="label">Data Final</label>
              <input
                type="date"
                value={filters.endDate}
                onChange={(e) => handleDateChange('endDate', e.target.value)}
                className="input"
                min={filters.startDate}
                max={format(new Date(), 'yyyy-MM-dd')}
              />
            </div>
            <div className="flex space-x-2">
              <button
                onClick={() => setFilters({
                  startDate: format(subDays(new Date(), 7), 'yyyy-MM-dd'),
                  endDate: format(new Date(), 'yyyy-MM-dd'),
                })}
                className="btn-outline text-sm"
              >
                Últimos 7 dias
              </button>
              <button
                onClick={() => setFilters({
                  startDate: format(subDays(new Date(), 30), 'yyyy-MM-dd'),
                  endDate: format(new Date(), 'yyyy-MM-dd'),
                })}
                className="btn-outline text-sm"
              >
                Últimos 30 dias
              </button>
              <button
                onClick={() => setFilters({
                  startDate: format(startOfMonth(new Date()), 'yyyy-MM-dd'),
                  endDate: format(endOfMonth(new Date()), 'yyyy-MM-dd'),
                })}
                className="btn-outline text-sm"
              >
                Este mês
              </button>
              <button
                onClick={() => setFilters({
                  startDate: format(startOfMonth(subMonths(new Date(), 1)), 'yyyy-MM-dd'),
                  endDate: format(endOfMonth(subMonths(new Date(), 1)), 'yyyy-MM-dd'),
                })}
                className="btn-outline text-sm"
              >
                Mês passado
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Volume Total"
          value={`${totalVolume.toLocaleString('pt-BR')} L`}
          icon={<Package className="w-6 h-6" />}
          color="text-primary-600"
          bgColor="bg-primary-100"
        />
        <StatCard
          title="Total de Coletas"
          value={totalCollections}
          icon={<Calendar className="w-6 h-6" />}
          color="text-green-600"
          bgColor="bg-green-100"
        />
        <StatCard
          title="Qualidade Média"
          value={avgQuality.toFixed(1)}
          icon={<TrendingUp className="w-6 h-6" />}
          color="text-purple-600"
          bgColor="bg-purple-100"
        />
        <StatCard
          title="Pontos Ativos"
          value={activePoints}
          icon={<Users className="w-6 h-6" />}
          color="text-blue-600"
          bgColor="bg-blue-100"
        />
      </div>

      <div className="card">
        <div className="card-header flex items-center justify-between">
          <h3 className="text-lg font-semibold text-gray-900">
            {activeTab === 'volume' && 'Volume Diário Coletado'}
            {activeTab === 'collectors' && 'Desempenho dos Coletores'}
            {activeTab === 'oil-types' && 'Volume por Tipo de Óleo'}
            {activeTab === 'points' && 'Volume por Ponto de Coleta'}
          </h3>
          <div className="flex items-center space-x-2">
            <button
              onClick={() => handleExport(activeTab)}
              className="btn-outline text-sm"
            >
              <Download className="w-4 h-4 mr-1" />
              Exportar
            </button>
          </div>
        </div>
        <div className="card-body">
          {(activeTab === 'volume' && volumeLoading) ||
          (activeTab === 'collectors' && collectorLoading) ||
          (activeTab === 'oil-types' && oilTypeLoading) ? (
            <div className="flex items-center justify-center h-64">
              <div className="spinner w-8 h-8"></div>
            </div>
          ) : activeTab === 'volume' ? (
            <ResponsiveContainer width="100%" height={400}>
              <BarChart data={dailyVolume}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis
                  dataKey="date"
                  tickFormatter={(value) => format(new Date(value), 'dd/MM', { locale: ptBR })}
                  tick={{ fontSize: 12 }}
                />
                <YAxis tickFormatter={(value) => `${value.toLocaleString('pt-BR')} L`} tick={{ fontSize: 12 }} />
                <Tooltip
                  formatter={(value: number) => [`${value.toLocaleString('pt-BR')} L`, 'Volume']}
                  labelFormatter={(value) => format(new Date(value), 'dd/MM/yyyy', { locale: ptBR })}
                />
                <Legend />
                <Bar dataKey="totalVolume" fill="#3b82f6" radius={[4, 4, 0, 0]} name="Volume (L)" />
              </BarChart>
            </ResponsiveContainer>
          ) : activeTab === 'collectors' ? (
            <div className="space-y-6">
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={collectorPerformance} layout="vertical">
                  <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                  <XAxis tickFormatter={(value) => `${value.toLocaleString('pt-BR')} L`} tick={{ fontSize: 12 }} />
                  <YAxis
                    type="category"
                    dataKey="collectorName"
                    width={120}
                    tick={{ fontSize: 12 }}
                  />
                  <Tooltip
                    formatter={(value: number, name: string) => [
                      name === 'totalVolume' ? `${value.toLocaleString('pt-BR')} L` : value,
                      name === 'totalVolume' ? 'Volume' : name === 'totalCollections' ? 'Coletas' : 'Qualidade',
                    ]}
                    labelFormatter={(value) => value}
                  />
                  <Legend />
                  <Bar dataKey="totalVolume" fill="#3b82f6" radius={[0, 4, 4, 0]} name="Volume (L)" />
                </BarChart>
              </ResponsiveContainer>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={collectorPerformance} layout="vertical">
                  <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                  <XAxis tick={{ fontSize: 12 }} />
                  <YAxis
                    type="category"
                    dataKey="collectorName"
                    width={120}
                    tick={{ fontSize: 12 }}
                  />
                  <Tooltip
                    formatter={(value: number) => [`${value.toFixed(1)}`, 'Qualidade Média']}
                    labelFormatter={(value) => value}
                  />
                  <Legend />
                  <Bar dataKey="averageQuality" fill="#10b981" radius={[0, 4, 4, 0]} name="Qualidade Média" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          ) : activeTab === 'oil-types' ? (
            <ResponsiveContainer width="100%" height={400}>
              <PieChart>
                <Pie
                  data={oilTypeVolumes}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={120}
                  dataKey="totalVolume"
                  nameKey="oilType"
                  label={({ oilType, totalVolume, percent }) =>
                    `${oilType}: ${totalVolume.toLocaleString('pt-BR')} L (${(percent * 100).toFixed(1)}%)`
                  }
                  labelLine={false}
                >
                  {oilTypeVolumes.map((_, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip
                  formatter={(value: number) => [`${value.toLocaleString('pt-BR')} L`, 'Volume']}
                />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          ) : activeTab === 'points' ? (
            <ResponsiveContainer width="100%" height={400}>
              <BarChart data={points.filter(p => p.currentVolumeLiters > 0).slice(0, 20)}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis
                  dataKey="name"
                  tick={{ fontSize: 11 }}
                  interval={0}
                />
                <YAxis tickFormatter={(value) => `${value.toLocaleString('pt-BR')} L`} tick={{ fontSize: 12 }} />
                <Tooltip
                  formatter={(value: number) => [`${value.toLocaleString('pt-BR')} L`, 'Volume Atual']}
                />
                <Legend />
                <Bar dataKey="currentVolumeLiters" fill="#10b981" radius={[4, 4, 0, 0]} name="Volume Atual (L)" />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="text-center py-12 text-gray-500">Selecione uma aba</div>
          )}
        </div>
      </div>

      {activeTab === 'collectors' && collectorPerformance.length > 0 && (
        <div className="card">
          <div className="card-header">
            <h3 className="text-lg font-semibold text-gray-900">Tabela de Desempenho dos Coletores</h3>
          </div>
          <div className="card-body p-0">
            <div className="table-container">
              <table className="table">
                <thead>
                  <tr>
                    <th>Coletor</th>
                    <th>Total de Coletas</th>
                    <th>Volume Total (L)</th>
                    <th>Qualidade Média</th>
                    <th>Volume por Coleta</th>
                  </tr>
                </thead>
                <tbody>
                  {collectorPerformance.map((collector) => (
                    <tr key={collector.collectorId}>
                      <td className="font-medium text-gray-900">{collector.collectorName}</td>
                      <td>{collector.totalCollections}</td>
                      <td className="font-medium">{collector.totalVolume.toLocaleString('pt-BR')}</td>
                      <td>
                        <div className="flex items-center space-x-1">
                          {[1, 2, 3, 4, 5].map((star) => (
                            <span
                              key={star}
                              className={`text-lg ${
                                star <= Math.round(collector.averageQuality) ? 'text-yellow-400' : 'text-gray-300'
                              }`}
                            >
                              ★
                            </span>
                          ))}
                          <span className="text-sm text-gray-500 ml-1">{collector.averageQuality.toFixed(1)}</span>
                        </div>
                      </td>
                      <td>{(collector.totalVolume / Math.max(collector.totalCollections, 1)).toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'oil-types' && oilTypeVolumes.length > 0 && (
        <div className="card">
          <div className="card-header">
            <h3 className="text-lg font-semibold text-gray-900">Detalhamento por Tipo de Óleo</h3>
          </div>
          <div className="card-body p-0">
            <div className="table-container">
              <table className="table">
                <thead>
                  <tr>
                    <th>Tipo de Óleo</th>
                    <th>Volume Total (L)</th>
                    <th>Percentual</th>
                  </tr>
                </thead>
                <tbody>
                  {oilTypeVolumes.map((item, index) => {
                    const total = oilTypeVolumes.reduce((sum, i) => sum + i.totalVolume, 0);
                    const percent = total > 0 ? (item.totalVolume / total) * 100 : 0;
                    return (
                      <tr key={index}>
                        <td className="capitalize font-medium text-gray-900">{item.oilType}</td>
                        <td>{item.totalVolume.toLocaleString('pt-BR')}</td>
                        <td>
                          <div className="flex items-center space-x-2">
                            <div className="w-24 h-2 bg-gray-200 rounded-full overflow-hidden">
                              <div
                                className="h-full rounded-full"
                                style={{ width: `${percent}%`, backgroundColor: COLORS[index % COLORS.length] }}
                              />
                            </div>
                            <span className="text-sm text-gray-600">{percent.toFixed(1)}%</span>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function StatCard({ title, value, icon, color, bgColor }: {
  title: string;
  value: string | number;
  icon: React.ReactNode;
  color: string;
  bgColor: string;
}) {
  return (
    <div className="card">
      <div className="card-body">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-sm font-medium text-gray-500">{title}</p>
            <p className="text-3xl font-bold text-gray-900 mt-1">{value}</p>
          </div>
          <div className={`p-3 rounded-xl ${bgColor}`}>
            <div className={`${color}`}>{icon}</div>
          </div>
        </div>
      </div>
    </div>
  );
}