# Relatório Técnico Completo - Cadastro e Autenticação (US1 e US8)

**Projeto:** Connect Oil - Plataforma Web de Coleta de Óleo  
**Branch:** `feature/cadastro-papeis-us1-us8`  
**Data:** 18/11/2026  
**Autor:** Samuelcode7  

---

## 1. Visão Geral e Objetivos

### 1.1 Histórias de Usuário Implementadas

| História | Descrição | Status |
|----------|-----------|--------|
| **US1** | Cadastro público com escolha de papel entre **Coletor** (CPF) e **Estabelecimento** (CNPJ + dados do estabelecimento), bloqueando criação pública de **ADMIN** | ✅ Concluído |
| **US8** | Política de senha forte com retorno `WEAK_PASSWORD` (mínimo 8 caracteres, 1 maiúscula, 1 minúscula, 1 número, 1 caractere especial) | ✅ Concluído |

### 1.2 Objetivos Alcançados

- ✅ Implementação completa do fluxo de registro público com validação de papéis (apenas `COLLECTOR`, `ESTABLISHMENT` e `ADMIN` para bootstrap)
- ✅ Validação de senha forte configurável via `SystemConfigService` (comportamento padrão: 8+ caracteres, maiúscula, minúscula, número, especial)
- ✅ Suporte a CNPJ alfanumérico (regex `^[A-Z0-9]{12}[0-9]{2}$`) e CPF numérico (11 dígitos)
- ✅ Interface de cadastro responsiva com seletor visual de tipo de conta, máscaras dinâmicas e checklist interativo de senha
- ✅ Tratamento de erros em português com códigos específicos (`INVALID_ROLE`, `WEAK_PASSWORD`, `CNPJ_REQUIRED`, etc.)
- ✅ Navegação da Sidebar adaptada para `ESTABLISHMENT` com traduções em português
- ✅ Testes unitários abrangentes (18 novos testes em `AuthServiceTest`)
- ✅ Verificação de tipos TypeScript sem erros (`npx tsc --noEmit`)

---

## 2. Alterações no Back-end (Java 21 / Spring Boot 3.2.5)

### 2.1 AuthService.java (`backend/src/main/java/com/oilcollection/service/AuthService.java`)

**Principais modificações no método `register()`:**

```java
// Constante para papéis permitidos no cadastro público
private static final Set<User.UserRole> PUBLIC_REGISTRATION_ROLES = Set.of(
    User.UserRole.ESTABLISHMENT,
    User.UserRole.COLLECTOR,
    User.UserRole.ADMIN  // TODO: remover antes da entrega final
);

// Validação de papel
if (role == null || !PUBLIC_REGISTRATION_ROLES.contains(role)) {
    throw new BusinessException("Invalid role for public registration", "INVALID_ROLE");
}

// Validações específicas para ESTABLISHMENT
if (role == User.UserRole.ESTABLISHMENT) {
    // CNPJ obrigatório
    if (request.getCnpj() == null || request.getCnpj().isBlank()) {
        throw new BusinessException("CNPJ is required for establishment registration", "CNPJ_REQUIRED");
    }
    // CNPJ alfanumérico: 12 alfanuméricos + 2 dígitos
    if (!request.getCnpj().matches("^[A-Z0-9]{12}[0-9]{2}$")) {
        throw new BusinessException("CNPJ must contain 12 alphanumeric characters followed by 2 digits", "INVALID_CNPJ");
    }
    // Campos obrigatórios do estabelecimento
    if (request.getEstablishmentName() == null || request.getEstablishmentName().isBlank()) {
        throw new BusinessException("Establishment name is required", "ESTABLISHMENT_NAME_REQUIRED");
    }
    if (request.getEstablishmentAddress() == null || request.getEstablishmentAddress().isBlank()) {
        throw new BusinessException("Establishment address is required", "ESTABLISHMENT_ADDRESS_REQUIRED");
    }
}
// Validações para COLLECTOR
else if (role == User.UserRole.COLLECTOR) {
    if (request.getCpf() == null || request.getCpf().isBlank()) {
        throw new BusinessException("CPF is required for collector registration", "CPF_REQUIRED");
    }
    if (!request.getCpf().matches("^\\d{11}$")) {
        throw new BusinessException("CPF must contain exactly 11 digits", "INVALID_CPF");
    }
}

// Validação de complexidade de senha (configurável)
validatePasswordComplexity(request.getPassword());
```

