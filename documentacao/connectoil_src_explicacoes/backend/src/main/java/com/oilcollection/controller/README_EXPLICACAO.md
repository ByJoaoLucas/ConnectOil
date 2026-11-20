# Pasta: `backend/src/main/java/com/oilcollection/controller`

## Visão Geral
Camada de exposição HTTP/REST (`@RestController`). Recebe requisições, extrai parâmetros/DTOs, delega as regras de negócio para os `Service`s correspondentes e retorna respostas HTTP padronizadas.

## Arquivos (11 controllers)
- **`AuthController.java`** (`/api/auth`): Endpoints de autenticação (`POST /login`, `POST /register`, `POST /refresh` e fluxos correlatos de sessão/conta).
- **`CollectionPointController.java`** (`/api/collection-points`): Gerenciamento de pontos de coleta de óleo (listagem, busca por ID, criação, atualização, status/ativação).
- **`CollectionRecordController.java`** (`/api/records`): Registro e acompanhamento das coletas efetivamente realizadas, volumes e confirmações.
- **`CollectionScheduleController.java`** (`/api/schedules`): Agendamento de coletas, janelas/status de agendamento e vinculação com pontos/coletores.
- **`CollectorCoverageController.java`** (`/api/collector-coverage`): Áreas ou regiões de cobertura atendidas por coletores.
- **`ExchangeItemController.java`** (`/api/exchange-items`): Gerenciamento de itens de troca vinculados a agendamentos (`schedule/{scheduleId}`).
- **`IncidentController.java`** (`/api/incidents`): Registro e consulta de ocorrências/problemas operacionais (inclusive por ponto de coleta `/point/{pointId}`).
- **`NotificationController.java`** (`/api/notifications`): Consulta de notificações do usuário, não lidas (`/unread`) e contagem (`/count/unread`).
- **`ProductController.java`** (`/api/products`): Catálogo/gestão de produtos associados ao sistema.
- **`SystemConfigController.java`** (`/api/system-config`): Consulta e configuração de parâmetros globais do sistema (por chave `/{key}` ou listagem).
- **`UserController.java`** (`/api/users`): Administração e consulta de usuários e perfis.
