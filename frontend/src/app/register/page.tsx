'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '@/contexts/AuthContext';
import toast from 'react-hot-toast';
import { MapPin, Mail, Lock, User, Phone, Eye, EyeOff, Building2, Truck, Check } from 'lucide-react';

type AccountType = 'ESTABLISHMENT' | 'COLLECTOR';

const baseSchema = z.object({
  name: z.string().min(2, 'Nome deve ter pelo menos 2 caracteres').max(100),
  email: z.string().email('E-mail inválido'),
  password: z.string().min(8, 'Senha deve ter pelo menos 8 caracteres')
    .regex(/[A-Z]/, 'Senha deve conter pelo menos uma letra maiúscula')
    .regex(/[a-z]/, 'Senha deve conter pelo menos uma letra minúscula')
    .regex(/[0-9]/, 'Senha deve conter pelo menos um número')
    .regex(/[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]/, 'Senha deve conter pelo menos um caractere especial'),
  confirmPassword: z.string(),
  phoneNumber: z.string().optional(),
});

const collectorSchema = baseSchema.extend({
  cpf: z.string().refine(
    (val) => {
      const clean = val.replace(/\D/g, '');
      return /^\d{11}$/.test(clean);
    },
    { message: 'CPF deve conter exatamente 11 dígitos' }
  ),
  cnpj: z.string().optional(),
  establishmentName: z.string().optional(),
  establishmentAddress: z.string().optional(),
  establishmentCity: z.string().optional(),
  establishmentState: z.string().optional(),
  establishmentZipCode: z.string().optional(),
}).refine((data) => data.password === data.confirmPassword, {
  message: 'As senhas não coincidem',
  path: ['confirmPassword'],
});

const establishmentSchema = baseSchema.extend({
  cnpj: z.string().refine(
    (val) => {
      const clean = val.replace(/[^A-Za-z0-9]/g, '').toUpperCase();
      return /^[A-Z0-9]{12}[0-9]{2}$/.test(clean);
    },
    { message: 'CNPJ deve conter 12 caracteres alfanuméricos seguidos de 2 dígitos numéricos' }
  ),
  establishmentName: z.string().min(1, 'Nome do estabelecimento é obrigatório'),
  establishmentAddress: z.string().min(1, 'Endereço do estabelecimento é obrigatório'),
  establishmentCity: z.string().optional(),
  establishmentState: z.string().optional(),
  establishmentZipCode: z.string().optional(),
  cpf: z.string().optional(),
}).refine((data) => data.password === data.confirmPassword, {
  message: 'As senhas não coincidem',
  path: ['confirmPassword'],
});

const schemaMap: Record<AccountType, z.ZodSchema> = {
  COLLECTOR: collectorSchema,
  ESTABLISHMENT: establishmentSchema,
};

type CollectorForm = z.infer<typeof collectorSchema>;
type EstablishmentForm = z.infer<typeof establishmentSchema>;
type RegisterForm = CollectorForm | EstablishmentForm;

const ERROR_MESSAGES: Record<string, string> = {
  EMAIL_ALREADY_EXISTS: 'Este e-mail já está cadastrado',
  CPF_ALREADY_EXISTS: 'Este CPF já está cadastrado',
  CNPJ_ALREADY_EXISTS: 'Este CNPJ já está cadastrado',
  INVALID_CPF: 'CPF inválido. Use apenas 11 dígitos',
  INVALID_CNPJ: 'CNPJ inválido. Use apenas 14 dígitos',
  CNPJ_REQUIRED: 'CNPJ é obrigatório para estabelecimento',
  CPF_REQUIRED: 'CPF é obrigatório para coletor',
  ESTABLISHMENT_NAME_REQUIRED: 'Nome do estabelecimento é obrigatório',
  ESTABLISHMENT_ADDRESS_REQUIRED: 'Endereço do estabelecimento é obrigatório',
  WEAK_PASSWORD: 'Senha fraca. Use pelo menos 8 caracteres, 1 maiúscula, 1 número e 1 caractere especial',
  INVALID_ROLE: 'Tipo de conta inválido',
};

