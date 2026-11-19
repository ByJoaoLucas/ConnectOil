# Avaliação Geral do Código — ConnectOil

## 1. Visão Arquitetural Geral
O repositório **ConnectOil** está estruturado em quatro camadas bem definidas:
- **Backend (`backend/`)**: Java 21 + Spring Boot 3.2.5, Spring Security com JWT (`jjwt` 0.12.5), Spring Data JPA/Hibernate (`ddl-auto: validate`), Flyway (`V1` a `V4`), MapStruct, Lombok, Actuator/Prometheus e SpringDoc OpenAPI.
- **Frontend (`frontend/`)**: Next.js 14.1.0 (App Router), React 18, TypeScript 5, Tailwind CSS, Axios, React Hook Form + Zod, React Query, Zustand, Leaflet e Recharts.
- **Banco de Dados (`database/`)**: PostgreSQL 16 (`postgres:16-alpine`) com script inicial `init.sql` (`uuid-ossp`, timezone `America/Sao_Paulo`) + migrations Flyway versionadas.
- **Infraestrutura (`docker-compose.yml` e `nginx/`)**: Orquestração Docker com healthchecks (`postgres`, `backend`, `frontend`, `pgadmin`) e configuração de proxy reverso Nginx com terminação SSL/TLS (`nginx/proxy.conf`).

### Pontos Positivos Identificados
- Uso de **Flyway** com `ddl-auto: validate` no JPA (boa prática para integridade do schema).
- Senhas criptografadas com **BCrypt (força 12)**, bloqueio de conta por tentativas inválidas, política de complexidade de senha configurável no banco (`SystemConfigService`) e trilha de auditoria (`AuditLogService`).
- Separação limpa de camadas no backend (`controller`, `service`, `repository`, `model`, `dto`, `security`, `exception`) e cliente HTTP tipado com interceptors de refresh token no frontend (`src/lib/api.ts`).

---

## 2. Resumo dos Erros e Problemas Encontrados (por Severidade)

| ID | Severidade | Camada | Arquivo(s) | Problema Resumido |
|---|---|---|---|---|
| **ERR-01** | **CRÍTICA** | Segurança / Backend | `AuthService.java` (linha 44) | Papel `ADMIN` liberado no cadastro público (`/api/auth/register`) |
| **ERR-02** | **CRÍTICA** | Segurança / Backend | `SecurityConfig.java` vs Controllers | Conflito de permissões URL vs `@PreAuthorize` bloqueia `ESTABLISHMENT` em endpoints válidos |
| **ERR-03** | **ALTA** | Backend | `UserService.java` (linhas 62, 108) | `NullPointerException` (`user.getCpf().equals(...)`) quando `cpf` é `null` (após Migration V4) |
| **ERR-04** | **ALTA** | Backend / JWT | `application.yml` & `docker-compose.yml` | Segredo `JWT_SECRET` padrão hardcoded em texto claro no repositório |
| **ERR-05** | **ALTA** | Frontend / Backend | `api.ts`, `application.yml`, Controllers | Prefixação dupla `/api/api/...` e URL `http://localhost:8080/api/api` hardcoded no navegador |
| **ERR-06** | **ALTA** | Frontend / SSR | `Layout.tsx` (linha 87) | Uso direto de `window.location.pathname` no render (risco de erro de SSR/hidratação) |
| **ERR-07** | **MÉDIA** | Frontend / Testes | `jest.config.js` (linha 3) | Propriedade inválida `setupFilesAfterSetup` (o correto é `setupFilesAfterEnv`) |
| **ERR-08** | **MÉDIA** | Frontend / Segurança | `src/app/**/page.tsx` | Páginas autenticadas sem `layout.tsx` de proteção de rota no nível de diretório |
| **ERR-09** | **MÉDIA** | Infra / Nginx | `nginx/proxy.conf` vs `docker-compose.yml` | `upstream frontend` aponta para `frontend:80`, mas o container Next.js escuta na porta `3000` |
| **ERR-10** | **BAIXA** | Configuração / Env | `.env.example` vs `[id]/page.tsx` | Typo `NEXT_PUBLIC_GOGLE_MAPS_API_KEY` (falta um `O`) impede leitura da chave do Google Maps |
| **ERR-11** | **BAIXA** | Segurança / HTTP | `SecurityConfig.java` (linha 71) | `contentTypeOptions().disable()` desativa o cabeçalho `X-Content-Type-Options: nosniff` |

---

## 3. Organização desta Pasta (`erros_a_corrigir/`)
- `00_RESUMO_AVALIACAO_GERAL.md`: Visão geral da arquitetura, pontos fortes e matriz de severidade.
- `01_BACKEND_SEGURANCA_E_REGRAS.md`: Detalhamento técnico e código corrigido para **ERR-01, ERR-02, ERR-03, ERR-04 e ERR-11**.
- `02_FRONTEND_ROTAS_SSR_E_TESTES.md`: Detalhamento técnico e código corrigido para **ERR-05, ERR-06, ERR-07, ERR-08 e ERR-10**.
- `03_INFRA_NGINX_E_AMBIENTE.md`: Detalhamento técnico e ajustes para **ERR-09**, portas locais e deploy.
