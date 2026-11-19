# Pasta: `backend/src/main/java/com/oilcollection/model`

## Visão Geral
Camada de domínio e persistência JPA (`@Entity`). Define as tabelas, relacionamentos, chaves e `enum`s de estado/tipo utilizados pelo Hibernate/PostgreSQL.

## Arquivos (11 modelos)
- **`User.java`** (implementa `UserDetails`, com `UserRole`): Usuários do sistema, credenciais, perfis e dados cadastrais.
- **`CollectionPoint.java`** (`PointStatus`): Pontos de coleta e seus estados operacionais.
- **`CollectionSchedule.java`** (`ScheduleStatus`, `SaleType`): Agendamentos de coleta, status e modalidade.
- **`CollectionRecord.java`**: Registros concretos de coleta realizada (volumes, confirmações/verificações).
- **`CollectorCoverage.java`**: Mapeamento de cobertura de coletores.
- **`ExchangeItem.java`**: Itens envolvidos em trocas associadas a coletas/agendamentos.
- **`Incident.java`** (`IncidentType`, `Severity`, `Status`): Ocorrências relatadas na operação.
- **`Notification.java`** (`Type`, `DeliveryMethod`): Notificações enviadas/registradas para usuários.
- **`Product.java`**: Produtos cadastrados no sistema.
- **`SystemConfig.java`** (`DataType`): Parâmetros configuráveis do sistema com tipagem.
- **`AuditLog.java`** (`Action`): Trilha de auditoria registrando ações relevantes no sistema.
