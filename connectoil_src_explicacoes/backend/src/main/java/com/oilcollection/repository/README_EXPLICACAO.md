# Pasta: `backend/src/main/java/com/oilcollection/repository`

## Visão Geral
Camada de acesso a dados baseada em **Spring Data JPA** (`JpaRepository<Entidade, UUID>`). Encapsula consultas derivadas, queries JPQL/SQL e operações CRUD sobre o banco de dados.

## Arquivos (11 repositórios)
- **`UserRepository.java`**, **`CollectionPointRepository.java`**, **`CollectionScheduleRepository.java`**, **`CollectionRecordRepository.java`**, **`CollectorCoverageRepository.java`**, **`ExchangeItemRepository.java`**, **`IncidentRepository.java`**, **`NotificationRepository.java`**, **`ProductRepository.java`**, **`SystemConfigRepository.java`**, **`AuditLogRepository.java`**: Cada interface estende `JpaRepository` com chave primária `UUID` e fornece consultas específicas do respectivo agregado/entidade.