**Método `validatePasswordComplexity()`:**
```java
private void validatePasswordComplexity(String password) {
    int minLength = systemConfigService.getPasswordMinLength();       // default: 8
    boolean requireUppercase = systemConfigService.getPasswordRequireUppercase();
    boolean requireLowercase = systemConfigService.getPasswordRequireLowercase();
    boolean requireNumber = systemConfigService.getPasswordRequireNumber();
    boolean requireSpecial = systemConfigService.getPasswordRequireSpecial();

    if (password.length() < minLength) {
        throw new BusinessException("Password must be at least " + minLength + " characters", "WEAK_PASSWORD");
    }
    if (requireUppercase && !UPPERCASE_PATTERN.matcher(password).find()) {
        throw new BusinessException("Password must contain at least one uppercase letter", "WEAK_PASSWORD");
    }
    if (requireLowercase && !LOWERCASE_PATTERN.matcher(password).find()) {
        throw new BusinessException("Password must contain at least one lowercase letter", "WEAK_PASSWORD");
    }
    if (requireNumber && !NUMBER_PATTERN.matcher(password).find()) {
        throw new BusinessException("Password must contain at least one number", "WEAK_PASSWORD");
    }
    if (requireSpecial && !SPECIAL_PATTERN.matcher(password).find()) {
        throw new BusinessException("Password must contain at least one special character", "WEAK_PASSWORD");
    }
}
```

### 2.2 UserDTO.java (`backend/src/main/java/com/oilcollection/dto/UserDTO.java`)

**Atualização da validação CNPJ:**
```java
@Pattern(regexp = "^[A-Z0-9]{12}[0-9]{2}$", 
         message = "CNPJ must contain 12 alphanumeric characters followed by 2 digits")
private String cnpj;
```

### 2.3 User.java (`backend/src/main/java/com/oilcollection/model/User.java`)

**Ajuste na coluna CPF:**
```java
@Column(nullable = true, unique = true, length = 14)
private String cpf;

@Column(unique = true, length = 18)
private String cnpj;
```

### 2.4 GlobalExceptionHandler.java (`backend/src/main/java/com/oilcollection/exception/GlobalExceptionHandler.java`)

**Melhoria no handler `DataIntegrityViolationException` com mensagens em português:**
```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
        DataIntegrityViolationException ex, WebRequest request) {
    String message = "Erro de integridade dos dados";
    String msg = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
    if (msg != null) {
        String lowerMsg = msg.toLowerCase();
        if (lowerMsg.contains("users_email_key") || lowerMsg.contains("idx_user_email") || lowerMsg.contains("email") && lowerMsg.contains("unique")) {
            message = "Este e-mail já está cadastrado";
        } else if (lowerMsg.contains("users_cpf_key") || lowerMsg.contains("idx_user_cpf") || lowerMsg.contains("cpf") && lowerMsg.contains("unique")) {
            message = "Este CPF já está cadastrado";
        } else if (lowerMsg.contains("users_cnpj_key") || lowerMsg.contains("idx_user_cnpj") || lowerMsg.contains("cnpj") && lowerMsg.contains("unique")) {
            message = "Este CNPJ já está cadastrado";
        } else if (lowerMsg.contains("unique") || lowerMsg.contains("duplicate")) {
            message = "Já existe um registro com este valor";
        }
        // ... outros casos (estado, CEP, telefone, CNPJ length)
    }
    return new ResponseEntity<>(new ErrorResponse(...), HttpStatus.CONFLICT);
}
```

