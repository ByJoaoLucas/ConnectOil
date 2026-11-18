'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { usersApi } from '@/lib/api';
import { User as UserType, UserRole } from '@/types';
import toast from 'react-hot-toast';
import {
  Plus,
  Search,
  Filter,
  User,
  UserCheck,
  UserX,
  Edit,
  Trash2,
  Shield,
  Mail,
  Phone,
  AlertCircle,
  CheckCircle,
  XCircle,
} from 'lucide-react';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { USER_ROLE_LABELS } from '@/types';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

interface UserFormData {
  name: string;
  email: string;
  cpf: string;
  phoneNumber?: string;
  password?: string;
  role: UserRole;
}

const userSchema = z.object({
  name: z.string().min(2, 'Nome deve ter pelo menos 2 caracteres').max(100),
  email: z.string().email('E-mail inválido'),
  cpf: z.string().regex(/^\d{3}\.?\d{3}\.?\d{3}-?\d{2}$/, 'CPF inválido (formato: 000.000.000-00)'),
  phoneNumber: z.string().optional(),
  password: z.string().min(6, 'Senha deve ter pelo menos 6 caracteres').optional().or(z.literal('')),
  role: z.enum(['ADMIN', 'COLLECTOR', 'CLIENT']),
});

export default function UsersPage() {
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState<UserRole | 'ALL'>('ALL');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<UserType | null>(null);
  const queryClient = useQueryClient();

  const { data: usersData, isLoading } = useQuery({
    queryKey: ['users'],
    queryFn: () => usersApi.getAllList(),
  });

  const users = usersData?.data || [];

  const filteredUsers = users.filter((user: UserType) => {
    const matchesSearch =
      user.name.toLowerCase().includes(search.toLowerCase()) ||
      user.email.toLowerCase().includes(search.toLowerCase()) ||
      user.cpf?.includes(search) ||
      user.cnpj?.includes(search);
    const matchesRole = roleFilter === 'ALL' || user.role === roleFilter;
    const matchesStatus =
      statusFilter === 'ALL' ||
      (statusFilter === 'ACTIVE' && user.isActive) ||
      (statusFilter === 'INACTIVE' && !user.isActive);
    return matchesSearch && matchesRole && matchesStatus;
  });

  const createMutation = useMutation({
    mutationFn: (data: UserFormData) => usersApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      toast.success('Usuário criado com sucesso!');
      setIsModalOpen(false);
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao criar usuário');
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: UserFormData }) =>
      usersApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      toast.success('Usuário atualizado com sucesso!');
      setIsModalOpen(false);
      setEditingUser(null);
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao atualizar usuário');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => usersApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      toast.success('Usuário excluído com sucesso!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao excluir usuário');
    },
  });

  const activateMutation = useMutation({
    mutationFn: (id: string) => usersApi.activate(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      toast.success('Usuário ativado com sucesso!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao ativar usuário');
    },
  });

  const handleSubmit = (data: UserFormData) => {
    const cleanData = { ...data };
    if (!cleanData.password) delete cleanData.password;
    if (editingUser) {
      updateMutation.mutate({ id: editingUser.id, data: cleanData });
    } else {
      createMutation.mutate(cleanData);
    }
  };

  const handleEdit = (user: UserType) => {
    setEditingUser(user);
    setIsModalOpen(true);
  };

  const handleDelete = (id: string) => {
    if (confirm('Tem certeza que deseja excluir este usuário?')) {
      deleteMutation.mutate(id);
    }
  };

  const handleToggleStatus = (user: UserType) => {
    if (!user.isActive) {
      if (confirm('Ativar este usuário?')) {
        activateMutation.mutate(user.id);
      }
    } else {
      toast.error('Use a exclusão para desativar permanentemente');
    }
  };

  const openCreateModal = () => {
    setEditingUser(null);
    setIsModalOpen(true);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="page-title">Usuários</h1>
          <p className="page-subtitle">Gerencie os usuários do sistema</p>
        </div>
        <button onClick={openCreateModal} className="btn-primary">
          <Plus className="w-4 h-4 mr-2" />
          Novo Usuário
        </button>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="flex flex-col sm:flex-row gap-4">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type="text"
                placeholder="Buscar por nome, e-mail, CPF..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="input pl-10"
              />
            </div>
            <div className="relative">
              <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <select
                value={roleFilter}
                onChange={(e) => setRoleFilter(e.target.value as UserRole | 'ALL')}
                className="input pl-10 pr-10 appearance-none"
              >
                <option value="ALL">Todos os perfis</option>
                <option value="ADMIN">Administrador</option>
                <option value="COLLECTOR">Coletor</option>
                <option value="CLIENT">Cliente</option>
              </select>
            </div>
            <div className="relative">
              <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value as 'ALL' | 'ACTIVE' | 'INACTIVE')}
                className="input pl-10 pr-10 appearance-none"
              >
                <option value="ALL">Todos os status</option>
                <option value="ACTIVE">Ativos</option>
                <option value="INACTIVE">Inativos</option>
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
                    <th>E-mail</th>
                    <th>CPF</th>
                    <th>Perfil</th>
                    <th>Status</th>
                    <th>Telefone</th>
                    <th>Criado em</th>
                    <th className="text-right">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredUsers.length === 0 ? (
                    <tr>
                      <td colSpan={8} className="text-center py-12 text-gray-500">
                        <User className="w-12 h-12 mx-auto mb-4 text-gray-300" />
                        <p className="text-lg">Nenhum usuário encontrado</p>
                        <p className="text-sm">Tente ajustar os filtros ou cadastre um novo usuário</p>
                      </td>
                    </tr>
                  ) : (
                    filteredUsers.map((user: UserType) => (
                      <tr key={user.id}>
                        <td>
                          <div>
                            <p className="font-medium text-gray-900">{user.name}</p>
                          </div>
                        </td>
                        <td className="text-sm text-gray-600">{user.email}</td>
                        <td className="text-sm text-gray-600 font-mono">{user.cpf || user.cnpj || '-'}</td>
                        <td>
                          <span className={`badge ${
                            user.role === 'ADMIN' ? 'badge-purple' :
                            user.role === 'COLLECTOR' ? 'badge-green' : 'badge-gray'
                          }`}>
                            {USER_ROLE_LABELS[user.role as keyof typeof USER_ROLE_LABELS]}
                          </span>
                        </td>
                        <td>
                          <span className={`badge ${user.isActive ? 'badge-success' : 'badge-danger'}`}>
                            {user.isActive ? 'Ativo' : 'Inativo'}
                          </span>
                        </td>
                        <td className="text-sm text-gray-600">{user.phoneNumber || '-'}</td>
                        <td className="text-sm text-gray-600">
                          {user.createdAt ? format(new Date(user.createdAt), 'dd/MM/yyyy', { locale: ptBR }) : '-'}
                        </td>
                        <td className="text-right">
                          <div className="flex items-center justify-end space-x-2">
                            <button
                              onClick={() => handleEdit(user)}
                              className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100"
                              title="Editar"
                            >
                              <Edit className="w-5 h-5" />
                            </button>
                            {!user.isActive && (
                              <button
                                onClick={() => handleToggleStatus(user)}
                                className="p-2 rounded-lg text-green-500 hover:text-green-700 hover:bg-green-50"
                                title="Ativar"
                              >
                                <UserCheck className="w-5 h-5" />
                              </button>
                            )}
                            <button
                              onClick={() => handleDelete(user.id)}
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
                {editingUser ? 'Editar Usuário' : 'Novo Usuário'}
              </h3>
              <button
                onClick={() => setIsModalOpen(false)}
                className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100"
              >
                <XCircle className="w-5 h-5" />
              </button>
            </div>
            <UserForm
              initialData={editingUser}
              onSubmit={handleSubmit}
              onClose={() => {
                setIsModalOpen(false);
                setEditingUser(null);
              }}
              isLoading={createMutation.isPending || updateMutation.isPending}
            />
          </div>
        </div>
      )}
    </div>
  );
}

