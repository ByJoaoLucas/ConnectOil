# Relatório de Remoção do Perfil "MANAGER" (Gerente)

**Data:** 18 de Novembro de 2026  
**Projeto:** ConnectOil  
**Branch:** `feature/user-tests`  
**Autor:** kauaduartt

---

## 1. Resumo Executivo

Este documento registra a remoção completa do perfil de usuário **MANAGER (Gerente)** do sistema ConnectOil. A decisão foi baseada na simplificação da arquitetura de permissões, mantendo apenas 4 papéis essenciais: **ADMIN**, **COLLECTOR**, **CLIENT** e **ESTABLISHMENT**.

**Impacto:** Nenhuma funcionalidade foi perdida. As permissões que eram exclusivas do MANAGER foram redistribuídas para o ADMIN (operações administrativas) ou mantidas para COLLECTOR/ESTABLISHMENT (operações operacionais).

---

## 2. Motivação

### Problemas Identificados
1. **Complexidade desnecessária** - O MANAGER tinha permissões idênticas ao ADMIN na maioria dos endpoints, criando ambiguidade
2. **Duplicação de responsabilidades** - Não havia distinção clara entre "gestor operacional" (MANAGER) e "superusuário" (ADMIN)
3. **Manutenção de código** - Cada novo endpoint exigia decisões sobre incluir MANAGER ou não
4. **Frontend confuso** - Menus e formulários exibiam "Gerente" sem propósito real diferenciado

### Decisão
Remover completamente o papel MANAGER, mantendo:
- **ADMIN**: Controle total do sistema (usuários, configurações, deleções)
- **COLLECTOR**: Operações de coleta (agendamentos, registros, pontos, incidentes)
- **ESTABLISHMENT**: Confirmação de coletas, visualização de pontos/agendamentos
- **CLIENT**: Visualização básica (dashboard, pontos de coleta)

---

## 3. Alterações Realizadas

### 3.1 Backend (Java/Spring Boot)

#### Modelo e Enum
| Arquivo | Alteração |
|---------|-----------|
| `User.java` | Removido `MANAGER` do enum `UserRole` (linha 151) |

#### Controllers - Permissões Atualizadas

| Controller | Endpoints Afetados | Antes | Depois |
|------------|-------------------|-------|--------|
| **UserController** | GET/POST/PUT/DELETE `/api/users/**` | `hasAnyRole('ADMIN','MANAGER')` | `hasRole('ADMIN')` |
| **CollectorCoverageController** | GET/POST/PUT/DELETE `/api/collector-coverage/**` | `hasAnyRole('ADMIN','MANAGER')` / `hasAnyRole('ADMIN','MANAGER','COLLECTOR')` | `hasRole('ADMIN')` / `hasAnyRole('ADMIN','COLLECTOR')` |
| **ProductController** | GET/POST/PUT/DELETE `/api/products/**` | `hasAnyRole('ADMIN','MANAGER',...)` | `hasAnyRole('ADMIN','COLLECTOR','ESTABLISHMENT')` / `hasRole('ADMIN')` |
| **CollectionScheduleController** | GET/POST/PUT/DELETE `/api/schedules/**` | `hasAnyRole('ADMIN','MANAGER','COLLECTOR')` | `hasAnyRole('ADMIN','COLLECTOR')` / `hasRole('ADMIN')` |
| **SystemConfigController** | GET/POST/DELETE `/api/system-config/**` | `hasAnyRole('ADMIN','MANAGER')` | `hasRole('ADMIN')` |
| **NotificationController** | GET/PUT `/api/notifications/**` | `hasAnyRole('ADMIN','MANAGER','COLLECTOR','ESTABLISHMENT')` | `hasAnyRole('ADMIN','COLLECTOR','ESTABLISHMENT')` |
| **CollectionPointController** | GET/POST/PUT/DELETE `/api/collection-points/**` | `hasAnyRole('ADMIN','MANAGER',...)` | `hasAnyRole('ADMIN','COLLECTOR','CLIENT','ESTABLISHMENT')` / `hasRole('ADMIN')` |
| **IncidentController** | GET/POST/PUT/DELETE `/api/incidents/**` | `hasAnyRole('ADMIN','MANAGER','COLLECTOR')` | `hasAnyRole('ADMIN','COLLECTOR')` / `hasRole('ADMIN')` |
| **CollectionRecordController** | GET/POST/PUT/DELETE `/api/records/**` | `hasAnyRole('ADMIN','MANAGER','COLLECTOR')` | `hasAnyRole('ADMIN','COLLECTOR')` / `hasRole('ADMIN')` |
| **ExchangeItemController** | GET/POST/DELETE `/api/exchange-items/**` | `hasAnyRole('ADMIN','MANAGER',...)` | `hasAnyRole('ADMIN','COLLECTOR','ESTABLISHMENT')` / `hasRole('ADMIN')` |