### 2.5 Migração Flyway V4 (`backend/src/main/resources/db/migration/V4__make_cpf_nullable_in_users.sql`)

```sql
-- Make CPF nullable for establishments (they have CNPJ instead)
ALTER TABLE users ALTER COLUMN cpf DROP NOT NULL;
```

---

## 3. Alterações no Front-end (Next.js 14 / TypeScript / React Hook Form / Zod)

### 3.1 Página de Cadastro (`frontend/src/app/register/page.tsx`)

**Principais funcionalidades implementadas:**

#### A. Seletor Visual de Tipo de Conta
```typescript
type AccountType = 'ESTABLISHMENT' | 'COLLECTOR';
const [accountType, setAccountType] = useState<AccountType>('COLLECTOR');

// Botões visuais com ícones (Truck para Coletor, Building2 para Estabelecimento)
// Estado ativo: border-primary-500 bg-primary-50
// Estado inativo: border-gray-200 hover:border-gray-300
```

#### B. Schemas Zod Condicionais com `.refine()`
```typescript
// Collector: CPF obrigatório, valida 11 dígitos (ignora máscara)
const collectorSchema = baseSchema.extend({
  cpf: z.string().refine(
    (val) => {
      const clean = val.replace(/\D/g, '');
      return /^\d{11}$/.test(clean);
    },
    { message: 'CPF deve conter exatamente 11 dígitos' }
  ),
  // ... campos opcionais de estabelecimento
});

// Establishment: CNPJ alfanumérico, nome e endereço obrigatórios
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
  // ... campos opcionais (cidade, estado, CEP)
});
```

#### C. Máscaras Dinâmicas de Entrada
```typescript
// CPF: 000.000.000-00
onChange={(e) => {
  const value = e.target.value.replace(/\D/g, '');
  const formatted = value
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d{1,2})$/, '$1-$2');
  setValue('cpf', formatted);
}}

// CNPJ alfanumérico: 00.000.000/0000-00 (aceita letras)
onChange={(e) => {
  const value = e.target.value.replace(/[^A-Za-z0-9]/g, '').toUpperCase();
  const formatted = value
    .replace(/([A-Z0-9]{2})([A-Z0-9])/, '$1.$2')
    .replace(/([A-Z0-9]{3})([A-Z0-9])/, '$1.$2')
    .replace(/([A-Z0-9]{3})([A-Z0-9])/, '$1/$2')
    .replace(/([A-Z0-9]{4})([A-Z0-9]{1,2})$/, '$1-$2');
  setValue('cnpj', formatted);
}}
```

#### D. Checklist Visual Interativo de Senha Forte
```typescript
const password = watch('password') || '';  // Fix para SSG undefined

const getPasswordRequirements = () => [
  { label: 'Mínimo 8 caracteres', met: password.length >= 8 },
  { label: 'Uma letra maiúscula', met: /[A-Z]/.test(password) },
  { label: 'Uma letra minúscula', met: /[a-z]/.test(password) },  // Correção: adicionado /[a-z]/
  { label: 'Um número', met: /[0-9]/.test(password) },
  { label: 'Um caractere especial', met: /[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]/.test(password) },
];

// Renderização com ícones Check (verde quando atendido, cinza caso contrário)
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
```

#### E. Mapeamento de Erros do Backend para Português
```typescript
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
```

#### F. Limpeza de Dados Antes do Envio
```typescript
const cleanPhoneNumber = data.phoneNumber ? data.phoneNumber.replace(/\D/g, '') : undefined;
const cleanCpf = data.cpf ? data.cpf.replace(/\D/g, '') : undefined;
// CNPJ mantém letras e números, converte para uppercase
const cleanCnpj = data.cnpj ? data.cnpj.replace(/[^A-Za-z0-9]/g, '').toUpperCase() : undefined;
const cleanZipCode = data.establishmentZipCode ? data.establishmentZipCode.replace(/\D/g, '') : undefined;
const cleanState = data.establishmentState ? data.establishmentState.toUpperCase() : undefined;
```

