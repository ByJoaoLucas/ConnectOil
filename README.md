###### ![Connect Oil  LOGO.png](documentacao/utils/Connect%20Oil%20%20LOGO.png)

# Projeto de Engenharia de Software
#### Sistemas de Informação | Universidade Federal da Paraíba

---
### Acesso Rápido
#### [📋 Backlog](Backlog.md)
#### [📱 Protótipo](Prototipo.md)
#### [👤 Personas](Personas.md)
#### [🛡️ User Stories Éticas](User-Stories-Eticas.md)
#### [🏎️ Sprint Review & Retrospective](Sprint-Rewiew-e-Retrospective.md)
#### [📜 Guia de Entrevista com Stakeholders](Guia-Entrevista-Stakeholders.md)

---
### Artefatos
#### 🎥 Pitch: [Connect Oil | Pitch](utils/Connect%20Oil%20%20Pitch.pdf)
#### 💾 Notion: [Connect Oil | Notion](https://app.notion.com/invite/86231f593f940bc69ab391dc9218407ae0f73613)

---

## Alunos:
* Miguel Nascimento
* Kauã Araujo
* João Lucas
* Joedson Lucas
* Samuel Jackson

---

# Sistema de Coleta de Óleo

Sistema completo para gestão de pontos de coleta, agendamentos e registros de coleta de óleo usado.

## 🏗️ Arquitetura

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Frontend      │     │    Backend      │     │   Database      │
│   (Next.js 14)  │◄───►│   (Spring Boot) │◄───►│   (PostgreSQL)  │
│   Port: 3000    │     │   Port: 8080    │     │   Port: 5432    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

## 🚀 Tecnologias

### Frontend
- **Next.js 14** (App Router) com TypeScript
- **React 18** com Server Components
- **Tailwind CSS** para estilização
- **Next.js Router** para roteamento (App Router)
- **TanStack Query (React Query v5)** para gerenciamento de estado servidor
- **React Hook Form + Zod** para formulários e validação
- **Axios** para requisições HTTP
- **Recharts** para gráficos e relatórios
- **React Leaflet** para mapas
- **Lucide React** para ícones
- **React Hot Toast** para notificações
- **Zustand** para estado global
- **Jest + React Testing Library** para testes
- **ESLint + Prettier** para qualidade de código

### Backend
- **Java 21** com **Spring Boot 3.2.5**
- **Spring Data JPA** com Hibernate 6.4
- **Spring Security 6** com JWT (JJWT 0.12.5)
- **Flyway** para migrações de banco
- **PostgreSQL 16** como banco de dados
- **MapStruct 1.5.5** para mapeamento DTO/Entity
- **Lombok 1.18.32** para redução de boilerplate
- **Springdoc OpenAPI 2.3** para documentação (Swagger UI)
- **JUnit 5 + Mockito + Testcontainers** para testes
- **JaCoCo** para cobertura de código (meta 80%)
- **Spring Boot Actuator** para health checks e métricas
- **Spring Boot Mail** para notificações por email

### DevOps
- **Docker** e **Docker Compose** (multi-stage builds)
- **GitHub Actions** para CI/CD (testes, build, push, deploy)
- **Nginx** como reverse proxy (produção)
- **PostgreSQL 16** em container
- **pgAdmin 4** para administração do banco
- **Trivy** para security scanning
- **Maven** para build do backend
- **npm** para build do frontend

## 📋 Funcionalidades

### Gestão de Pontos de Coleta
- ✅ CRUD completo de pontos de coleta
- ✅ Geolocalização (latitude/longitude)
- ✅ Controle de capacidade e volume atual
- ✅ Status: Ativo, Inativo, Manutenção, Cheio, Pendente Aprovação
- ✅ Atribuição de coletores
- ✅ Filtros por cidade, status, coletor
- ✅ Alertas de "quase cheio"

### Agendamentos
- ✅ Criação e gestão de agendamentos
- ✅ Status: Pendente, Confirmado, Em Andamento, Concluído, Cancelado, Reagendado
- ✅ Tipos: Venda ou Troca (Exchange)
- ✅ Janelas de horário: manhã, tarde, flexível
- ✅ Filtros por data, coletor, ponto de coleta
- ✅ Ações: Confirmar, Iniciar, Concluir, Cancelar, Reagendar
- ✅ Regras de cancelamento (2h antes para estabelecimento)

### Registros de Coleta
- ✅ Registro de coletas realizadas
- ✅ Volume real, tipo de óleo, qualidade, contaminação
- ✅ Código de confirmação
- ✅ Verificação por gerentes
- ✅ Detecção de discrepância (estimado vs real)
- ✅ Fotos e observações
- ✅ GPS da coleta