#### Configuração de Segurança
| Arquivo | Alteração |
|---------|-----------|
| `SecurityConfig.java` | Todas as regras `authorizeHttpRequests` atualizadas para remover MANAGER dos matchers de URL |

#### Testes
| Arquivo | Alteração |
|---------|-----------|
| `AuthServiceTest.java` | Removido teste `register_ManagerRole_ThrowsInvalidRole()` (linhas 309-321) |

---

### 3.2 Frontend (Next.js/TypeScript/React)

#### Tipos TypeScript
| Arquivo | Alteração |
|---------|-----------|
| `types/index.ts` | `UserRole` type: `'ADMIN' \| 'MANAGER' \| 'COLLECTOR' \| 'CLIENT' \| 'ESTABLISHMENT'` → `'ADMIN' \| 'COLLECTOR' \| 'CLIENT' \| 'ESTABLISHMENT'`<br>`USER_ROLE_LABELS`: Removida entrada `MANAGER: 'Gerente'` |

#### Componentes de Navegação
| Arquivo | Alteração |
|---------|-----------|
| `components/Layout.tsx` | Array `navigation`: Removido `'MANAGER'` de todos os `roles` dos itens de menu |

#### Páginas Protegidas
| Arquivo | Alteração |
|---------|-----------|
| `app/reports/layout.tsx` | Verificação de acesso: `user?.role !== 'ADMIN' && user?.role !== 'MANAGER'` → `user?.role !== 'ADMIN'` |
| `app/users/layout.tsx` | Verificação de acesso: `user?.role !== 'ADMIN' && user?.role !== 'MANAGER'` → `user?.role !== 'ADMIN'` |

#### Página de Usuários (CRUD Admin)
| Arquivo | Alterações |
|---------|-----------|
| `app/users/page.tsx` | 1. Schema Zod: `z.enum(['ADMIN','MANAGER','COLLECTOR','CLIENT'])` → `z.enum(['ADMIN','COLLECTOR','CLIENT'])`<br>2. Filtro de role: Removida `<option value="MANAGER">Gerente</option>`<br>3. Badge de role: Removido case `user.role === 'MANAGER' ? 'badge-blue'`<br>4. Formulário select: Removida `<option value="MANAGER">Gerente</option>` |

#### Página de Perfil
| Arquivo | Alteração |
|---------|-----------|
| `app/profile/page.tsx` | 1. Badge de exibição: Removido case `user?.role === 'MANAGER' ? 'bg-blue-100 text-blue-800'`<br>2. Select desabilitado: Removida `<option value="MANAGER">Gerente</option>` |

---

### 3.3 Documentação

| Arquivo | Alterações |
|---------|-----------|
| `../README.md` | Linha 101: `Roles: ADMIN, MANAGER, COLLECTOR, CLIENT, ESTABLISHMENT` → `Roles: ADMIN, COLLECTOR, CLIENT, ESTABLISHMENT` |
| `RELATORIO_CADASTRO_AUTENTICACAO.md` | 1. Código de navegação Layout.tsx atualizado<br>2. Tipos TypeScript atualizados<br>3. Tabela de testes: "Validação de Papéis" de 4 para 3 testes<br>4. Tabela de erros: "Papel inválido (ex: MANAGER via API)" → "Papel inválido (ex: CLIENT via API)" |

---

## 4. Matriz de Permissões Final

### Após Remoção (Estado Atual)