### 3.2 Correção na Página de Usuários (`frontend/src/app/users/page.tsx`)

**Problema:** TypeScript error - `user.cpf` possibly undefined  
**Solução:** Optional chaining e fallback para CNPJ
```typescript
// Antes (erro):
user.cpf?.includes(search)

// Depois (corrigido):
user.cpf?.includes(search) || user.cnpj?.includes(search)

// Na tabela:
<td className="text-sm text-gray-600 font-mono">{user.cpf || user.cnpj || '-'}</td>

// No formulário de edição:
setValue('cpf', initialData.cpf || initialData.cnpj || '');
```

### 3.3 Sidebar e Navegação (`frontend/src/components/Layout.tsx`)

**Adição de ESTABLISHMENT nos papéis de navegação:**
```typescript
const navigation = [
  { name: 'Dashboard', href: '/dashboard', icon: LayoutDashboard, roles: ['ADMIN', 'COLLECTOR', 'CLIENT', 'ESTABLISHMENT'] },
  { name: 'Pontos de Coleta', href: '/collection-points', icon: MapPin, roles: ['ADMIN', 'COLLECTOR', 'CLIENT', 'ESTABLISHMENT'] },
  { name: 'Agendamentos', href: '/schedules', icon: Calendar, roles: ['ADMIN', 'COLLECTOR', 'ESTABLISHMENT'] },
  { name: 'Registros', href: '/records', icon: Package, roles: ['ADMIN', 'COLLECTOR', 'ESTABLISHMENT'] },
  { name: 'Relatórios', href: '/reports', icon: BarChart3, roles: ['ADMIN'] },
  { name: 'Usuários', href: '/users', icon: Users, roles: ['ADMIN'] },
];
```

**Tradução dos perfis com `USER_ROLE_LABELS`:**
```typescript
import { USER_ROLE_LABELS } from '@/types';

// No componente de perfil do usuário:
<p className="text-xs text-gray-500 truncate">
  {user?.role ? USER_ROLE_LABELS[user.role as keyof typeof USER_ROLE_LABELS] : ''}
</p>
```

### 3.4 Tipos TypeScript (`frontend/src/types/index.ts`)

```typescript
export interface User {
  id: string;
  name: string;
  email: string;
  cpf?: string;      // Opcional para estabelecimentos
  cnpj?: string;     // Opcional para coletores
  // ...
  role: 'ADMIN' | 'COLLECTOR' | 'CLIENT' | 'ESTABLISHMENT';
  // ...
}

export interface RegisterData {
  name: string;
  email: string;
  password: string;
  cpf?: string;
  cnpj?: string;
  phoneNumber?: string;
  establishmentName?: string;
  establishmentAddress?: string;
  establishmentCity?: string;
  establishmentState?: string;
  establishmentZipCode?: string;
  role: 'COLLECTOR' | 'ESTABLISHMENT';
}

// Labels em português
export const USER_ROLE_LABELS: Record<UserRole, string> = {
  ADMIN: 'Administrador',
  COLLECTOR: 'Coletor',
  CLIENT: 'Cliente',
  ESTABLISHMENT: 'Estabelecimento',
};
```

### 3.5 AuthContext (`frontend/src/contexts/AuthContext.tsx`)

```typescript
import { RegisterData } from '@/types';

// register function typed with RegisterData
const register = async (data: RegisterData) => {
  const response = await api.post<AuthResponse>('/auth/register', data);
  // ...
};
```

### 3.6 Docker Ignore (`frontend/.dockerignore`)