### Venda e Troca de Óleo
- ✅ Catálogo de produtos de limpeza
- ✅ Preço base do óleo configurável
- ✅ Cálculo de valor (venda) ou seleção de produtos (troca)
- ✅ Itens de troca vinculados ao agendamento

### Relatórios
- ✅ Volume diário
- ✅ Desempenho por coletor
- ✅ Distribuição por tipo de óleo
- ✅ Exportação CSV
- ✅ Certificados de coleta com código de verificação

### Usuários e Autenticação
- ✅ JWT Authentication (access token 24h, refresh token 7 dias)
- ✅ Roles: ADMIN, COLLECTOR, CLIENT, ESTABLISHMENT
- ✅ Refresh token
- ✅ Alteração de senha com validação de complexidade
- ✅ Perfil do usuário
- ✅ Bloqueio de conta após 5 tentativas falhas (30 min)

### Incidentes e Auditoria
- ✅ Registro de incidentes (vazamentos, acidentes)
- ✅ Logs de auditoria para todas as operações críticas
- ✅ Notificações in-app

## 🛠️ Como Executar

### Pré-requisitos
- Docker 24+ e Docker Compose 2+
- Ou: Java 21, Node.js 20, PostgreSQL 16

### Com Docker (Recomendado)

```bash
# Clone o repositório
git clone <repo-url>
cd oil-collection

# Copie o arquivo de exemplo de variáveis de ambiente
cp .env.example .env

# Edite as variáveis conforme necessário
nano .env

# Suba os containers
docker-compose up -d

# Acesse:
# Frontend: http://localhost:3000
# Backend API: http://localhost:8080/api
# Swagger UI: http://localhost:8080/swagger-ui.html
# pgAdmin: http://localhost:5050
```

### Desenvolvimento Local

#### Backend
```bash
cd backend
./mvnw spring-boot:run
```

#### Frontend
```bash
cd frontend
npm install
npm run dev
```

### Credenciais Padrão
- **Admin**: admin@oilcollection.com / admin123

## 📁 Estrutura do Projeto

```
oil-collection/
├── backend/                    # Spring Boot Application
│   ├── src/main/java/com/oilcollection/
│   │   ├── config/            # Configurações (Security, OpenAPI)
│   │   ├── controller/        # REST Controllers
│   │   ├── dto/               # Data Transfer Objects
│   │   ├── exception/         # Exceções personalizadas
│   │   ├── model/             # Entidades JPA
│   │   ├── repository/        # Spring Data Repositories
│   │   ├── security/          # JWT, UserDetails, Filters
│   │   └── service/           # Lógica de negócio
│   ├── src/main/resources/
│   │   ├── application.yml    # Configurações
│   │   └── db/migration/      # Scripts Flyway (V1, V2, V3)
│   └── src/test/              # Testes unitários e integração
│
├── frontend/                   # Next.js 14 Application
│   ├── src/
│   │   ├── app/               # App Router pages
│   │   ├── components/        # Componentes reutilizáveis
│   │   ├── contexts/          # React Contexts (Auth)
│   │   ├── lib/               # Utilitários (API client)
│   │   ├── types/             # TypeScript Types
│   │   └── styles/            # Estilos globais (Tailwind)
│   └── public/                # Assets estáticos
│
├── database/                   # Scripts de inicialização do DB
├── nginx/                      # Configurações Nginx
├── .github/workflows/          # CI/CD Pipeline
├── docker-compose.yml          # Desenvolvimento
├── docker-compose.prod.yml     # Produção
└── .env.example               # Exemplo de variáveis de ambiente
```

## 🔧 Variáveis de Ambiente

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `POSTGRES_DB` | Nome do banco de dados | `oil_collection` |
| `POSTGRES_USER` | Usuário do PostgreSQL | `postgres` |
| `POSTGRES_PASSWORD` | Senha do PostgreSQL | `postgres` |
| `JWT_SECRET` | Chave secreta para JWT | (obrigatório) |
| `MAIL_HOST` | Servidor SMTP | `smtp.gmail.com` |
| `MAIL_PORT` | Porta SMTP | `587` |
| `MAIL_USERNAME` | Usuário SMTP | - |
| `MAIL_PASSWORD` | Senha SMTP (app password) | - |
| `NEXT_PUBLIC_API_URL` | URL da API no frontend | `http://localhost:8080/api` |
| `NEXT_PUBLIC_INTERNAL_API_URL` | URL interna da API para SSR no Docker | `http://backend:8080/api` |
| `PGADMIN_EMAIL` | Email do pgAdmin | `admin@oilcollection.com` |
| `PGADMIN_PASSWORD` | Senha do pgAdmin | `admin123` |

