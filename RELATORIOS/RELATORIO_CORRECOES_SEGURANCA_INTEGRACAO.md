# Relatorio das Correcoes de Estabilizacao e Integracao (Miguel)

**Data:** 20/11/2026  
**Projeto:** ConnectOil  
**Escopo:** Fase 1 do briefing tecnico de seguranca, integracao front-back e preparacao para User Stories

## 1. Estado inicial

Antes das correcoes, o projeto apresentava riscos e inconsistencias nas seguintes areas:

- O cadastro publico aceitava o papel `ADMIN`, permitindo criacao externa de conta administrativa.
- As regras de URL do Spring Security bloqueavam perfis antes da avaliacao dos `@PreAuthorize`, impedindo operacoes validas de `ESTABLISHMENT` e `COLLECTOR`.
- A atualizacao de usuarios podia executar `user.getCpf().equals(...)` com CPF nulo e causar `NullPointerException`.
- O backend utilizava `context-path: /api` enquanto os controllers ja iniciavam com `/api`, produzindo URLs duplicadas como `/api/api/...`.
- O cliente Axios navegacao tinha URL hardcoded para `localhost:8080/api/api` e nao respeitava corretamente as variaveis de ambiente.
- O SSR do frontend podia usar uma URL publica incorreta dentro da rede Docker.
- `Layout.tsx` acessava `window.location.pathname` diretamente durante a renderizacao.
- O JWT possuia fallback com chave Base64 fixa no backend e no Docker.
- A configuracao Jest usava `setupFilesAfterSetup`, uma chave invalida.
- O upstream Nginx apontava Next.js para a porta `80`, embora o container exponha a porta `3000`.
- Parte das rotas frontend protegidas nao possuia layout de autenticacao proprio.

## 2. Correcoes implementadas

### 2.1 Cadastro publico seguro

Em `AuthService`, o conjunto de perfis publicos passou de:

- `ESTABLISHMENT`
- `COLLECTOR`
- `ADMIN`

para:

- `ESTABLISHMENT`
- `COLLECTOR`

O teste que aceitava cadastro publico de `ADMIN` foi alterado para esperar `INVALID_ROLE`.

### 2.2 Permissoes HTTP

Em `SecurityConfig`, as regras por URL foram alinhadas aos `@PreAuthorize` dos controllers:

- `/api/records/**` aceita `ADMIN`, `COLLECTOR` e `ESTABLISHMENT`.
- `/api/incidents/**` aceita `ADMIN`, `COLLECTOR` e `ESTABLISHMENT`.
- `GET /api/exchange-items/**` aceita `ADMIN`, `COLLECTOR` e `ESTABLISHMENT`; demais metodos permanecem administrativos.
- `GET /api/system-config/price/base-oil` aceita `ADMIN` e `COLLECTOR`.
- `GET /api/collector-coverage/**` aceita `ADMIN` e `COLLECTOR`.
- Operacoes administrativas nao GET continuam restritas conforme os metodos dos controllers.

Tambem foi removida a desativacao de `X-Content-Type-Options`, mantendo o comportamento `nosniff`.

### 2.3 CPF e CNPJ nulos

Em `UserService`, consultas de duplicidade passaram a considerar:

- CPF com valor ausente ou blank;
- CNPJ com valor ausente ou blank;
- alteracao de CPF/CNPJ apenas quando o novo valor for diferente do valor atual.

Isso elimina `NullPointerException` ao editar estabelecimentos com CPF nulo e evita consultas desnecessarias com documentos vazios.

### 2.4 Unificacao dos endpoints

Foi removido o `context-path: /api` do backend, porque os controllers ja possuem o prefixo `/api`.

Como resultado:

- O backend continua expondo `/api/auth`, `/api/users`, `/api/records`, etc.
- O frontend usa a mesma base sem adicionar `/api` novamente.
- URLs antigas com `/api/api` deixaram de ser usadas.
- O `NEXT_PUBLIC_API_URL` passou a ser usado no navegador.
- O `NEXT_PUBLIC_INTERNAL_API_URL` passou a ser usado por requisicoes SSR dentro do Docker.

O cliente Axios tambem teve contratos divergentes corrigidos:

- Notificacoes: `/notifications` e `/notifications/count/unread`;
- Configuracoes: `/system-config/list`;
- Atualizacao de configuracao: `POST /system-config`;
- Exclusao de configuracao: `DELETE /system-config/{id}`.

### 2.5 Navegacao e protecao no frontend

`Layout.tsx` passou a usar `usePathname()` em vez de `window.location.pathname`.

Foi criado `ProtectedLayout` compartilhado para:

- redirecionar usuarios nao autenticados para `/login`;
- aguardar a inicializacao do contexto de autenticacao;
- envolver o conteudo no layout autenticado padrao.

Layouts autenticados foram adicionados em `collector`, `incidents` e `products`. Rotas dinamicas existentes passam pelos layouts protegidos dos respectivos grupos.

### 2.6 JWT

O fallback fixo de `JWT_SECRET` foi removido de `application.yml` e `docker-compose.yml`.

O `.env.example` passou a exigir a configuracao explicita e documenta a geracao por:

```text
openssl rand -base64 64
```

O Docker Compose exige `JWT_SECRET` no startup por meio de `${JWT_SECRET:?JWT_SECRET must be set}`.

Durante o debug posterior, foi confirmado que a chave precisa estar em Base64 valido e decodificar para pelo menos 32 bytes. Texto comum ou Base64 URL-safe nao deve ser usado nessa configuracao.

### 2.7 Qualidade e infraestrutura

Foram corrigidos:

- `setupFilesAfterSetup` para `setupFilesAfterEnv` no Jest;
- upstream Nginx de `frontend:80` para `frontend:3000`;
- URL da API no workflow CI;
- variaveis Docker publicas e internas do frontend;
- `JWT_SECRET` no Compose;
- Swagger/README para o prefixo unico `/api`;
- exemplos antigos com `/api/api` em relatorios operacionais;
- documentacao explicativa do cliente Axios.

## 3. Arquivos alterados

### Backend