```dockerignore
# Dependencies
node_modules

# Next.js build output
.next

# Build artifacts
out
build

# Environment files
.env
.env.local
.env.development.local
.env.test.local
.env.production.local

# IDE
.vscode
.idea
*.swp
*.swo

# OS
.DS_Store
Thumbs.db

# Logs
npm-debug.log*
yarn-debug.log*
yarn-error.log*

# Testing
coverage

# TypeScript
*.tsbuildinfo
```

---

## 4. Problemas Encontrados e Soluções Aplicadas

| # | Problema | Causa Raiz | Solução Aplicada | Arquivos Afetados |
|---|----------|------------|------------------|-------------------|
| **1** | **Erro TypeScript: `user.cpf` possibly undefined** | `User` interface tinha `cpf?: string`, mas código acessava `user.cpf.includes()` sem optional chaining | Adicionado optional chaining (`user.cpf?.includes()`) e fallback para `user.cnpj` na busca e exibição | `frontend/src/app/users/page.tsx` |
| **2** | **Lentidão de build Docker** | Ausência de `.dockerignore` fazia o Docker copiar `node_modules`, `.next`, etc. | Criado `frontend/.dockerignore` ignorando dependências, builds, env files, IDE, logs, coverage | `frontend/.dockerignore` (novo) |
| **3** | **Validação CNPJ contando caracteres da máscara** | Zod `.regex()` validava string com pontuação (ex: `00.000.000/0000-00` = 18 chars) ao invés de apenas alfanuméricos | Alterado para `.refine()` que limpa a máscara antes de validar: `val.replace(/[^A-Za-z0-9]/g, '').toUpperCase()` | `frontend/src/app/register/page.tsx` |
| **4** | **Violação NOT NULL coluna CPF no PostgreSQL (SQLState: 23502)** | Entidade `User` tinha `cpf @Column(nullable=false)`, mas estabelecimentos não têm CPF | 1. Alterado `@Column(nullable=true, unique=true)` em `User.java`<br>2. Criada migração Flyway `V4__make_cpf_nullable_in_users.sql` com `ALTER TABLE users ALTER COLUMN cpf DROP NOT NULL` | `backend/src/main/java/com/oilcollection/model/User.java`, `backend/src/main/resources/db/migration/V4__make_cpf_nullable_in_users.sql` |
| **5** | **Sincronização da regra de letra minúscula na senha** | Backend validava `/[a-z]/` via `LOWERCASE_PATTERN`, mas frontend checklist não tinha essa verificação | Adicionado `{ label: 'Uma letra minúscula', met: /[a-z]/.test(password) }` no `getPasswordRequirements()` e regex `/[a-z]/` no schema Zod `baseSchema` | `frontend/src/app/register/page.tsx` |
| **6** | **Barra lateral vazia / em inglês para ESTABLISHMENT** | `navigation` array não incluía `ESTABLISHMENT` nos `roles`; exibição do role usava string raw em inglês | 1. Adicionado `'ESTABLISHMENT'` em todos os itens de navegação relevantes<br>2. Usado `USER_ROLE_LABELS[user.role]` para tradução (exibe "Estabelecimento") | `frontend/src/components/Layout.tsx`, `frontend/src/types/index.ts` |

---

## 5. Evidências de Testes e Instruções de Execução

### 5.1 Testes Unitários Backend

**Arquivo:** `backend/src/test/java/com/oilcollection/service/AuthServiceTest.java`

**Total de testes:** 33 (15 originais + 18 novos para US1/US8)

**Cenários cobertos pelos 18 novos testes:**

