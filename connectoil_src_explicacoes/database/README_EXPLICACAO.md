# Pasta: `ConnectOil-main/database` (Inicialização do Banco de Dados PostgreSQL)

## Visão Geral
Contém os scripts SQL de bootstrap executados automaticamente na primeira inicialização do container **PostgreSQL 16** (`postgres:16-alpine`), preparando o ambiente antes que a aplicação Spring Boot assuma o controle da estrutura das tabelas.

## Arquivos (1 arquivo)
- **`init.sql`**: Script de preparação inicial montado em `/docker-entrypoint-initdb.d/init.sql` no `docker-compose.yml` (executado apenas quando o volume `postgres_data` ainda está vazio):
  - **Extensão UUID (`CREATE EXTENSION IF NOT EXISTS "uuid-ossp";`)**: Habilita funções de geração de identificadores únicos universais (`UUID`) no PostgreSQL, padrão utilizado nas chaves primárias das entidades do sistema.
  - **Fuso Horário (`SET TIMEZONE = 'America/Sao_Paulo';`)**: Padroniza o timezone da sessão/banco para o horário de Brasília, garantindo consistência nos registros de agendamentos, coletas e logs de auditoria.
  - **Role/Usuário Dedicado (comentado para produção)**: Contém bloco SQL pronto (comentado) para criação de uma role restrita `oil_collection_app` com privilégios no banco `oil_collection` e schema `public`.
  - **Separação de Responsabilidade com o Flyway**: Não cria tabelas manualmente — conforme documentado no próprio arquivo, toda a criação e evolução do esquema relacional (`users`, `collection_points`, `collection_schedules`, `collection_records`, etc.) é delegada às migrations versionadas do **Flyway** (`backend/src/main/resources/db/migration/V1..V4`) quando o backend sobe após o healthcheck `pg_isready` confirmar que o PostgreSQL está saudável.