function UserForm({
  initialData,
  onSubmit,
  onClose,
  isLoading,
}: {
  initialData: UserType | null;
  onSubmit: (data: UserFormData) => void;
  onClose: () => void;
  isLoading: boolean;
}) {
  const { register, handleSubmit, setValue, formState: { errors } } = useForm<UserFormData>({
    resolver: zodResolver(userSchema),
    defaultValues: {
      name: '',
      email: '',
      cpf: '',
      phoneNumber: '',
      password: '',
      role: 'CLIENT',
    },
  });

  if (initialData) {
    setValue('name', initialData.name);
    setValue('email', initialData.email);
    setValue('cpf', initialData.cpf || initialData.cnpj || '');
    setValue('phoneNumber', initialData.phoneNumber || '');
    setValue('role', initialData.role);
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="modal-body space-y-4 p-6 max-h-[70vh] overflow-y-auto">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="md:col-span-2">
          <label className="label">Nome Completo *</label>
          <input {...register('name')} className={`input ${errors.name ? 'input-error' : ''}`} placeholder="Nome completo" />
          {errors.name && <p className="error-message">{errors.name.message}</p>}
        </div>

        <div>
          <label className="label">E-mail *</label>
          <input {...register('email')} type="email" className={`input ${errors.email ? 'input-error' : ''}`} placeholder="usuario@email.com" />
          {errors.email && <p className="error-message">{errors.email.message}</p>}
        </div>

        <div>
          <label className="label">CPF *</label>
          <input {...register('cpf')} className={`input ${errors.cpf ? 'input-error' : ''}`} placeholder="000.000.000-00" maxLength={14} />
          {errors.cpf && <p className="error-message">{errors.cpf.message}</p>}
        </div>

        <div>
          <label className="label">Telefone</label>
          <input {...register('phoneNumber')} className="input" placeholder="(11) 99999-9999" />
        </div>

        <div>
          <label className="label">{initialData ? 'Nova Senha (opcional)' : 'Senha *'} </label>
          <input
            type="password"
            {...register('password')}
            className={`input ${errors.password ? 'input-error' : ''}`}
            placeholder={initialData ? 'Deixe em branco para não alterar' : 'Mínimo 6 caracteres'}
          />
          {errors.password && <p className="error-message">{errors.password.message}</p>}
        </div>

        <div>
          <label className="label">Perfil *</label>
          <select {...register('role')} className={`input ${errors.role ? 'input-error' : ''}`}>
            <option value="CLIENT">Cliente</option>
            <option value="COLLECTOR">Coletor</option>
            <option value="ADMIN">Administrador</option>
          </select>
          {errors.role && <p className="error-message">{errors.role.message}</p>}
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