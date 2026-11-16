'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '@/contexts/AuthContext';
import toast from 'react-hot-toast';
import { MapPin, Mail, Lock, User, Phone, Eye, EyeOff } from 'lucide-react';

const registerSchema = z.object({
  name: z.string().min(2, 'Nome deve ter pelo menos 2 caracteres').max(100),
  email: z.string().email('E-mail inválido'),
  password: z.string().min(6, 'Senha deve ter pelo menos 6 caracteres'),
  confirmPassword: z.string(),
  cpf: z.string().regex(/^\d{11}$/, 'CPF deve conter exatamente 11 dígitos'),
  phoneNumber: z.string().optional(),
}).refine((data) => data.password === data.confirmPassword, {
  message: 'As senhas não coincidem',
  path: ['confirmPassword'],
});

type RegisterForm = z.infer<typeof registerSchema>;

export default function RegisterPage() {
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const { register: registerUser } = useAuth();
  const router = useRouter();

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
  });

  const password = watch('password');

  const onSubmit = async (data: RegisterForm) => {
    setIsLoading(true);
    try {
      // Clean phone number: remove non-digits
      const cleanPhoneNumber = data.phoneNumber ? data.phoneNumber.replace(/\D/g, '') : undefined;

      await registerUser({
        name: data.name,
        email: data.email,
        password: data.password,
        cpf: data.cpf,
        phoneNumber: cleanPhoneNumber,
        role: 'COLLECTOR',
      });
      toast.success('Conta criada com sucesso!');
      router.push('/dashboard');
      router.refresh();
    } catch (error: any) {
      const message = error.response?.data?.message || 'Erro ao criar conta. Tente novamente.';
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4 py-12">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-primary-600 rounded-2xl mb-4">
            <MapPin className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-3xl font-bold text-gray-900">Sistema de Coleta de Óleo</h1>
          <p className="text-gray-500 mt-2">Crie sua conta para começar</p>
        </div>

        <div className="card">
          <div className="card-body">
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
              <div>
                <label htmlFor="name" className="label">
                  Nome completo
                </label>
                <div className="relative">
                  <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <input
                    {...register('name')}
                    id="name"
                    type="text"
                    autoComplete="name"
                    className={`input pl-10 ${errors.name ? 'input-error' : ''}`}
                    placeholder="João da Silva"
                    disabled={isLoading}
                  />
                </div>
                {errors.name && <p className="error-message">{errors.name.message}</p>}
              </div>

              <div>
                <label htmlFor="email" className="label">
                  E-mail
                </label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <input
                    {...register('email')}
                    id="email"
                    type="email"
                    autoComplete="email"
                    className={`input pl-10 ${errors.email ? 'input-error' : ''}`}
                    placeholder="seu@email.com"
                    disabled={isLoading}
                  />
                </div>
                {errors.email && <p className="error-message">{errors.email.message}</p>}
              </div>

              <div>
                <label htmlFor="cpf" className="label">
                  CPF
                </label>
                <input
                  {...register('cpf')}
                  id="cpf"
                  type="text"
                  maxLength={11}
                  className={`input ${errors.cpf ? 'input-error' : ''}`}
                  placeholder="Apenas números"
                  disabled={isLoading}
                />
                {errors.cpf && <p className="error-message">{errors.cpf.message}</p>}
              </div>

              <div>
                <label htmlFor="phoneNumber" className="label">
                  Telefone (opcional)
                </label>
                <div className="relative">
                  <Phone className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <input
                    {...register('phoneNumber')}
                    id="phoneNumber"
                    type="tel"
                    autoComplete="tel"
                    className={`input pl-10 ${errors.phoneNumber ? 'input-error' : ''}`}
                    placeholder="(11) 99999-9999"
                    disabled={isLoading}
                  />
                </div>
              </div>

              <div>
                <label htmlFor="password" className="label">
                  Senha
                </label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <input
                    {...register('password')}
                    id="password"
                    type={showPassword ? 'text' : 'password'}
                    autoComplete="new-password"
                    className={`input pl-10 pr-10 ${errors.password ? 'input-error' : ''}`}
                    placeholder="••••••••"
                    disabled={isLoading}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                  >
                    {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                  </button>
                </div>
                {errors.password && <p className="error-message">{errors.password.message}</p>}
              </div>

              <div>
                <label htmlFor="confirmPassword" className="label">
                  Confirmar senha
                </label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <input
                    {...register('confirmPassword')}
                    id="confirmPassword"
                    type={showPassword ? 'text' : 'password'}
                    autoComplete="new-password"
                    className={`input pl-10 ${errors.confirmPassword ? 'input-error' : ''}`}
                    placeholder="••••••••"
                    disabled={isLoading}
                  />
                </div>
                {errors.confirmPassword && <p className="error-message">{errors.confirmPassword.message}</p>}
              </div>

              <button
                type="submit"
                disabled={isLoading}
                className="btn-primary w-full py-3"
              >
                {isLoading ? (
                  <span className="flex items-center justify-center space-x-2">
                    <div className="spinner w-5 h-5"></div>
                    <span>Criando conta...</span>
                  </span>
                ) : (
                  'Criar conta'
                )}
              </button>
            </form>

            <div className="mt-6 text-center">
              <p className="text-gray-600">
                Já tem uma conta?{' '}
                <Link href="/login" className="text-primary-600 hover:text-primary-500 font-medium">
                  Faça login
                </Link>
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}