# Pasta: `backend/src/main/resources/db/migration`

## Visão Geral
Contém as migrations SQL versionadas (padrão Flyway `V<versao>__<descricao>.sql`) que criam e evoluem o esquema relacional do banco de dados de forma rastreável.

## Arquivos
- **`V1__initial_schema.sql`**: Criação das tabelas iniciais do domínio (usuários, pontos de coleta, agendamentos, registros de coleta e estruturas base).
- **`V2__enhance_schema_for_user_stories.sql`**: Expansão do schema para atender histórias de usuário (ex.: dados de estabelecimento como CNPJ, endereço, cidade, estado, CEP e novos campos/tabelas).
- **`V3__fix_missing_columns.sql`**: Ajustes incrementais em colunas (ex.: `updated_at` em `incidents` e colunas de confirmação/verificação em `collection_records`).
- **`V4__make_cpf_nullable_in_users.sql`**: Altera a coluna `cpf` em `users` para permitir valor nulo (`DROP NOT NULL`).