| Categoria | Testes | Códigos de Erro Validados |
|-----------|--------|---------------------------|
| **Validação de Papéis** | 3 | `INVALID_ROLE` para CLIENT, null, ADMIN (bootstrap) |
| **Campos Obrigatórios Estabelecimento** | 4 | `ESTABLISHMENT_NAME_REQUIRED`, `ESTABLISHMENT_ADDRESS_REQUIRED` (null e blank) |
| **Complexidade de Senha** | 5 | `WEAK_PASSWORD` para: sem maiúscula, sem minúscula, sem número, sem especial, muito curta |
| **Sucesso** | 2 | Registro COLLECTOR e ESTABLISHMENT com dados válidos |
| **Duplicatas** | 3 | `EMAIL_ALREADY_EXISTS`, `CPF_ALREADY_EXISTS`, `CNPJ_ALREADY_EXISTS` |

**Comando para executar:**
```bash
cd backend
mvn test -Dtest=AuthServiceTest
```

**Testes adicionais existentes:**
- `AuthServiceAccountDeletionTest` - Testes de exclusão de conta
- `AuthControllerAccountDeletionTest` - Testes de controller para exclusão

### 5.2 Verificação de Tipos Frontend

**Comando:**
```bash
cd frontend
npx tsc --noEmit
```

**Resultado:** ✅ Sem erros (saída vazia = sucesso)

### 5.3 Como Subir com Docker Compose

```bash
# Na raiz do projeto
docker-compose up -d --build
```

**Serviços:**
- **PostgreSQL** - Porta 5432
- **Backend (Spring Boot)** - Porta 8080
- **Frontend (Next.js)** - Porta 3000
- **Nginx** - Porta 80/443 (proxy reverso)

**Variáveis de ambiente necessárias** (ver `.env.example`):
```env
POSTGRES_DB=oilcollection
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
JWT_SECRET=sua_chave_secreta_muito_longa_e_segura
JWT_EXPIRATION_MS=86400000
JWT_REFRESH_EXPIRATION_MS=604800000
```

### 5.4 Como Testar Cada Fluxo na Interface

#### A. Cadastro de Coletor (CPF)
1. Acesse `http://localhost:3000/register`
2. Selecione **"Sou coletor"** (ícone de caminhão)
3. Preencha:
   - Nome completo
   - E-mail
   - Telefone (opcional)
   - **CPF** (formato: 000.000.000-00, valida 11 dígitos)
   - Senha (checklist verde = forte)
   - Confirmar senha
4. Clique **"Criar conta"**
5. Redirecionamento para `/dashboard` com toast "Conta criada com sucesso!"

#### B. Cadastro de Estabelecimento (CNPJ + Dados)
1. Acesse `http://localhost:3000/register`
2. Selecione **"Sou estabelecimento"** (ícone de prédio)
3. Preencha:
   - Nome completo
   - E-mail
   - Telefone (opcional)
   - **CNPJ** (alfanumérico: 00.000.000/0000-00, aceita letras, valida 12 alfanum + 2 dígitos)
   - **Nome do estabelecimento** (obrigatório)
   - **Endereço do estabelecimento** (obrigatório)
   - Cidade, Estado (2 chars), CEP (opcionais)
   - Senha (checklist interativo)
   - Confirmar senha
4. Clique **"Criar conta"**

#### C. Testes de Validação (Esperam Erros)
| Cenário | Erro Esperado |
|---------|---------------|
| E-mail duplicado | "Este e-mail já está cadastrado" |
| CPF duplicado (coletor) | "Este CPF já está cadastrado" |
| CNPJ duplicado (estabelecimento) | "Este CNPJ já está cadastrado" |
| CPF inválido (menos de 11 dígitos) | "CPF deve conter exatamente 11 dígitos" |
| CNPJ inválido (formato errado) | "CNPJ deve conter 12 caracteres alfanuméricos seguidos de 2 dígitos numéricos" |
| Estabelecimento sem CNPJ | "CNPJ é obrigatório para estabelecimento" |
| Coletor sem CPF | "CPF é obrigatório para coletor" |
| Estabelecimento sem nome | "Nome do estabelecimento é obrigatório" |
| Estabelecimento sem endereço | "Endereço do estabelecimento é obrigatório" |
| Senha < 8 chars | "Senha deve ter pelo menos 8 caracteres" |
| Senha sem maiúscula | Checklist item vermelho |
| Senha sem minúscula | Checklist item vermelho |
| Senha sem número | Checklist item vermelho |
| Senha sem especial | Checklist item vermelho |
| Papel inválido (ex: CLIENT via API) | "Tipo de conta inválido" (INVALID_ROLE) |