| Recurso | ADMIN | COLLECTOR | CLIENT | ESTABLISHMENT |
|---------|-------|-----------|--------|---------------|
| **Usuários** (CRUD) | ✅ Total | ❌ | ❌ | ❌ |
| **Pontos de Coleta** (Leitura) | ✅ | ✅ | ✅ | ✅ |
| **Pontos de Coleta** (Escrita) | ✅ | ❌ | ❌ | ❌ |
| **Agendamentos** (Leitura) | ✅ | ✅ | ❌ | ✅ |
| **Agendamentos** (Escrita) | ✅ | ✅ | ❌ | ✅ |
| **Registros** (Leitura) | ✅ | ✅ | ❌ | ❌ |
| **Registros** (Criação/Edição) | ✅ | ✅ | ❌ | ❌ |
| **Registros** (Verificação) | ✅ | ❌ | ❌ | ❌ |
| **Produtos** (Leitura) | ✅ | ✅ | ❌ | ✅ |
| **Produtos** (Escrita) | ✅ | ❌ | ❌ | ❌ |
| **Incidentes** (Leitura) | ✅ | ✅ | ❌ | ❌ |
| **Incidentes** (Criação) | ✅ | ✅ | ❌ | ✅ |
| **Incidentes** (Resolução) | ✅ | ❌ | ❌ | ❌ |
| **Notificações** | ✅ | ✅ | ❌ | ✅ |
| **Config. Sistema** | ✅ | ❌ | ❌ | ❌ |
| **Cobertura Coletor** (Leitura) | ✅ | ✅ | ❌ | ❌ |
| **Cobertura Coletor** (Escrita) | ✅ | ❌ | ❌ | ❌ |
| **Relatórios** | ✅ | ❌ | ❌ | ❌ |
| **Itens de Troca** (Leitura) | ✅ | ✅ | ❌ | ✅ |
| **Itens de Troca** (Escrita) | ✅ | ❌ | ❌ | ❌ |

---

## 5. Verificação de Integridade

### Checklist de Validação

- [x] **Enum UserRole** - MANAGER removido, valores restantes contíguos
- [x] **Controllers** - Todos os `@PreAuthorize` atualizados, sem referências órfãs
- [x] **SecurityConfig** - Matchers de URL consistentes com controllers
- [x] **AuthService** - `PUBLIC_REGISTRATION_ROLES` não continha MANAGER (já correto)
- [x] **Testes** - Teste de MANAGER removido, testes restantes válidos
- [x] **Frontend Types** - `UserRole` e `USER_ROLE_LABELS` sincronizados
- [x] **Navegação** - Sidebar filtra corretamente sem MANAGER
- [x] **Páginas Protegidas** - Guards de rota verificam apenas ADMIN
- [x] **Formulários** - Selects de role não incluem MANAGER
- [x] **Badges/Exibição** - Nenhum case switch para MANAGER
- [x] **Documentação** - README e relatórios técnicos atualizados

### Busca Global Final
```
grep -r "MANAGER" --include="*.java" --include="*.ts" --include="*.tsx" \
  --exclude-dir=RELATORIOS --exclude="*.md"
```
**Resultado:** Zero ocorrências em código de produção e testes.

---

## 6. Impacto Zero em Funcionalidades

### O que NÃO mudou:
1. **Autenticação/Registro** - Fluxos de login, registro, refresh token, troca de senha
2. **Coleta Operacional** - Coletores continuam criando/atualizando agendamentos, registros, incidentes
3. **Estabelecimentos** - Continuam **solicitando coletas (criando agendamentos)**, confirmando coletas, visualizando pontos/agendamentos
4. **Admin** - Mantém controle total (agora único gestor de usuários e configurações)
5. **API Contracts** - DTOs, responses, códigos de erro inalterados
6. **Banco de Dados** - Enum armazenado como STRING, valores existentes não afetados

### Bug Corrigido Durante Revisão
Durante a validação do relatório, identificou-se que a remoção do MANAGER havia deixado o endpoint `POST /api/schedules` (criar agendamento) restrito apenas a ADMIN, impedindo estabelecimentos de solicitar coletas.

**Correção aplicada:**
- `CollectionScheduleController.java` (linha 112): `@PreAuthorize("hasRole('ADMIN')")` → `@PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR', 'ESTABLISHMENT')")`
- `SecurityConfig.java` (linha 88): `hasAnyRole("ADMIN", "COLLECTOR")` → `hasAnyRole("ADMIN", "COLLECTOR", "ESTABLISHMENT")`

### Migração de Dados (se houver usuários MANAGER existentes):
```sql
-- Se necessário, migração única:
UPDATE users SET role = 'ADMIN' WHERE role = 'MANAGER';
-- Ou para papel operacional:
UPDATE users SET role = 'COLLECTOR' WHERE role = 'MANAGER';
```
> **Nota:** Como o sistema está em desenvolvimento, não há dados de produção para migrar.

---

## 7. Conclusão

A remoção do perfil **MANAGER** foi concluída com **sucesso total**:

✅ **0 referências remanescentes** em código de produção  
✅ **Arquitetura simplificada** - 4 papéis claros e distintos  
✅ **Permissões lógicas** - ADMIN para admin, COLLECTOR/ESTABLISHMENT para operações  
✅ **Frontend consistente** - Menus, formulários, guards alinhados  
✅ **Testes válidos** - Suite de testes passa sem o caso MANAGER  
✅ **Documentação sincronizada** - README e relatórios técnicos atualizados  

O sistema está **pronto para uso** com a nova estrutura de permissões.
