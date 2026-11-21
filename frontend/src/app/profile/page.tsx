'use client';

import { useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { authApi } from '@/lib/api';
import { User as UserType, UserRole } from '@/types';
import toast from 'react-hot-toast';
import {
  User,
  Mail,
  Phone,
  Shield,
  Lock,
  Eye,
  EyeOff,
  Save,
  AlertCircle,
  CheckCircle,
  Trash2,
  X,
  AlertTriangle,
} from 'lucide-react';
import { USER_ROLE_LABELS } from '@/types';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

interface ProfileFormData {
  name: string;
  email: string;
  phoneNumber?: string;
}

interface PasswordFormData {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

const profileSchema = z.object({
  name: z.string().min(2, 'Nome deve ter pelo menos 2 caracteres').max(100),
  email: z.string().email('E-mail inválido'),
  phoneNumber: z.string().optional(),
});

const passwordSchema = z.object({
  currentPassword: z.string().min(1, 'Senha atual é obrigatória'),
  newPassword: z.string().min(6, 'Nova senha deve ter pelo menos 6 caracteres'),
  confirmPassword: z.string().min(1, 'Confirmação é obrigatória'),
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: 'As senhas não coincidem',
  path: ['confirmPassword'],
});

export default function ProfilePage() {
  const { user, updateUser, deleteAccount, logout } = useAuth();
  const [activeTab, setActiveTab] = useState<'profile' | 'password'>('profile');
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [deleteConfirmation, setDeleteConfirmation] = useState('');

  const handleDeleteAccount = async () => {
    if (deleteConfirmation !== 'EXCLUIR') return;
    try {
      await deleteAccount();
      // logout is called inside deleteAccount, which clears tokens and redirects
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Erro ao excluir conta');
    }
  };

  const profileForm = useForm<ProfileFormData>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      name: user?.name || '',
      email: user?.email || '',
      phoneNumber: user?.phoneNumber || '',
    },
  });

  const passwordForm = useForm<PasswordFormData>({
    resolver: zodResolver(passwordSchema),
    defaultValues: {
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    },
  });

  const handleProfileSubmit = async (data: ProfileFormData) => {
    try {
      const response = await authApi.updateProfile(data);
      updateUser(response.data);
      toast.success('Perfil atualizado com sucesso!');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Erro ao atualizar perfil');
    }
  };

  const handlePasswordSubmit = async (data: PasswordFormData) => {
    try {
      await authApi.changePassword(data.currentPassword, data.newPassword);
      toast.success('Senha alterada com sucesso!');
      passwordForm.reset();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Erro ao alterar senha');
    }
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div>
        <h1 className="page-title">Meu Perfil</h1>
        <p className="page-subtitle">Gerencie suas informações pessoais e segurança</p>
      </div>

      <div className="card">
        <div className="card-header border-b border-gray-200">
          <div className="flex items-center space-x-4 border-b border-gray-200 pb-4">
            <div className="w-20 h-20 rounded-full bg-primary-100 flex items-center justify-center">
              <User className="w-10 h-10 text-primary-600" />
            </div>
            <div>
              <h3 className="text-xl font-semibold text-gray-900">{user?.name}</h3>
              <p className="text-gray-500">{user?.email}</p>
              <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium mt-1 ${
                user?.role === 'ADMIN' ? 'bg-purple-100 text-purple-800' :
                user?.role === 'COLLECTOR' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
              }`}>
                {USER_ROLE_LABELS[user?.role as UserRole]}
              </span>
            </div>
          </div>
          <div className="flex space-x-6 mt-4">
            <button
              onClick={() => setActiveTab('profile')}
              className={`flex items-center space-x-2 py-2 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'profile'
                  ? 'border-primary-600 text-primary-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              <User className="w-4 h-4" />
              <span>Perfil</span>
            </button>
            <button
              onClick={() => setActiveTab('password')}
              className={`flex items-center space-x-2 py-2 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'password'
                  ? 'border-primary-600 text-primary-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              <Lock className="w-4 h-4" />
              <span>Segurança</span>
            </button>
          </div>
        </div>
        <div className="card-body">
          {activeTab === 'profile' && (
            <form onSubmit={profileForm.handleSubmit(handleProfileSubmit)} className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="md:col-span-2">
                  <label className="label">Nome Completo *</label>
                  <div className="relative">
                    <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                    <input
                      {...profileForm.register('name')}
                      className={`input pl-10 ${profileForm.formState.errors.name ? 'input-error' : ''}`}
                    />
                  </div>
                  {profileForm.formState.errors.name && (
                    <p className="error-message">{profileForm.formState.errors.name.message}</p>
                  )}
                </div>

                <div>
                  <label className="label">E-mail *</label>
                  <div className="relative">
                    <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                    <input
                      type="email"
                      {...profileForm.register('email')}
                      className={`input pl-10 ${profileForm.formState.errors.email ? 'input-error' : ''}`}
                    />
                  </div>
                  {profileForm.formState.errors.email && (
                    <p className="error-message">{profileForm.formState.errors.email.message}</p>
                  )}
                </div>

                <div>
                  <label className="label">Telefone</label>
                  <div className="relative">
                    <Phone className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                    <input
                      {...profileForm.register('phoneNumber')}
                      className="input pl-10"
                      placeholder="(11) 99999-9999"
                    />
                  </div>
                </div>

                <div>
                  <label className="label">{user?.role === 'ESTABLISHMENT' ? 'CNPJ' : 'CPF'}</label>
                  <div className="relative">
                    <Shield className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                    <input
                      value={user?.role === 'ESTABLISHMENT' ? user?.cnpj : user?.cpf}
                      className="input pl-10 bg-gray-50 cursor-not-allowed"
                      disabled
                    />
                  </div>
                  <p className="text-xs text-gray-500 mt-1">
                    {user?.role === 'ESTABLISHMENT' ? 'O CNPJ não pode ser alterado' : 'O CPF não pode ser alterado'}
                  </p>
                </div>

                <div className="md:col-span-2">
                  <label className="label">Perfil</label>
                  <div className="relative">
                    <Shield className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                    <select
                      value={user?.role}
                      className="input pl-10 bg-gray-50 cursor-not-allowed"
                      disabled
                    >
                      <option value="ADMIN">Administrador</option>
                      <option value="COLLECTOR">Coletor</option>
                      <option value="ESTABLISHMENT">Estabelecimento</option>
                      <option value="CLIENT">Cliente</option>
                    </select>
                  </div>
                  <p className="text-xs text-gray-500 mt-1">Entre em contato com o administrador para alterar seu perfil</p>
                </div>

                <div className="md:col-span-2">
                  <label className="label">Status</label>
                  <div className="flex items-center space-x-2">
                    <span className={`badge ${user?.isActive ? 'badge-success' : 'badge-danger'}`}>
                      {user?.isActive ? 'Ativo' : 'Inativo'}
                    </span>
                    <span className="text-sm text-gray-500">
                      {user?.createdAt && `Membro desde ${new Date(user.createdAt).toLocaleDateString('pt-BR')}`}
                    </span>
                  </div>
                </div>
              </div>

              <div className="flex justify-end pt-4 border-t border-gray-200">
                <button type="submit" className="btn-primary" disabled={profileForm.formState.isSubmitting}>
                  <Save className="w-4 h-4 mr-2" />
                  {profileForm.formState.isSubmitting ? 'Salvando...' : 'Salvar Alterações'}
                </button>
              </div>
            </form>
          )}

          {activeTab === 'password' && (
            <form onSubmit={passwordForm.handleSubmit(handlePasswordSubmit)} className="space-y-6 max-w-md">
              <div>
                <label className="label">Senha Atual *</label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <input
                    type={showCurrentPassword ? 'text' : 'password'}
                    {...passwordForm.register('currentPassword')}
                    className={`input pl-10 pr-10 ${passwordForm.formState.errors.currentPassword ? 'input-error' : ''}`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                  >
                    {showCurrentPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                  </button>
                </div>
                {passwordForm.formState.errors.currentPassword && (
                  <p className="error-message">{passwordForm.formState.errors.currentPassword.message}</p>
                )}
              </div>

              <div>
                <label className="label">Nova Senha *</label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <input
                    type={showNewPassword ? 'text' : 'password'}
                    {...passwordForm.register('newPassword')}
                    className={`input pl-10 pr-10 ${passwordForm.formState.errors.newPassword ? 'input-error' : ''}`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowNewPassword(!showNewPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                  >
                    {showNewPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                  </button>
                </div>
                {passwordForm.formState.errors.newPassword && (
                  <p className="error-message">{passwordForm.formState.errors.newPassword.message}</p>
                )}
              </div>

              <div>
                <label className="label">Confirmar Nova Senha *</label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <input
                    type={showConfirmPassword ? 'text' : 'password'}
                    {...passwordForm.register('confirmPassword')}
                    className={`input pl-10 pr-10 ${passwordForm.formState.errors.confirmPassword ? 'input-error' : ''}`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                  >
                    {showConfirmPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                  </button>
                </div>
                {passwordForm.formState.errors.confirmPassword && (
                  <p className="error-message">{passwordForm.formState.errors.confirmPassword.message}</p>
                )}
              </div>

              <div className="p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
                <div className="flex items-start space-x-3">
                  <AlertCircle className="w-5 h-5 text-yellow-600 mt-0.5" />
                  <div className="text-sm text-yellow-800">
                    <p className="font-medium">Dicas de segurança:</p>
                    <ul className="list-disc list-inside mt-2 space-y-1">
                      <li>Use pelo menos 8 caracteres</li>
                      <li>Combine letras maiúsculas, minúsculas, números e símbolos</li>
                      <li>Não use informações pessoais (nome, data de nascimento)</li>
                      <li>Não reutilize senhas de outros sites</li>
                    </ul>
                  </div>
                </div>
              </div>

              <div className="flex justify-end pt-4 border-t border-gray-200">
                <button type="submit" className="btn-primary" disabled={passwordForm.formState.isSubmitting}>
                  <Save className="w-4 h-4 mr-2" />
                  {passwordForm.formState.isSubmitting ? 'Alterando...' : 'Alterar Senha'}
                </button>
              </div>
            </form>
          )}
        </div>
      </div>

      {/* Zona de Perigo - Apenas para COLLECTOR e ESTABLISHMENT */}
      {(user?.role === 'COLLECTOR' || user?.role === 'ESTABLISHMENT') && (
        <div className="card border-red-200">
          <div className="card-header border-b border-red-200">
            <div className="flex items-center space-x-3">
              <AlertTriangle className="w-5 h-5 text-red-600" />
              <h3 className="text-lg font-semibold text-red-800">Zona de Perigo</h3>
            </div>
            <p className="text-sm text-red-600 mt-1">
              Ações irreversíveis. Tenha certeza antes de prosseguir.
            </p>
          </div>
          <div className="card-body">
            <div className="flex items-center justify-between p-4 bg-red-50 border border-red-200 rounded-lg">
              <div>
                <p className="font-medium text-red-900">Excluir minha conta</p>
                <p className="text-sm text-red-700 mt-1">
                  Esta ação desativará sua conta permanentemente. Seu e-mail e CPF/CNPJ permanecerão
                  ocupados no sistema e não poderão ser usados para novo cadastro. Seus dados de
                  coletas e agendamentos serão mantidos para histórico, mas você não poderá mais
                  acessar a plataforma.
                </p>
              </div>
              <button
                type="button"
                onClick={() => setIsDeleteModalOpen(true)}
                className="btn-danger flex-shrink-0"
              >
                <Trash2 className="w-4 h-4 mr-2" />
                Excluir conta
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal de confirmação de exclusão */}
      {isDeleteModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" onClick={() => setIsDeleteModalOpen(false)}>
          <div className="w-full max-w-md bg-white rounded-xl shadow-xl p-6" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center space-x-3 mb-4">
              <div className="w-10 h-10 rounded-full bg-red-100 flex items-center justify-center">
                <AlertTriangle className="w-5 h-5 text-red-600" />
              </div>
              <h3 className="text-lg font-semibold text-gray-900">Confirmar exclusão da conta</h3>
            </div>
            <p className="text-gray-600 mb-6">
              Esta ação é <strong>irreversível</strong>. Sua conta será desativada permanentemente e você
              não poderá mais acessar a plataforma Connect Oil.
            </p>
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
              <p className="text-sm text-red-800 font-medium mb-2">
                Para confirmar, digite exatamente: <code className="bg-red-100 px-1.5 py-0.5 rounded text-sm">EXCLUIR</code>
              </p>
              <input
                type="text"
                value={deleteConfirmation}
                onChange={(e) => setDeleteConfirmation(e.target.value.toUpperCase())}
                onKeyDown={(e) => e.stopPropagation()}
                className="input font-mono text-lg tracking-widest text-center"
                placeholder="DIGITE EXCLUIR"
                maxLength={7}
                autoFocus
              />
              <p className="text-xs text-red-600 mt-2 text-center">
                O botão será habilitado apenas quando o texto for exatamente EXCLUIR
              </p>
            </div>
            <div className="flex space-x-3">
              <button
                type="button"
                onClick={() => {
                  setIsDeleteModalOpen(false);
                  setDeleteConfirmation('');
                }}
                className="btn-secondary flex-1"
              >
                <X className="w-4 h-4 mr-2" />
                Cancelar
              </button>
              <button
                type="button"
                onClick={handleDeleteAccount}
                disabled={deleteConfirmation !== 'EXCLUIR'}
                className="btn-danger flex-1"
              >
                <Trash2 className="w-4 h-4 mr-2" />
                Excluir definitivamente
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}