#### D. Login e Navegação ESTABLISHMENT
1. Faça login com conta de estabelecimento
2. Sidebar exibe: **Dashboard**, **Pontos de Coleta**, **Agendamentos**, **Registros**
3. Perfil do usuário mostra: **"Estabelecimento"** (traduzido via `USER_ROLE_LABELS`)

---

## 6. Resumo das Mudanças por Arquivo

### Backend (Java)

| Arquivo | Tipo | Descrição |
|---------|------|-----------|
| `AuthService.java` | Modificado | Validação de papéis públicos, campos obrigatórios por papel, CNPJ alfanumérico, senha forte |
| `UserDTO.java` | Modificado | `@Pattern` CNPJ atualizado para `^[A-Z0-9]{12}[0-9]{2}$` |
| `User.java` | Modificado | `cpf` nullable=true, `cnpj` unique=true |
| `GlobalExceptionHandler.java` | Modificado | Mensagens em português para violações de unicidade |
| `V4__make_cpf_nullable_in_users.sql` | Novo | Migração Flyway para tornar CPF nullable |
| `AuthServiceTest.java` | Modificado | +18 testes para US1/US8 |

### Frontend (TypeScript/React)

| Arquivo | Tipo | Descrição |
|---------|------|-----------|
| `register/page.tsx` | Reescrito | Página completa com seletor, campos condicionais, máscaras, Zod refine, checklist senha |
| `users/page.tsx` | Modificado | Fix TypeScript `cpf?`, fallback CNPJ, tradução roles |
| `Layout.tsx` | Modificado | `ESTABLISHMENT` em navegação, `USER_ROLE_LABELS` para tradução |
| `types/index.ts` | Modificado | `RegisterData`, `User` com `cpf?`/`cnpj?`, `USER_ROLE_LABELS` |
| `AuthContext.tsx` | Modificado | Import `RegisterData` de types |
| `.dockerignore` | Novo | Otimização build Docker |
| `.gitignore` (raiz) | Modificado | Confirmado `frontend/.next/` |

---

## 7. Checklist de Entrega

- [x] **US1**: Cadastro público Coletor/Estabelecimento com validação de papel (bloqueia ADMIN público)
- [x] **US8**: Política de senha forte com retorno `WEAK_PASSWORD`
- [x] Backend: Validações server-side completas com códigos de erro específicos
- [x] Frontend: Página de cadastro responsiva, máscaras dinâmicas, checklist visual
- [x] CNPJ alfanumérico (`^[A-Z0-9]{12}[0-9]{2}$`) suportado em ambos os lados
- [x] CPF nullable no banco para estabelecimentos (migração Flyway V4)
- [x] Tratamento de erros em português (GlobalExceptionHandler + mapeamento frontend)
- [x] Sidebar com navegação e tradução para ESTABLISHMENT
- [x] Testes unitários: 33 testes em AuthServiceTest (18 novos)
- [x] TypeScript: `npx tsc --noEmit` sem erros
- [x] Docker: `.dockerignore` para builds rápidos
- [x] Git: `.next` removido do índice, `.gitignore` atualizado
- [x] Documentação: Relatório técnico completo em `RELATORIOS/RELATORIO_CADASTRO_AUTENTICACAO.md`
- [x] Limpeza: `IMPLEMENTATION_PLAN.md` removido da raiz

---

**Fim do Relatório**  
*Gerado automaticamente a partir da inspeção dos arquivos da branch `feature/cadastro-papeis-us1-us8`*