export default function RegisterPage() {
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [accountType, setAccountType] = useState<AccountType>('COLLECTOR');
  const { register: registerUser } = useAuth();
  const router = useRouter();

  const currentSchema = schemaMap[accountType];

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
    setValue,
    reset,
  } = useForm<RegisterForm>({
    resolver: zodResolver(currentSchema),
    defaultValues: {
      phoneNumber: '',
      cpf: '',
      cnpj: '',
      establishmentName: '',
      establishmentAddress: '',
      establishmentCity: '',
      establishmentState: '',
      establishmentZipCode: '',
    },
  });

  const password = watch('password') || '';

  const handleAccountTypeChange = (type: AccountType) => {
    setAccountType(type);
    reset({
      phoneNumber: '',
      cpf: '',
      cnpj: '',
      establishmentName: '',
      establishmentAddress: '',
      establishmentCity: '',
      establishmentState: '',
      establishmentZipCode: '',
      name: watch('name') || '',
      email: watch('email') || '',
      password: '',
      confirmPassword: '',
    });
  };

  const onSubmit = async (data: RegisterForm) => {
    setIsLoading(true);
    try {
      const cleanPhoneNumber = data.phoneNumber ? data.phoneNumber.replace(/\D/g, '') : undefined;
      const cleanCpf = data.cpf ? data.cpf.replace(/\D/g, '') : undefined;
      // For CNPJ, keep letters and numbers, convert to uppercase (alphanumeric CNPJ support)
      const cleanCnpj = data.cnpj ? data.cnpj.replace(/[^A-Za-z0-9]/g, '').toUpperCase() : undefined;
      const cleanZipCode = data.establishmentZipCode ? data.establishmentZipCode.replace(/\D/g, '') : undefined;
      const cleanState = data.establishmentState ? data.establishmentState.toUpperCase() : undefined;

      const payload = {
        name: data.name,
        email: data.email,
        password: data.password,
        phoneNumber: cleanPhoneNumber,
        role: accountType,
        ...(accountType === 'COLLECTOR' && { cpf: cleanCpf }),
        ...(accountType === 'ESTABLISHMENT' && {
          cnpj: cleanCnpj,
          establishmentName: data.establishmentName,
          establishmentAddress: data.establishmentAddress,
          establishmentCity: data.establishmentCity,
          establishmentState: cleanState,
          establishmentZipCode: cleanZipCode,
        }),
      };

      await registerUser(payload);
      toast.success('Conta criada com sucesso!');
      router.push('/dashboard');
      router.refresh();
    } catch (error: any) {
      const errorCode = error.response?.data?.errorCode || error.response?.data?.error;
      const message = errorCode && ERROR_MESSAGES[errorCode]
        ? ERROR_MESSAGES[errorCode]
        : error.response?.data?.message || 'Erro ao criar conta. Tente novamente.';
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  const getPasswordRequirements = () => [
    { label: 'Mínimo 8 caracteres', met: password.length >= 8 },
    { label: 'Uma letra maiúscula', met: /[A-Z]/.test(password) },
    { label: 'Uma letra minúscula', met: /[a-z]/.test(password) },
    { label: 'Um número', met: /[0-9]/.test(password) },
    { label: 'Um caractere especial', met: /[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]/.test(password) },
  ];

  const renderFields = () => {
    if (accountType === 'COLLECTOR') {
      return (
        <>
          <div>
            <label htmlFor="cpf" className="label">
              CPF
            </label>
            <input
              {...register('cpf')}
              id="cpf"
              type="text"
              maxLength={14}
              className={`input ${errors.cpf ? 'input-error' : ''}`}
              placeholder="000.000.000-00"
              disabled={isLoading}
              onChange={(e) => {
                const value = e.target.value.replace(/\D/g, '');
                const formatted = value
                  .replace(/(\d{3})(\d)/, '$1.$2')
                  .replace(/(\d{3})(\d)/, '$1.$2')
                  .replace(/(\d{3})(\d{1,2})$/, '$1-$2');
                setValue('cpf', formatted);
              }}
            />
            {errors.cpf && <p className="error-message">{errors.cpf.message}</p>}
          </div>
        </>
      );
    }

    return (
      <>
        <div>
          <label htmlFor="cnpj" className="label">
            CNPJ
          </label>
          <input
            {...register('cnpj')}
            id="cnpj"
            type="text"
            maxLength={18}
            className={`input ${errors.cnpj ? 'input-error' : ''}`}
            placeholder="00.000.000/0000-00"
            disabled={isLoading}
            onChange={(e) => {
              // Accept letters and numbers, convert letters to uppercase
              const value = e.target.value.replace(/[^A-Za-z0-9]/g, '').toUpperCase();
              const formatted = value
                .replace(/([A-Z0-9]{2})([A-Z0-9])/, '$1.$2')
                .replace(/([A-Z0-9]{3})([A-Z0-9])/, '$1.$2')
                .replace(/([A-Z0-9]{3})([A-Z0-9])/, '$1/$2')
                .replace(/([A-Z0-9]{4})([A-Z0-9]{1,2})$/, '$1-$2');
              setValue('cnpj', formatted);
            }}
          />
          {errors.cnpj && <p className="error-message">{errors.cnpj.message}</p>}
        </div>

        <div>
          <label htmlFor="establishmentName" className="label">
            Nome do estabelecimento
          </label>
          <div className="relative">
            <Building2 className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
            <input
              {...register('establishmentName')}
              id="establishmentName"
              type="text"
              autoComplete="organization"
              className={`input pl-10 ${errors.establishmentName ? 'input-error' : ''}`}
              placeholder="Meu Restaurante"
              disabled={isLoading}
            />
          </div>
          {errors.establishmentName && <p className="error-message">{errors.establishmentName.message}</p>}
        </div>

        <div>
          <label htmlFor="establishmentAddress" className="label">
            Endereço do estabelecimento
          </label>
          <div className="relative">
            <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
            <input
              {...register('establishmentAddress')}
              id="establishmentAddress"
              type="text"
              autoComplete="street-address"
              className={`input pl-10 ${errors.establishmentAddress ? 'input-error' : ''}`}
              placeholder="Rua das Flores, 123"
              disabled={isLoading}
            />
          </div>
          {errors.establishmentAddress && <p className="error-message">{errors.establishmentAddress.message}</p>}
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="establishmentCity" className="label">
              Cidade
            </label>
            <input
              {...register('establishmentCity')}
              id="establishmentCity"
              type="text"
              className={`input ${errors.establishmentCity ? 'input-error' : ''}`}
              placeholder="São Paulo"
              disabled={isLoading}
            />
          </div>
          <div>
            <label htmlFor="establishmentState" className="label">
              Estado
            </label>
            <input
              {...register('establishmentState')}
              id="establishmentState"
              type="text"
              maxLength={2}
              className={`input ${errors.establishmentState ? 'input-error' : ''}`}
              placeholder="SP"
              disabled={isLoading}
            />
          </div>
        </div>

        <div>
          <label htmlFor="establishmentZipCode" className="label">
            CEP
          </label>
          <input
            {...register('establishmentZipCode')}
            id="establishmentZipCode"
            type="text"
            maxLength={9}
            className={`input ${errors.establishmentZipCode ? 'input-error' : ''}`}
            placeholder="00000-000"
            disabled={isLoading}
            onChange={(e) => {
              const value = e.target.value.replace(/\D/g, '');
              const formatted = value.replace(/(\d{5})(\d)/, '$1-$2');
              setValue('establishmentZipCode', formatted);
            }}
          />
        </div>
      </>
    );
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4 py-12">
      <div className="w-full max-w-2xl">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-primary-600 rounded-2xl mb-4">
            <MapPin className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-3xl font-bold text-gray-900">Sistema de Coleta de Óleo</h1>
          <p className="text-gray-500 mt-2">Crie sua conta para começar</p>
        </div>

        <div className="card">
          <div className="card-body">
            {/* Account Type Selection */}
            <div className="mb-6">
              <p className="text-sm text-gray-600 mb-3">Que tipo de conta você deseja criar?</p>
              <div className="grid grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => handleAccountTypeChange('COLLECTOR')}
                  className={`relative p-4 border-2 rounded-xl text-left transition-all ${
                    accountType === 'COLLECTOR'
                      ? 'border-primary-500 bg-primary-50'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <div className="flex items-center space-x-3">
                    <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${
                      accountType === 'COLLECTOR' ? 'bg-primary-600' : 'bg-gray-100'
                    }`}>
                      <Truck className={`w-5 h-5 ${accountType === 'COLLECTOR' ? 'text-white' : 'text-gray-600'}`} />
                    </div>
                    <div>
                      <p className="font-medium text-gray-900">Sou coletor</p>
                      <p className="text-xs text-gray-500">Coleto óleo usado em estabelecimentos</p>
                    </div>
                  </div>
                  {accountType === 'COLLECTOR' && (
                    <Check className="absolute top-2 right-2 w-5 h-5 text-primary-600" />
                  )}
                </button>

                <button
                  type="button"
                  onClick={() => handleAccountTypeChange('ESTABLISHMENT')}
                  className={`relative p-4 border-2 rounded-xl text-left transition-all ${
                    accountType === 'ESTABLISHMENT'
                      ? 'border-primary-500 bg-primary-50'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <div className="flex items-center space-x-3">
                    <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${
                      accountType === 'ESTABLISHMENT' ? 'bg-primary-600' : 'bg-gray-100'
                    }`}>
                      <Building2 className={`w-5 h-5 ${accountType === 'ESTABLISHMENT' ? 'text-white' : 'text-gray-600'}`} />
                    </div>
                    <div>
                      <p className="font-medium text-gray-900">Sou estabelecimento</p>
                      <p className="text-xs text-gray-500">Tenho um local que gera óleo usado</p>
                    </div>
                  </div>
                  {accountType === 'ESTABLISHMENT' && (
                    <Check className="absolute top-2 right-2 w-5 h-5 text-primary-600" />
                  )}
                </button>
              </div>
            </div>

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
                    onChange={(e) => {
                      const value = e.target.value.replace(/\D/g, '');
                      const formatted = value
                        .replace(/(\d{2})(\d)/, '($1) $2')
                        .replace(/(\d{5})(\d{1,4})$/, '$1-$2');
                      setValue('phoneNumber', formatted);
                    }}
                  />
                </div>
              </div>

              {renderFields()}

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

                {/* Password requirements indicator */}
                <div className="mt-2 space-y-1">
                  {getPasswordRequirements().map((req, i) => (
                    <div key={i} className="flex items-center space-x-2 text-xs">
                      <span className={`w-4 h-4 rounded-full border-2 flex items-center justify-center ${
                        req.met ? 'text-green-600 border-green-600' : 'text-gray-300 border-gray-300'
                      }`}>
                        {req.met && <Check className="w-3 h-3" />}
                      </span>
                      <span className={`text-gray-600 ${req.met ? 'text-green-600' : ''}`}>{req.label}</span>
                    </div>
                  ))}
                </div>
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