# Pasta: `backend/src/main/java/com/oilcollection/service`

## Visão Geral
Camada de regras de negócio (`@Service`). Orquestra validações, transações, cálculos, notificações, auditoria e interações entre repositórios antes de devolver dados aos controllers.

## Arquivos (12 serviços)
- **`AuthService.java`**: Fluxos de autenticação, registro, renovação de token e gestão de conta/sessão.
- **`UserService.java`**: Regras de cadastro, atualização, consulta e exclusão/gerenciamento de usuários.
- **`CollectionPointService.java`**: Regras e consultas de pontos de coleta.
- **`CollectionScheduleService.java`**: Lógica de agendamentos de coleta, transições de status e validações.
- **`CollectionRecordService.java`**: Registro de coletas, cálculos/volumes e confirmações.
- **`CollectorCoverageService.java`**: Gerenciamento de cobertura de coletores.
- **`ExchangeItemService.java`**: Regras de itens de troca vinculados a agendamentos.
- **`IncidentService.java`**: Gestão de ocorrências operacionais.
- **`NotificationService.java`**: Criação, leitura e contagem de notificações.
- **`ProductService.java`**: Regras de gerenciamento de produtos.
- **`SystemConfigService.java`**: Leitura e atualização de configurações do sistema.
- **`AuditLogService.java`**: Registro de eventos de auditoria.
