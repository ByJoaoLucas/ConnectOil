# Sistema de Coleta de Óleo

Sistema completo para gestão de pontos de coleta, agendamentos e registros de coleta de óleo usado.

## 🏗️ Arquitetura

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Frontend      │     │    Backend      │     │   Database      │
│   (React)       │◄───►│   (Spring Boot) │◄───►│   (PostgreSQL)  │
│   Port: 3000    │     │   Port: 8080    │     │   Port: 5432    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

## 🚀 Tecnologias

### Frontend
- **React 18** com TypeScript
- **Vite** como build tool
- **Tailwind CSS** para estilização
- **React Router** para roteamento
- **TanStack Query** para gerenciamento de estado servidor
- **React Hook Form + Zod** para formulários e validação
- **Axios** para requisições HTTP
- **Recharts** para gráficos
- **React Leaflet** para mapas
- **Jest + React Testing Library** para testes

### Backend
- **Java 21** com **Spring Boot 3.2**
- **Spring Data JPA** com Hibernate
- **Spring Security** com JWT
- **Flyway** para migrações de banco
- **PostgreSQL** como banco de dados
- **MapStruct** para mapeamento DTO/Entity
- **Lombok** para redução de boilerplate
- **OpenAPI/Swagger** para documentação
- **JUnit 5 + Mockito + Testcontainers** para testes
- **JaCoCo** para cobertura de código

### DevOps
- **Docker** e **Docker Compose**
- **GitHub Actions** para CI/CD
- **Nginx** como reverse proxy
- **PostgreSQL** em container

## 📋 Funcionalidades

### Gestão de Pontos de Coleta
- ✅ CRUD completo de pontos de coleta
- ✅ Geolocalização (latitude/longitude)
- ✅ Controle de capacidade e volume atual
- ✅ Status: Ativo, Inativo, Manutenção, Cheio, Pendente Aprovação
- ✅ Atribuição de coletores
- ✅ Filtros por cidade, status, coletor

### Agendamentos
- ✅ Criação e gestão de agendamentos
- ✅ Status: Pendente, Confirmado, Em Andamento, Concluído, Cancelado, Reagendado
- ✅ Filtros por data, coletor, ponto de coleta
- ✅ Ações: Confirmar, Iniciar, Concluir, Cancelar, Reagendar

### Registros de Coleta
- ✅ Registro de coletas realizadas
- ✅ Volume, tipo de óleo, qualidade, contaminação
- ✅ Verificação por gerentes
- ✅ Fotos e observações
- ✅ GPS da coleta

### Relatórios
- ✅ Volume diário
- ✅ Desempenho por coletor
- ✅ Distribuição por tipo de óleo
- ✅ Exportação CSV

### Usuários e Autenticação
- ✅ JWT Authentication
- ✅ Roles: ADMIN, MANAGER, COLLECTOR, CLIENT
- ✅ Refresh token
- ✅ Alteração de senha
- ✅ Perfil do usuário

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
# Swagger UI: http://localhost:8080/api/swagger-ui.html
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
│   │   └── db/migration/      # Scripts Flyway
│   └── src/test/              # Testes unitários e integração
│
├── frontend/                   # React Application
│   ├── src/
│   │   ├── components/        # Componentes reutilizáveis
│   │   ├── contexts/          # React Contexts (Auth)
│   │   ├── pages/             # Páginas/Rotas
│   │   ├── services/          # API Services (Axios)
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
| `JWT_SECRET` | Chave secreta para JWT | (obrigatório em prod) |
| `MAIL_HOST` | Servidor SMTP | `smtp.gmail.com` |
| `MAIL_PORT` | Porta SMTP | `587` |
| `VITE_API_URL` | URL da API no frontend | `http://localhost:8080/api` |

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

Acesse o Swagger UI em: `http://localhost:8080/api/swagger-ui.html`

### Principais Endpoints

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/auth/login` | Login |
| POST | `/api/auth/register` | Registro |
| GET | `/api/collection-points` | Listar pontos |
| POST | `/api/collection-points` | Criar ponto |
| GET | `/api/schedules` | Listar agendamentos |
| POST | `/api/schedules` | Criar agendamento |
| GET | `/api/records` | Listar registros |
| POST | `/api/records` | Criar registro |
| GET | `/api/reports/*` | Relatórios |

## 🔒 Segurança

- Senhas criptografadas com BCrypt (cost 12)
- JWT com expiração de 24h e refresh token de 7 dias
- CORS configurado
- Validação de entrada em todas as camadas
- Rate limiting (configurável)
- Headers de segurança no Nginx

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