- `backend/src/main/java/com/oilcollection/service/AuthService.java`
- `backend/src/main/java/com/oilcollection/service/UserService.java`
- `backend/src/main/java/com/oilcollection/security/SecurityConfig.java`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/com/oilcollection/service/AuthServiceTest.java`

### Frontend

- `frontend/src/lib/api.ts`
- `frontend/src/components/Layout.tsx`
- `frontend/src/components/ProtectedLayout.tsx`
- `frontend/src/app/collector/layout.tsx`
- `frontend/src/app/incidents/layout.tsx`
- `frontend/src/app/products/layout.tsx`
- `frontend/jest.config.js`

### Infraestrutura e documentacao

- `.env.example`
- `docker-compose.yml`
- `nginx/proxy.conf`
- `.github/workflows/ci-cd.yml`
- `README.md`
- `RELATORIOS/CHANGELOG_CRUD_ESTABELECIMENTO.md`
- `RELATORIOS/RELATORIO_EXCLUSAO_CONTAS.md`
- `connectoil_src_explicacoes/frontend/src/lib/README_EXPLICACAO.md`

## 4. Problemas encontrados durante a inicializacao

O primeiro erro de build estava ligado ao JDK 24 usado no ambiente, incompativel com a configuracao Lombok do projeto. O projeto declara Java 21 e o ambiente foi alinhado para essa versao.

Depois da correcao do JDK, o Spring iniciou o contexto ate a criacao do `JwtTokenProvider` e encontrou um problema separado:

```text
io.jsonwebtoken.io.DecodingException: Illegal base64 character: '_'
```

A causa foi uma `JWT_SECRET` invalida ou ausente no ambiente. Essa exigencia passou a ser obrigatoria, sem fallback inseguro.

## 5. Pendencias operacionais

- Definir `JWT_SECRET` valido no IntelliJ, terminal, `.env` ou plataforma de deploy.
- Corrigir o typo `NEXT_PUBLIC_GOGLE_MAPS_API_KEY` no `.env.example` para `NEXT_PUBLIC_GOOGLE_MAPS_API_KEY`.
- Executar novamente os testes Maven e Jest apos a configuracao do ambiente.
- Executar o build de producao do frontend.
- Validar manualmente os fluxos por papel:
  - estabelecimento confirma coleta e solicita certificado;
  - estabelecimento abre incidente;
  - coletor consulta cobertura e itens de troca;
  - coletor consulta preco base;
  - usuarios sem autenticacao sao redirecionados no frontend.

## 6. Resultado

A Fase 1 foi aplicada no codigo com alinhamento entre:

- seguranca backend;
- contratos de endpoint;
- configuracao de ambiente;
- comunicacao cliente-servidor;
- navegacao frontend;
- Docker e Nginx;
- testes e documentacao.

## 7. Testes adicionados e validados

### 7.1 Backend

Foram adicionados testes para:

- Cadastro publico de `ADMIN`, esperando rejeicao com `INVALID_ROLE`.
- Cadastro de estabelecimento com CPF nulo sem consulta desnecessaria ao repositorio de CPF.
- Atualizacao de estabelecimento com CPF ou CNPJ nulos ou vazios.
- Inicializacao do `JwtTokenProvider` com segredo valido, invalido e ausente.
- Autorizacao por perfil para:
  - estabelecimento confirmar coleta;
  - estabelecimento obter certificado;
  - estabelecimento abrir incidente;
  - coletor consultar itens de troca;
  - coletor consultar preco base do oleo;
  - coletor consultar cobertura.

Os testes ficaram distribuidos em:

- `AuthServiceTest`;
- `UserServiceTest`;
- `JwtTokenProviderTest`;
- `EndpointAuthorizationTest`;
- `OilCollectionApplicationTests`.

Resultado da suite backend executada com Java 21:

- 87 testes executados;
- 0 falhas;
- 0 erros;
- 1 teste ignorado por Docker/Testcontainers indisponivel.

Os testes unitarios e de autorizacao HTTP passaram. O teste `OilCollectionApplicationTests`, que inicia um PostgreSQL em container, foi configurado com `disabledWithoutDocker = true` para nao impedir a suiete local quando o Docker nao estiver acessivel.

### 7.2 Frontend

Foram adicionados testes em:

- `frontend/src/lib/api.test.ts`;
- `frontend/src/components/ProtectedLayout.test.tsx`.

A cobertura inclui:

- uso de `NEXT_PUBLIC_API_URL` no navegador;
- uso de `NEXT_PUBLIC_INTERNAL_API_URL` no SSR;
- fallback para `/api`;
- ausencia de duplicacao `/api/api`;
- contrato real das rotas de notificacoes e configuracoes;
- refresh token pela mesma base URL;
- redirecionamento de usuario nao autenticado para `/login`;
- renderizacao do conteudo protegido;
- estado de carregamento da autenticacao.

Resultado da suuite Jest:

- 2 suites executadas;
- 9 testes executados;
- 9 testes aprovados;
- 0 falhas.

O build de producao Next.js tambem foi executado com sucesso.

### 7.3 Dependencias de teste

Foram declaradas no frontend:

- `@testing-library/jest-dom`;
- `@testing-library/react`.

Tambem foi ajustada a configuracao Jest:

- transformacao TypeScript/TSX explicitamente configurada;
- artefatos `.next` ignorados pelo haste map;
- `setupFilesAfterEnv` utilizado corretamente.

## 8. Aumento da cobertura de testes do frontend

Foi iniciada a Meta 1 de cobertura com testes de API, autenticacao, layouts, login, cadastro, dashboard, pontos de coleta, registros, agendamentos e incidentes.

Resultado atual da execucao `npm run test:coverage -- --runInBand`:

| Indicador | Cobertura atual |
|---|---:|
| Statements | 29,21% |
| Branches | 19,58% |
| Functions | 16,07% |
| Lines | 29,05% |

- 12 suites de testes executadas;
- 51 testes aprovados;
- 0 falhas;
- Meta 1 parcialmente atingida;
- Statements, Branches e Lines ja ultrapassaram os objetivos de 20%, 15% e 20%, respectivamente;
- Functions atingiu 16,07%, ainda abaixo da meta de 20%;
- Nao foram desabilitadas regras nem enfraquecidos testes para elevar os numeros.

Principais modulos cobertos:

- Cliente Axios e grupos de endpoints;
- contexto de autenticacao;
- layout protegido e layout principal;
- login e cadastro;
- dashboard;
- pontos de coleta e seus detalhes;
- registros de coleta;
- agendamentos;
- incidentes.