## 🧪 Testes

```bash
# Backend
cd backend
./mvnw test

# Frontend
cd frontend
npm test
npm run test:coverage
```

## 📦 Build para Produção

```bash
# Backend
cd backend
./mvnw clean package -Pdocker

# Frontend
cd frontend
npm run build
```

## 🚀 Deploy

### Docker Compose (Produção)
```bash
docker-compose -f docker-compose.prod.yml up -d
```

### Deploy com Railway (Backend) + Vercel (Frontend)

#### 1. Preparação Local

```bash
# 1. Instale as CLIs
npm i -g @railway/cli vercel

# 2. Faça login
railway login
vercel login

# 3. Configure variáveis de ambiente local (opcional)
cp .env.example .env
# Edite .env com suas credenciais
```

#### 2. Deploy do Backend no Railway

```bash
cd backend

# Inicialize o projeto Railway (ou conecte a um existente)
railway init

# Configure as variáveis de ambiente no Railway
railway variables set SPRING_PROFILES_ACTIVE=prod
railway variables set DATABASE_URL="jdbc:postgresql://<railway-postgres-host>:5432/<dbname>"
railway variables set DATABASE_USERNAME="<user>"
railway variables set DATABASE_PASSWORD="<password>"
railway variables set JWT_SECRET="<seu-jwt-secret-base64>"
railway variables set MAIL_HOST="smtp.gmail.com"
railway variables set MAIL_PORT="587"
railway variables set MAIL_USERNAME="<seu-email>"
railway variables set MAIL_PASSWORD="<app-password>"
railway variables set FRONTEND_URL="https://<seu-projeto>.vercel.app"

# Deploy
railway up
```

**Nota:** O Railway cria automaticamente um banco PostgreSQL. Use `railway variables` para ver as variáveis geradas.

#### 3. Deploy do Frontend no Vercel

```bash
cd frontend

# Deploy (primeira vez)
vercel

# Deploy para produção
vercel --prod
```

Configure as variáveis de ambiente no dashboard do Vercel:
- `NEXT_PUBLIC_API_URL`: `https://<seu-backend>.railway.app` (ex: `https://connectoil-api.railway.app`)

#### 4. Atualize CORS no Backend

Após obter a URL do Vercel, atualize a variável `FRONTEND_URL` no Railway:
```bash
railway variables set FRONTEND_URL="https://seu-projeto.vercel.app"
```

Redeploy automático será acionado.

### Kubernetes (Exemplo)
```yaml
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: oil-collection-backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: oil-collection-backend
  template:
    metadata:
      labels:
        app: oil-collection-backend
    spec:
      containers:
      - name: backend
        image: ghcr.io/username/oil-collection-backend:latest
        ports:
        - containerPort: 8080
        envFrom:
        - secretRef:
            name: oil-collection-secrets
```

## 📚 Documentação da API

Acesse o Swagger UI em: `http://localhost:8080/swagger-ui.html`

### Principais Endpoints

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/auth/login` | Login |
| POST | `/api/auth/register` | Registro |
| POST | `/api/auth/refresh` | Refresh token |
| GET | `/api/auth/me` | Usuário atual |
| GET | `/api/collection-points` | Listar pontos |
| POST | `/api/collection-points` | Criar ponto |
| GET | `/api/schedules` | Listar agendamentos |
| POST | `/api/schedules` | Criar agendamento |
| GET | `/api/records` | Listar registros |
| POST | `/api/records` | Criar registro |
| GET | `/api/collector/complete/{id}` | Completar coleta |
| GET | `/api/reports/*` | Relatórios |
| GET | `/api/products` | Listar produtos |
| GET | `/api/incidents` | Listar incidentes |
| GET | `/api/notifications` | Notificações |

## 🔒 Segurança

- Senhas criptografadas com BCrypt (cost 12)
- JWT com expiração de 24h e refresh token de 7 dias
- CORS configurado para origins específicas
- Validação de entrada em todas as camadas (Bean Validation)
- Rate limiting configurável
- Headers de segurança (HSTS, X-Frame-Options, CSP)
- Account lockout após 5 tentativas (30 min)
- Auditoria de todas as operações sensíveis

## 🤝 Contribuindo

1. Fork o projeto
2. Crie uma branch: `git checkout -b feature/nova-funcionalidade`
3. Commit: `git commit -m 'Adiciona nova funcionalidade'`
4. Push: `git push origin feature/nova-funcionalidade`
5. Abra um Pull Request

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para detalhes.

## 📞 Suporte

Para dúvidas ou suporte, abra uma issue no GitHub ou envie email para suporte@oilcollection.com
